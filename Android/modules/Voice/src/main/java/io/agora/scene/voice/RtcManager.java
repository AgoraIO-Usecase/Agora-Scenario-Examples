package io.agora.scene.voice;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;


public class RtcManager {
    private static final String TAG = "RtcManager";
    private static final int LOCAL_RTC_UID = 0;

    private volatile boolean isInitialized = false;
    private RtcEngine engine;
    private Context mContext;
    private OnChannelListener publishChannelListener;
    private String publishChannelId;


    // 空间音效
    public static final int[] VOICE_EFFECT_ROOM_ACOUSTICS = {
            Constants.ROOM_ACOUSTICS_KTV,
            Constants.ROOM_ACOUSTICS_VOCAL_CONCERT,
            Constants.ROOM_ACOUSTICS_STUDIO,
            Constants.ROOM_ACOUSTICS_PHONOGRAPH,
            Constants.ROOM_ACOUSTICS_VIRTUAL_STEREO,
            Constants.ROOM_ACOUSTICS_SPACIAL,
            Constants.ROOM_ACOUSTICS_ETHEREAL,
            Constants.ROOM_ACOUSTICS_3D_VOICE
    };
    // 变音特效
    public static final int[] VOICE_EFFECT_VOICE_CHANGER = {
            Constants.VOICE_CHANGER_EFFECT_UNCLE,
            Constants.VOICE_CHANGER_EFFECT_OLDMAN,
            Constants.VOICE_CHANGER_EFFECT_BOY,
            Constants.VOICE_CHANGER_EFFECT_SISTER,
            Constants.VOICE_CHANGER_EFFECT_GIRL,
            Constants.VOICE_CHANGER_EFFECT_PIGKING,
            Constants.VOICE_CHANGER_EFFECT_HULK
    };
    // 曲风音效
    public static final int[] VOICE_EFFECT_STYLE_TRANSFORMATION = {
            Constants.STYLE_TRANSFORMATION_RNB,
            Constants.STYLE_TRANSFORMATION_POPULAR,
    };

    // 电音音效
    public static final int VOICE_EFFECT_PITCH_CORRECTION = Constants.PITCH_CORRECTION;
    public static final int[] VOICE_EFFECT_PITCH_CORRECTION_VALUES = {
            1,//A
            2,//Ab
            3,//B
            4,//C
            5,//Cb
            6,//D
            7,//Db
            8,//E
            9,//F
            10,//Fb
            11,//G
            12 //Gb
    };

    // 语聊美声
    public static int[] VOICE_BEAUTIFIER_CHAT = {
            Constants.CHAT_BEAUTIFIER_MAGNETIC,
            Constants.CHAT_BEAUTIFIER_FRESH,
            Constants.CHAT_BEAUTIFIER_VITALITY,
    };

    // 歌唱美声
    public static int VOICE_BEAUTIFIER_SINGING = Constants.SINGING_BEAUTIFIER;

    // 音色变换
    public static int[] VOICE_BEAUTIFIER_TRANASFORMATION = {
            Constants.TIMBRE_TRANSFORMATION_VIGOROUS,
            Constants.TIMBRE_TRANSFORMATION_DEEP,
            Constants.TIMBRE_TRANSFORMATION_MELLOW,
            Constants.TIMBRE_TRANSFORMATION_FALSETTO,
            Constants.TIMBRE_TRANSFORMATION_FULL,
            Constants.TIMBRE_TRANSFORMATION_CLEAR,
            Constants.TIMBRE_TRANSFORMATION_RESOUNDING,
            Constants.TIMBRE_TRANSFORMATION_RINGING
    };

    public void init(Context context, String appId, OnInitializeListener listener) {
        if (isInitialized) {
            return;
        }
        mContext = context;
        try {
            // 0. create engine
            engine = RtcEngine.create(mContext.getApplicationContext(), appId, new IRtcEngineEventHandler() {

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
            });
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            engine.enableAudio();
            engine.setDefaultAudioRoutetoSpeakerphone(true);

            isInitialized = true;
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(-1, "RtcEngine create exception : " + e.toString());
            }
        }
    }

    public void joinChannel(String channelId, String uid, String token, boolean publish, OnChannelListener listener) {
        if (engine == null) {
            return;
        }
        int _uid = LOCAL_RTC_UID;
        if (!TextUtils.isEmpty(uid)) {
            try {
                _uid = Integer.parseInt(uid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = publish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        options.publishAudioTrack= publish;
        options.autoSubscribeAudio = true;

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
                engine.muteAllRemoteAudioStreams(false);

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

    public void setPublishAudio(boolean publish) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = publish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        options.publishAudioTrack = publish;
        options.autoSubscribeAudio = true;
        engine.updateChannelMediaOptions(options);
        engine.enableLocalAudio(publish);
    }

    public void setAudioVoiceEffect(int preset, int index, int pitchCorrectionValue) {
        if (engine == null) {
            return;
        }
        if (index >= 0 && pitchCorrectionValue >= 0) {
            engine.setAudioEffectParameters(preset, index, pitchCorrectionValue);
        } else {
            engine.setAudioEffectPreset(preset);
        }
    }

    public void setPitchCorrection(boolean isOn){
        engine.setAudioEffectPreset(isOn ? Constants.PITCH_CORRECTION: Constants.AUDIO_EFFECT_OFF);
    }

    public void setVoiceBeautifier(int preset, boolean isWoman) {
        if (engine == null) {
            return;
        }
        if (isWoman) {
            engine.setVoiceBeautifierParameters(preset, 2, 3);
        } else {
            engine.setVoiceBeautifierPreset(preset);
        }
    }

    public void startAudioMixing(String url) {
        if (engine == null) {
            return;
        }
        engine.startAudioMixing(url, true, true, 1);
    }

    public void stopAudioMixing() {
        if (engine == null) {
            return;
        }
        engine.stopAudioMixing();
    }

    public void setAudioMixingVolume(int value) {
        if (engine == null) {
            return;
        }
        engine.adjustAudioMixingVolume(value);
    }

    public void enableLocalAudio(boolean enable){
        if (engine == null) {
            return;
        }
        engine.muteLocalAudioStream(!enable);
    }

    public void enableEarMonitoring(boolean enable){
        if (engine == null) {
            return;
        }
        engine.enableInEarMonitoring(enable);
    }


    public void release() {
        if (engine != null) {
            if (!TextUtils.isEmpty(publishChannelId)) {
                engine.leaveChannel();
                publishChannelId = "";
                publishChannelListener = null;
            }
            RtcEngine.destroy();
            engine = null;
        }
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
