package io.agora.livepk.manager;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
import io.agora.rtc2.IAgoraEventHandler;
import io.agora.rtc2.IDirectCdnStreamingEventHandler;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.UserInfo;
import io.agora.rtc2.live.LiveTranscoding;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

import static io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360;

public class RtcManager {
    private static final String TAG = "RtcManager";
    private static final int LOCAL_RTC_UID = 0;
    private static final String AGORA_CDN_CHANNEL_PUSH_PREFIX = "rtmp://mdetest.push.agoramde.agoraio.cn/live/%s";
    private static final String AGORA_CDN_CHANNEL_PULL_PREFIX = "rtmp://mdetest2.pull.agoramde.agoraio.cn/live/%s";

    private volatile boolean isInitialized = false;
    private final Map<Integer, Runnable> firstVideoFramePendingRuns = new HashMap<>();
    private Context mContext;

    private RtcEngine engine;
    private static CameraCapturerConfiguration.CAMERA_DIRECTION currCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;

    private final VideoEncoderConfiguration encoderConfiguration = new VideoEncoderConfiguration(
            VD_640x360,
            FRAME_RATE_FPS_15,
            700,
            ORIENTATION_MODE_FIXED_PORTRAIT
    );

    private LiveTranscoding liveTranscoding = new LiveTranscoding();
    private int canvas_width = 480;
    private int canvas_height = 640;

    private int localUid = 0;
    private Runnable pendingDirectCDNStoppedRun = null;

    public void init(Context context, String appId, OnInitializeListener listener) {
        if (isInitialized) {
            return;
        }
        mContext = context;
        try {
            // 0. create engine
            RtcEngineConfig config = new RtcEngineConfig();
            /**
             * The context of Android Activity
             */
            config.mContext = context.getApplicationContext();
            /**
             * The App ID issued to you by Agora. See <a href="https://docs.agora.io/en/Agora%20Platform/token#get-an-app-id"> How to get the App ID</a>
             */
            config.mAppId = appId;
            /** Sets the channel profile of the Agora RtcEngine.
             CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile.
             Use this profile in one-on-one calls or group calls, where all users can talk freely.
             CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast
             channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams;
             an audience can only receive streams.*/
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            /**
             * IRtcEngineEventHandler is an abstract class providing default implementation.
             * The SDK uses this class to report to the app on SDK runtime events.
             */
            config.mEventHandler = new IRtcEngineEventHandler() {
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
                    }
                }

                @Override
                public void onLeaveChannel(RtcStats stats) {
                    super.onLeaveChannel(stats);
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    localUid = uid;
                    LiveTranscoding.TranscodingUser user = new LiveTranscoding.TranscodingUser();
                    user.x = 0;
                    user.y = 0;
                    user.width = canvas_width;
                    user.height = canvas_height;
                    user.uid = localUid;
                    liveTranscoding.addUser(user);
                    engine.setLiveTranscoding(liveTranscoding);
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    if (listener != null) {
                        listener.onUserJoined(uid);
                    }
                    LiveTranscoding.TranscodingUser user1 = new LiveTranscoding.TranscodingUser();
                    user1.x = canvas_width / 2;
                    user1.y = 0;
                    user1.width = canvas_width / 2;
                    user1.height = canvas_height / 2;
                    user1.uid = uid;
                    liveTranscoding.addUser(user1);
                    engine.setLiveTranscoding(liveTranscoding);
                }
            };
            engine = RtcEngine.create(config);
            engine.setDefaultAudioRoutetoSpeakerphone(true);
            engine.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
            engine.enableVideo();
            canvas_height = Math.max(encoderConfiguration.dimensions.height, encoderConfiguration.dimensions.width);
            canvas_width = Math.min(encoderConfiguration.dimensions.height, encoderConfiguration.dimensions.width);
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

    public void renderLocalVideo(FrameLayout container, Runnable firstFrame) {
        if (engine == null) {
            return;
        }
        engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        engine.enableLocalVideo(true);
        engine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(currCameraDirection));
        View videoView = RtcEngine.CreateRendererView(mContext);
        container.addView(videoView);
        firstVideoFramePendingRuns.put(LOCAL_RTC_UID, firstFrame);
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
        } else {
            currCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;
        }
    }

    public void startDirectCDNStreaming(String channelId) {
        if (engine == null) {
            return;
        }
        engine.setDirectCdnStreamingVideoConfiguration(encoderConfiguration);
        DirectCdnStreamingMediaOptions directCdnStreamingMediaOptions = new DirectCdnStreamingMediaOptions();
        directCdnStreamingMediaOptions.publishCameraTrack = true;
        directCdnStreamingMediaOptions.publishMicrophoneTrack = true;
        engine.startDirectCdnStreaming(new IDirectCdnStreamingEventHandler() {
            @Override
            public void onDirectCdnStreamingStateChanged(DirectCdnStreamingState directCdnStreamingState, DirectCdnStreamingError directCdnStreamingError, String s) {
                if(directCdnStreamingState == DirectCdnStreamingState.STOPPED){
                    if(pendingDirectCDNStoppedRun != null){
                        pendingDirectCDNStoppedRun.run();
                        pendingDirectCDNStoppedRun = null;
                    }
                }
            }

            @Override
            public void onDirectCdnStreamingStats(DirectCdnStreamingStats directCdnStreamingStats) {

            }
        }, String.format(Locale.US, AGORA_CDN_CHANNEL_PUSH_PREFIX, channelId), directCdnStreamingMediaOptions);
    }

    public void stopDirectCDNStreaming(Runnable onDirectCDNStopped) {
        if (engine == null) {
            return;
        }
        pendingDirectCDNStoppedRun = onDirectCDNStopped;
        engine.stopDirectCdnStreaming();
    }

    public void startRtcStreaming(String channelId, boolean publish) {
        if (engine == null) {
            return;
        }

        ChannelMediaOptions channelMediaOptions = new ChannelMediaOptions();
        channelMediaOptions.publishAudioTrack = true;
        channelMediaOptions.publishCameraTrack = true;
        channelMediaOptions.clientRoleType = CLIENT_ROLE_BROADCASTER;
        engine.joinChannel(null, channelId, 0, channelMediaOptions);
        if(publish){
            engine.setLiveTranscoding(liveTranscoding);
            engine.addPublishStreamUrl(String.format(Locale.US, AGORA_CDN_CHANNEL_PUSH_PREFIX, channelId), true);
        }
    }

    public void stopRtcStreaming() {
        if (engine == null) {
            return;
        }
        engine.leaveChannel();
    }

    public void renderRemoteVideo(FrameLayout container, int uid, Runnable firstFrame) {
        if (engine == null) {
            return;
        }
        // 4. render video
        View videoView = RtcEngine.CreateRendererView(mContext);
        container.addView(videoView);
        firstVideoFramePendingRuns.put(uid, firstFrame);
        engine.setupRemoteVideo(new VideoCanvas(videoView, RENDER_MODE_HIDDEN, uid));
    }

    public void release() {
        //Set<String> keys = mRtcChannels.keySet();
        //for (String key : keys) {
        //    mRtcChannels.get(key).leaveChannel();
        //}
        //mRtcChannels.clear();
        engine.stopPreview();
        if (engine != null) {
            RtcEngine.destroy();
        }
    }

    public void startDirectCDNPlayer(FrameLayout container, String channelId, Runnable completedRun) {
        if (engine == null) {
            return;
        }

        IMediaPlayer mediaPlayer = engine.createMediaPlayer();
        mediaPlayer.registerPlayerObserver(new IMediaPlayerObserver() {
            @Override
            public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState mediaPlayerState, io.agora.mediaplayer.Constants.MediaPlayerError mediaPlayerError) {
                if (mediaPlayerState == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
                    mediaPlayer.play();
                    if (completedRun != null) {
                        completedRun.run();
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

        });
        SurfaceView surfaceView = new SurfaceView(container.getContext());
        surfaceView.setZOrderMediaOverlay(false);
        container.addView(surfaceView);

        engine.setupLocalVideo(new VideoCanvas(surfaceView,
                Constants.RENDER_MODE_HIDDEN,
                Constants.VIDEO_MIRROR_MODE_AUTO,
                Constants.VIDEO_SOURCE_MEDIA_PLAYER,
                mediaPlayer.getMediaPlayerId(),
                LOCAL_RTC_UID
        ));
        engine.startPreview();
        engine.setDefaultAudioRoutetoSpeakerphone(true);
        mediaPlayer.openWithAgoraCDNSrc(String.format(Locale.US, AGORA_CDN_CHANNEL_PULL_PREFIX, channelId), LOCAL_RTC_UID);
    }


    public interface OnInitializeListener {
        void onError(int code, String message);

        void onSuccess();

        void onUserJoined(int uid);
    }

}
