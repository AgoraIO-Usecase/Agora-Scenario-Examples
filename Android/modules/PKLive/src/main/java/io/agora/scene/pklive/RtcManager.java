package io.agora.scene.pklive;


import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_1;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_10;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_7;
import static io.agora.rtc2.video.VideoEncoderConfiguration.MIRROR_MODE_TYPE;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;


public class RtcManager {
    private static final String TAG = "RtcManager";
    private static final int LOCAL_RTC_UID = 0;
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
    public static final List<FRAME_RATE> sFrameRates = Arrays.asList(
            FRAME_RATE_FPS_1,
            FRAME_RATE_FPS_7,
            FRAME_RATE_FPS_10,
            FRAME_RATE_FPS_15,
            FRAME_RATE_FPS_24,
            FRAME_RATE_FPS_30
    );


    private static CameraCapturerConfiguration.CAMERA_DIRECTION cameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;
    public static final VideoEncoderConfiguration encoderConfiguration = new VideoEncoderConfiguration(
            VD_640x360,
            FRAME_RATE_FPS_15,
            700,
            ORIENTATION_MODE_FIXED_PORTRAIT
    );

    private volatile boolean isInitialized = false;
    private RtcEngineEx engine;
    private final Map<Integer, Runnable> firstVideoFramePendingRuns = new HashMap<>();
    private Context mContext;

    private final Map<String, RtcConnection> connectionMap = new HashMap<>();

    private OnChannelListener publishChannelListener;
    private String publishChannelId;

    public void init(Context context, String appId, OnInitializeListener listener) {
        if (isInitialized) {
            return;
        }
        mContext = context;
        try {
            // 0. create engine
            engine = (RtcEngineEx) RtcEngineEx.create(mContext.getApplicationContext(), appId, new IRtcEngineEventHandler() {
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
            if(cameraDirection == CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT){
                encoderConfiguration.mirrorMode = MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED;
                engine.setVideoEncoderConfiguration(encoderConfiguration);
            }else{
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
        TextureView videoView = new TextureView(container.getContext());
        container.addView(videoView);
        firstVideoFramePendingRuns.put(LOCAL_RTC_UID, firstFrame);
        engine.setupLocalVideo(new VideoCanvas(videoView, RENDER_MODE_HIDDEN, LOCAL_RTC_UID));
        engine.startPreview();
    }

    public void joinChannel(String channelId, String uid, String token, boolean publish, OnChannelListener listener) {
        if (engine == null) {
            return;
        }
        if(publish && !TextUtils.isEmpty(publishChannelId)){
            throw new RuntimeException("The channel " + publishChannelId + " is publishing stream now!");
        }
        if(publish){
            int _uid = LOCAL_RTC_UID;
            if(!TextUtils.isEmpty(uid)){
                try {
                    _uid = Integer.parseInt(uid);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishCameraTrack = publish;
            options.publishAudioTrack = publish;
            engine.setClientRole(publish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE);
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
        }else{
            RtcConnection rtcConnection = connectionMap.get(channelId);
            if (rtcConnection != null) {
                engine.leaveChannelEx(rtcConnection);
            }
            RtcConnection connection = new RtcConnection();
            int localUid = 0;
            try {
                localUid = Integer.parseInt(uid);
            } catch (NumberFormatException e) {
                localUid = new Random(System.currentTimeMillis()).nextInt(100) + 10000;
            }
            connection.localUid = localUid;
            connection.channelId = channelId;

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            connectionMap.put(channelId, connection);
            engine.joinChannelEx(token, connection, options, new IRtcEngineEventHandler() {
                @Override
                public void onError(int err) {
                    super.onError(err);
                    if(listener != null){
                        listener.onError(err, RtcEngine.getErrorDescription(err));
                    }
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    if(listener != null){
                        listener.onJoinSuccess(uid);
                    }
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    if(listener != null){
                        listener.onUserJoined(channelId, uid);
                    }
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    if(listener != null){
                        listener.onUserOffline(channelId, uid);
                    }
                }
            });
        }

    }

    public void leaveChannel(String channelId){
        if(channelId.equals(publishChannelId)){
            engine.leaveChannel();
            publishChannelId = "";
            publishChannelListener = null;
        }else{
            RtcConnection rtcConnection = connectionMap.get(channelId);
            if(rtcConnection != null){
                engine.leaveChannelEx(rtcConnection);
                connectionMap.remove(channelId);
            }
        }
    }

    public void renderRemoteVideo(FrameLayout container, String channelId, int uid) {
        if (engine == null) {
            return;
        }
        if(channelId.equals(publishChannelId)){
            TextureView view = new TextureView(container.getContext());
            container.removeAllViews();
            container.addView(view);
            engine.setupRemoteVideo(new VideoCanvas(view, RENDER_MODE_HIDDEN, uid));
        }else{
            RtcConnection rtcConnection = connectionMap.get(channelId);
            if(rtcConnection != null){
                TextureView view = new TextureView(container.getContext());
                container.removeAllViews();
                container.addView(view);
                engine.setupRemoteVideoEx(new VideoCanvas(view, RENDER_MODE_HIDDEN, uid), rtcConnection);
            }
        }
    }

    public void release() {
        publishChannelListener = null;
        if (engine != null) {
            RtcEngine.destroy();
        }
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
