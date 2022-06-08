package io.agora.scene.pklivebycdn;

import static io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_1;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_10;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_7;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_120x120;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_1280x720;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_160x120;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_180x180;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_240x180;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_240x240;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_320x180;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_320x240;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_360x360;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_424x240;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_480x360;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_480x480;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x480;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_840x480;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_960x720;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.PlayerUpdatedInfo;
import io.agora.mediaplayer.data.SrcInfo;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DirectCdnStreamingError;
import io.agora.rtc2.DirectCdnStreamingMediaOptions;
import io.agora.rtc2.DirectCdnStreamingState;
import io.agora.rtc2.DirectCdnStreamingStats;
import io.agora.rtc2.IDirectCdnStreamingEventHandler;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.LeaveChannelOptions;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.live.LiveTranscoding;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class RtcManager {
    private static final String TAG = "RtcManager";
    private static final int LOCAL_RTC_UID = 0;
    private static final String AGORA_CDN_CHANNEL_PUSH_PREFIX = "rtmp://examplepush.agoramde.agoraio.cn/live/%s";
    private static final String AGORA_CDN_CHANNEL_PULL_PREFIX = "http://examplepull.agoramde.agoraio.cn/live/%s.flv";
    public static final List<VideoEncoderConfiguration.VideoDimensions> sVideoDimensions = Arrays.asList(
            VD_120x120,
            VD_160x120,
            VD_180x180,
            VD_240x180,
            VD_320x180,
            VD_240x240,
            VD_320x240,
            VD_424x240,
            VD_360x360,
            VD_480x360,
            VD_640x360,
            VD_480x480,
            VD_640x480,
            VD_840x480,
            VD_960x720,
            VD_1280x720
    );
    public static final List<VideoEncoderConfiguration.FRAME_RATE> sFrameRates = Arrays.asList(
            FRAME_RATE_FPS_1,
            FRAME_RATE_FPS_7,
            FRAME_RATE_FPS_10,
            FRAME_RATE_FPS_15,
            FRAME_RATE_FPS_24,
            FRAME_RATE_FPS_30
    );

    private static CameraCapturerConfiguration.CAMERA_DIRECTION currCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;
    public static boolean isMuteLocalAudio = false;
    public static final VideoEncoderConfiguration encoderConfiguration = new VideoEncoderConfiguration(
            VD_640x360,
            FRAME_RATE_FPS_15,
            700,
            ORIENTATION_MODE_FIXED_PORTRAIT
    );

    private volatile boolean isInitialized = false;
    private RtcEngine engine;

    private LiveTranscoding liveTranscoding;
    private int canvas_width = 480;
    private int canvas_height = 640;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile int localUid = 0;
    private Runnable pendingDirectCDNStoppedRun = null;
    private final IDirectCdnStreamingEventHandler iDirectCdnStreamingEventHandler = new IDirectCdnStreamingEventHandler() {
        @Override
        public void onDirectCdnStreamingStateChanged(DirectCdnStreamingState directCdnStreamingState, DirectCdnStreamingError directCdnStreamingError, String s) {
            Log.d(TAG, String.format("Stream Publish(DirectCdnStreaming): onDirectCdnStreamingStateChanged directCdnStreamingState=%s directCdnStreamingError=%s", directCdnStreamingState.toString(), directCdnStreamingError.toString()));
            switch (directCdnStreamingState){
                case STOPPED:
                    if(pendingDirectCDNStoppedRun != null){
                        mainHandler.post(pendingDirectCDNStoppedRun);
                        pendingDirectCDNStoppedRun = null;
                    }
                    break;
            }
        }

        @Override
        public void onDirectCdnStreamingStats(DirectCdnStreamingStats directCdnStreamingStats) {

        }
    };

    private Runnable pendingLeaveChannelRun = null;
    private Runnable pendingJoinChannelRun = null;

    private IMediaPlayer mMediaPlayer = null;

    public void init(Context context, String appId, OnInitializeListener listener) {
        if (isInitialized) {
            return;
        }
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = context.getApplicationContext();
            config.mAppId = appId;
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onError(int err) {
                    super.onError(err);
                    Log.e(TAG, String.format("onError code %d", err));
                    if (err == ErrorCode.ERR_OK) {
                        if (listener != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onSuccess();
                                }
                            });
                        }
                    } else {
                        if (listener != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError(err, err == ErrorCode.ERR_INVALID_TOKEN ? "invalid token" : "");
                                }
                            });
                        }
                    }
                }

                @Override
                public void onStreamUnpublished(String url) {
                    super.onStreamUnpublished(url);
                    Log.d(TAG, String.format("Stream Publish: onStreamUnpublished url=%s", url));
                    LeaveChannelOptions leaveChannelOptions = new LeaveChannelOptions();
                    leaveChannelOptions.stopMicrophoneRecording = false;
                    engine.leaveChannel(leaveChannelOptions);
                    engine.startPreview();

                }

                @Override
                public void onStreamPublished(String url, int error) {
                    super.onStreamPublished(url, error);
                    Log.d(TAG, String.format("Stream Publish: onStreamPublished url=%s error=%d", url, error));
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    localUid = uid;
                    if (pendingJoinChannelRun != null) {
                        mainHandler.post(pendingJoinChannelRun);
                        pendingJoinChannelRun = null;
                    }
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    if (listener != null) {
                        mainHandler.post(() -> listener.onUserJoined(uid));
                    }
                    if(liveTranscoding != null){
                        LiveTranscoding.TranscodingUser user1 = new LiveTranscoding.TranscodingUser();
                        user1.x = canvas_width / 2;
                        user1.y = 0;
                        user1.width = canvas_width / 2;
                        user1.height = canvas_height / 2;
                        user1.uid = uid;
                        user1.zOrder = 1;
                        liveTranscoding.addUser(user1);
                        engine.setLiveTranscoding(liveTranscoding);
                    }
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    if (listener != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onUserLeaved(uid, reason);
                            }
                        });

                    }
                }

                @Override
                public void onLeaveChannel(RtcStats stats) {
                    super.onLeaveChannel(stats);
                    if (pendingLeaveChannelRun != null) {
                        mainHandler.post(pendingLeaveChannelRun);
                        pendingLeaveChannelRun = null;
                    }
                }
            };
            engine = RtcEngine.create(config);
            engine.setDefaultAudioRoutetoSpeakerphone(true);
            engine.enableDualStreamMode(false);
            engine.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
            engine.enableVideo();
            engine.enableAudio();
            engine.enableLocalAudio(!isMuteLocalAudio);

            canvas_height = Math.max(encoderConfiguration.dimensions.height, encoderConfiguration.dimensions.width);
            canvas_width = Math.min(encoderConfiguration.dimensions.height, encoderConfiguration.dimensions.width);

            if(currCameraDirection == CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT){
                encoderConfiguration.mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED;
            }else{
                encoderConfiguration.mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED;
            }
            engine.setVideoEncoderConfiguration(encoderConfiguration);
            engine.setDirectCdnStreamingVideoConfiguration(encoderConfiguration);

            isInitialized = true;
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(-1, "RtcEngine create exception : " + e.toString());
            }
        }
    }

    public void setCurrCameraDirection(CameraCapturerConfiguration.CAMERA_DIRECTION direction) {
        if (currCameraDirection != direction) {
            currCameraDirection = direction;
            if (engine != null) {
                engine.switchCamera();
            }
        }
    }

    public void renderLocalVideo(FrameLayout container) {
        if (engine == null) {
            return;
        }
        engine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(currCameraDirection));
        View videoView = new TextureView(container.getContext());
        container.removeAllViews();
        container.addView(videoView);
        int ret = engine.setupLocalVideo(new VideoCanvas(videoView, RENDER_MODE_HIDDEN, LOCAL_RTC_UID));
        Log.d(TAG, "setupLocalVideo ret=" + ret);
        ret = engine.startPreview();
        Log.d(TAG, "startPreview ret=" + ret);
    }

    public void switchCamera() {
        if (engine == null) {
            return;
        }
        engine.switchCamera();
        if (currCameraDirection == CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT) {
            currCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR;
            encoderConfiguration.mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED;
        } else {
            currCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;
            encoderConfiguration.mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED;
        }
        engine.setVideoEncoderConfiguration(encoderConfiguration);
        engine.setDirectCdnStreamingVideoConfiguration(encoderConfiguration);
    }

    public void startDirectCDNStreaming(String channelId) {
        if (engine == null) {
            return;
        }
        DirectCdnStreamingMediaOptions directCdnStreamingMediaOptions = new DirectCdnStreamingMediaOptions();
        directCdnStreamingMediaOptions.publishCameraTrack = true;
        directCdnStreamingMediaOptions.publishMicrophoneTrack = true;

        String url = String.format(Locale.US, AGORA_CDN_CHANNEL_PUSH_PREFIX, channelId);
        Log.d(TAG, "Stream Publish(DirectCdnStreaming): startDirectCDNStreaming url=" + url);
        engine.setDirectCdnStreamingVideoConfiguration(encoderConfiguration);
        engine.startDirectCdnStreaming(iDirectCdnStreamingEventHandler, url, directCdnStreamingMediaOptions);
    }

    public void stopDirectCDNStreaming(Runnable onDirectCDNStopped) {
        if (engine == null) {
            return;
        }

        pendingDirectCDNStoppedRun = onDirectCDNStopped;
        engine.stopDirectCdnStreaming();
    }

    public void startRtcStreaming(String channelId, String token, String userId, boolean transcoding) {
        if (engine == null) {
            return;
        }
        ChannelMediaOptions channelMediaOptions = new ChannelMediaOptions();
        channelMediaOptions.publishAudioTrack = true;
        channelMediaOptions.publishCameraTrack = true;
        channelMediaOptions.autoSubscribeVideo = true;
        channelMediaOptions.autoSubscribeAudio = true;
        channelMediaOptions.clientRoleType = CLIENT_ROLE_BROADCASTER;
        engine.joinChannel(token, channelId, Integer.parseInt(userId), channelMediaOptions);
        if(transcoding){
            pendingJoinChannelRun = () -> {
                liveTranscoding = new LiveTranscoding();
                liveTranscoding.width = canvas_width;
                liveTranscoding.height = canvas_height;
                liveTranscoding.videoBitrate = encoderConfiguration.bitrate;
                liveTranscoding.videoFramerate = encoderConfiguration.frameRate;

                LiveTranscoding.TranscodingUser user = new LiveTranscoding.TranscodingUser();
                user.uid = localUid;
                user.x = user.y = 0;
                user.width = canvas_width;
                user.height = canvas_height;
                liveTranscoding.addUser(user);
                engine.setLiveTranscoding(liveTranscoding);
                engine.addPublishStreamUrl(String.format(Locale.US, AGORA_CDN_CHANNEL_PUSH_PREFIX, channelId), true);
            };
        }
    }

    public void stopRtcStreaming(String channelId, Runnable leaveChannel) {
        if (engine == null) {
            return;
        }
        pendingJoinChannelRun = null;
        localUid = 0;
        pendingLeaveChannelRun = leaveChannel;
        if(liveTranscoding != null){
            int ret = engine.removePublishStreamUrl(String.format(Locale.US, AGORA_CDN_CHANNEL_PUSH_PREFIX, channelId));
            Log.d(TAG, "Stream Publish: stopRtcStreaming removePublishStreamUrl ret=" + ret );
        }else{
            engine.leaveChannel();
        }
    }

    public void renderRemoteVideo(FrameLayout container, int uid) {
        if (engine == null) {
            return;
        }
        // 4. render video
        container.removeAllViews();
        TextureView videoView = new TextureView(container.getContext());
        container.addView(videoView);
        engine.setupRemoteVideo(new VideoCanvas(videoView, RENDER_MODE_HIDDEN, uid));
    }

    public void release() {
        stopPlayer();
        mainHandler.removeCallbacksAndMessages(null);
        pendingDirectCDNStoppedRun = null;
        pendingLeaveChannelRun = null;
        if (engine != null) {
            engine.leaveChannel();
            engine.stopDirectCdnStreaming();
            engine.stopPreview();
            RtcEngine.destroy();
            engine = null;
        }
        isInitialized = false;
    }

    public void renderPlayerView(FrameLayout container, Runnable completedRun) {
        if (engine == null) {
            return;
        }
        if(mMediaPlayer == null){
            mMediaPlayer = engine.createMediaPlayer();
            mMediaPlayer.registerPlayerObserver(new IMediaPlayerObserver() {
                @Override
                public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState, io.agora.mediaplayer.Constants.MediaPlayerError mediaPlayerError) {
                    Log.d(TAG, "MediaPlayer onPlayerStateChanged -- url=" + mMediaPlayer.getPlaySrc() + "state=" + mediaPlayerState + ", error=" + mediaPlayerError);
                    if (mediaPlayerState == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.play();
                        }
                        if (completedRun != null) {
                            mainHandler.post(completedRun);
                        }
                    }
                }

                @Override
                public void onPositionChanged(long l) {

                }

                @Override
                public void onPlayerEvent(io.agora.mediaplayer.Constants.MediaPlayerEvent mediaPlayerEvent, long l, String s) {

                }

                @Override
                public void onMetaData(io.agora.mediaplayer.Constants.MediaPlayerMetadataType mediaPlayerMetadataType, byte[] bytes) {

                }

                @Override
                public void onPlayBufferUpdated(long l) {

                }

                @Override
                public void onPreloadEvent(String s, io.agora.mediaplayer.Constants.MediaPlayerPreloadEvent mediaPlayerPreloadEvent) {

                }

                @Override
                public void onCompleted() {

                }

                @Override
                public void onAgoraCDNTokenWillExpire() {

                }

                @Override
                public void onPlayerSrcInfoChanged(SrcInfo srcInfo, SrcInfo srcInfo1) {

                }

                @Override
                public void onPlayerInfoUpdated(PlayerUpdatedInfo playerUpdatedInfo) {

                }

                @Override
                public void onAudioVolumeIndication(int volume) {

                }

            });
            engine.setDefaultAudioRoutetoSpeakerphone(true);
        }

        container.removeAllViews();
        SurfaceView surfaceView = new SurfaceView(container.getContext());
        surfaceView.setZOrderMediaOverlay(false);
        container.addView(surfaceView);

        engine.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN,
                Constants.VIDEO_MIRROR_MODE_AUTO,
                Constants.VIDEO_SOURCE_MEDIA_PLAYER,
                mMediaPlayer.getMediaPlayerId(),
                LOCAL_RTC_UID
        ));
        engine.startPreview(Constants.VideoSourceType.VIDEO_SOURCE_MEDIA_PLAYER);
    }

    public void openPlayerSrc(String channelId, boolean isCdn){
        if(mMediaPlayer == null){
            return;
        }
        String url = String.format(Locale.US, AGORA_CDN_CHANNEL_PULL_PREFIX, channelId);
        if (mMediaPlayer.getState() == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.setPlayerOption("fps_probe_size", 0);
        if(isCdn){
            mMediaPlayer.openWithAgoraCDNSrc(url, LOCAL_RTC_UID);
        }else{
            mMediaPlayer.open(url, LOCAL_RTC_UID);
        }
    }

    public void stopPlayer(){
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            engine.stopPreview();
        }
    }

    public void muteLocalAudio(boolean mute){
        if(engine == null){
            return;
        }
        isMuteLocalAudio = mute;
        engine.enableLocalAudio(!mute);
    }

    public interface OnInitializeListener {
        void onError(int code, String message);

        void onSuccess();

        void onUserJoined(int uid);

        void onUserLeaved(int uid, int reason);
    }

}
