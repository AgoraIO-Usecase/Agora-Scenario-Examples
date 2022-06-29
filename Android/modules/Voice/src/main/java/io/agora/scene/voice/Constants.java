package io.agora.scene.voice;

public class Constants {
    // 空间音效
    public static final int[] VOICE_EFFECT_ROOM_ACOUSTICS = {
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_KTV,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_VOCAL_CONCERT,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_STUDIO,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_PHONOGRAPH,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_VIRTUAL_STEREO,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_SPACIAL,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_ETHEREAL,
            io.agora.rtc2.Constants.ROOM_ACOUSTICS_3D_VOICE
    };
    // 变音特效
    public static final int[] VOICE_EFFECT_VOICE_CHANGER = {
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_UNCLE,
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_OLDMAN,
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_BOY,
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_SISTER,
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_GIRL,
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_PIGKING,
            io.agora.rtc2.Constants.VOICE_CHANGER_EFFECT_HULK
    };
    // 曲风音效
    public static final int[] VOICE_EFFECT_STYLE_TRANSFORMATION = {
            io.agora.rtc2.Constants.STYLE_TRANSFORMATION_RNB,
            io.agora.rtc2.Constants.STYLE_TRANSFORMATION_POPULAR,
    };

    // 电音音效
    public static final int VOICE_EFFECT_PITCH_CORRECTION = io.agora.rtc2.Constants.PITCH_CORRECTION;
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
            io.agora.rtc2.Constants.CHAT_BEAUTIFIER_MAGNETIC,
            io.agora.rtc2.Constants.CHAT_BEAUTIFIER_FRESH,
            io.agora.rtc2.Constants.CHAT_BEAUTIFIER_VITALITY,
    };

    // 歌唱美声
    public static int VOICE_BEAUTIFIER_SINGING = io.agora.rtc2.Constants.SINGING_BEAUTIFIER;

    // 音色变换
    public static int[] VOICE_BEAUTIFIER_TRANASFORMATION = {
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_VIGOROUS,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_DEEP,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_MELLOW,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_FALSETTO,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_FULL,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_CLEAR,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RESOUNDING,
            io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING
    };
}
