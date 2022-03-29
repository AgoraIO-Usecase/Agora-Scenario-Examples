package io.agora.sample.club;


import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.MIRROR_MODE_TYPE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;


public class RtcManager {
    private static final String TAG = "RtcManager";
    private static final String SAMPLE_MOVIE_URL = "https://webdemo.agora.io/agora-web-showcase/examples/Agora-Custom-VideoSource-Web/assets/sample.mp4";
    private static int LOCAL_RTC_UID = 0;

    private static CameraCapturerConfiguration.CAMERA_DIRECTION cameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;
    public static final VideoEncoderConfiguration encoderConfiguration = new VideoEncoderConfiguration(
            VD_640x360,
            FRAME_RATE_FPS_15,
            700,
            ORIENTATION_MODE_FIXED_PORTRAIT
    );

    private volatile boolean isInitialized = false;
    private RtcEngine engine;
    private final Map<Integer, Runnable> firstVideoFramePendingRuns = new HashMap<>();
    private Context mContext;
    private OnChannelListener publishChannelListener;
    private String publishChannelId;

    private volatile static RtcManager INSTANCE = null;
    private IMediaPlayer mMediaPlayer;

    private RtcManager() {
    }

    public static final RtcManager getInstance() {
        if (INSTANCE == null) {
            synchronized (RtcManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RtcManager();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context, String appId, OnInitializeListener listener) {
        if (isInitialized) {
            return;
        }
        mContext = context;
        try {
            // 0. create engine
            engine = RtcEngine.create(mContext.getApplicationContext(), appId, new IRtcEngineEventHandler() {
                @Override
                public void onWarning(int warn) {
                    super.onWarning(warn);
                    Log.w(TAG, String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
                }

                @Override
                public void onError(int err) {
                    super.onError(err);
                    Log.e(TAG, String.format("onError code %d", err));
                    if (err == ErrorCode.ERR_OK) {
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    } else {
                        if (listener != null) {
                            listener.onError(err, err == ErrorCode.ERR_INVALID_TOKEN ? "invalid token" : "");
                        }
                        if (publishChannelListener != null) {
                            publishChannelListener.onError(err, "");
                        }
                    }
                }

                @Override
                public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
                    super.onFirstLocalVideoFrame(width, height, elapsed);

                    Log.d(TAG, "onFirstLocalVideoFrame");
                    Runnable runnable = firstVideoFramePendingRuns.get(LOCAL_RTC_UID);
                    if (runnable != null) {
                        runnable.run();
                        firstVideoFramePendingRuns.remove(LOCAL_RTC_UID);
                    }
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    if (publishChannelId.equals(channel)) {
                        if (publishChannelListener != null) {
                            publishChannelListener.onJoinSuccess(uid);
                        }
                    }
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    if (publishChannelListener != null) {
                        publishChannelListener.onUserJoined(publishChannelId, uid);
                    }
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    if (publishChannelListener != null) {
                        publishChannelListener.onUserOffline(publishChannelId, uid);
                    }
                }

                @Override
                public void onRtcStats(RtcStats stats) {
                    super.onRtcStats(stats);
                }

                @Override
                public void onLastmileProbeResult(LastmileProbeResult result) {
                    super.onLastmileProbeResult(result);

                }

                @Override
                public void onRemoteVideoStats(RemoteVideoStats stats) {
                    super.onRemoteVideoStats(stats);
                }

                @Override
                public void onStreamMessage(int uid, int streamId, byte[] data) {
                    super.onStreamMessage(uid, streamId, data);
                    Log.d(TAG, "onStreamMessage uid=" + uid + ",streamId=" + streamId + ",data=" + new String(data));
                }

                @Override
                public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
                    super.onStreamMessageError(uid, streamId, error, missed, cached);
                    Log.d(TAG, "onStreamMessageError uid=" + uid + ",streamId=" + streamId + ",error=" + error + ",missed=" + missed + ",cached=" + cached);
                }
            });
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

            engine.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD, Constants.AUDIO_SCENARIO_GAME_STREAMING);
            engine.setDefaultAudioRoutetoSpeakerphone(true);
            engine.enableDualStreamMode(false);

            engine.enableVideo();
            engine.enableAudio();

            engine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(cameraDirection));
            if (cameraDirection == CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT) {
                encoderConfiguration.mirrorMode = MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED;
                engine.setVideoEncoderConfiguration(encoderConfiguration);
            } else {
                encoderConfiguration.mirrorMode = MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED;
                engine.setVideoEncoderConfiguration(encoderConfiguration);
            }

            isInitialized = true;
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(-1, "RtcEngine create exception : " + e.toString());
            }
        }
    }

    public void renderLocalVideo(FrameLayout container, Runnable firstFrame) {
        if (engine == null) {
            return;
        }
        container.removeAllViews();
        TextureView videoView = new TextureView(container.getContext());
        videoView.setOpaque(true);
        container.addView(videoView);
        firstVideoFramePendingRuns.put(LOCAL_RTC_UID, firstFrame);
        engine.setupLocalVideo(new VideoCanvas(videoView, RENDER_MODE_HIDDEN, LOCAL_RTC_UID));
        engine.startPreview();
    }

    public void joinChannel(String channelId, String uid, String token, boolean publish, OnChannelListener listener) {
        if (engine == null) {
            return;
        }
        int _uid = LOCAL_RTC_UID;
        if (!TextUtils.isEmpty(uid)) {
            try {
                _uid = Integer.parseInt(uid);
                LOCAL_RTC_UID = _uid;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCameraTrack = publish;
        options.publishAudioTrack = publish;
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;
        options.clientRoleType = publish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;

        publishChannelId = channelId;
        publishChannelListener = new OnChannelListener() {
            @Override
            public void onError(int code, String message) {
                if (listener != null) {
                    listener.onError(code, message);
                }
            }

            @Override
            public void onJoinSuccess(int uid) {
                if (listener != null) {
                    listener.onJoinSuccess(uid);
                }
            }

            @Override
            public void onUserJoined(String channelId, int uid) {
                if (listener != null) {
                    listener.onUserJoined(channelId, uid);
                }
            }

            @Override
            public void onUserOffline(String channelId, int uid) {
                if (listener != null) {
                    listener.onUserOffline(channelId, uid);
                }
            }

        };

        int ret = engine.joinChannel(token, channelId, _uid, options);
        Log.i(TAG, String.format("joinChannel channel %s ret %d", channelId, ret));
    }

    public void setLocalPublish(boolean publish) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCameraTrack = true;
        options.publishAudioTrack = true;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        options.clientRoleType = publish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        engine.updateChannelMediaOptions(options);
    }

    public void enableLocalVideo(boolean enable){
        engine.enableLocalVideo(enable);
    }

    public void enableLocalAudio(boolean enable){
        engine.enableLocalAudio(enable);
    }

    public void renderRemoteVideo(FrameLayout container, int uid) {
        if (engine == null) {
            return;
        }
        container.removeAllViews();
        TextureView view = new TextureView(container.getContext());
        view.setOpaque(true);
        container.addView(view);
        engine.setupRemoteVideo(new VideoCanvas(view, RENDER_MODE_HIDDEN, uid));
    }

    public void renderPlayerVideo(FrameLayout container){
        container.removeAllViews();
        TextureView view = new TextureView(container.getContext());
        container.addView(view);
        getOrCreateMediaPlayer().setView(view);
    }

    public void openPlayerVideo(String url){
        if(TextUtils.isEmpty(url)){
            url = SAMPLE_MOVIE_URL;
        }
        IMediaPlayer mediaPlayer = getOrCreateMediaPlayer();
        mediaPlayer.setPlayerOption("fps_probe_size", 0);
        mediaPlayer.open(url, 0);
    }

    public void closePlayerVideo(){
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.destroy();
            mMediaPlayer = null;
        }
    }

    public IMediaPlayer getOrCreateMediaPlayer(){
        if(mMediaPlayer != null){
            return mMediaPlayer;
        }
        mMediaPlayer = engine.createMediaPlayer();
        mMediaPlayer.registerPlayerObserver(new IMediaPlayerObserver() {
            @Override
            public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState, io.agora.mediaplayer.Constants.MediaPlayerError mediaPlayerError) {
                Log.d(TAG, "MediaPlayer onPlayerStateChanged -- url=" + mMediaPlayer.getPlaySrc() + "state=" + mediaPlayerState + ", error=" + mediaPlayerError);
                if (mediaPlayerState == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.play();
                    }
                }
            }

            @Override
            public void onPositionChanged(long l) {

            }

            @Override
            public void onPlayerEvent(io.agora.mediaplayer.Constants.MediaPlayerEvent mediaPlayerEvent) {

            }

            @Override
            public void onMetaData(io.agora.mediaplayer.Constants.MediaPlayerMetadataType mediaPlayerMetadataType, byte[] bytes) {

            }

            @Override
            public void onPlayBufferUpdated(long l) {

            }

            @Override
            public void onCompleted() {

            }
        });

        return mMediaPlayer;
    }

    public void release() {
        publishChannelListener = null;
        if (engine != null) {
            engine.stopPreview();
            closePlayerVideo();
            engine = null;
            RtcEngine.destroy();
        }
        isInitialized = false;
    }

    public void switchCamera() {
        if (engine == null) {
            return;
        }
        engine.switchCamera();
        if (cameraDirection == CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT) {
            cameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR;
            encoderConfiguration.mirrorMode = MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED;
        } else {
            cameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;
            encoderConfiguration.mirrorMode = MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED;
        }
        engine.setVideoEncoderConfiguration(encoderConfiguration);
    }


    public interface OnInitializeListener {
        void onError(int code, String message);

        void onSuccess();
    }

    public interface OnChannelListener {
        void onError(int code, String message);

        void onJoinSuccess(int uid);

        void onUserJoined(String channelId, int uid);

        void onUserOffline(String channelId, int uid);
    }
}
