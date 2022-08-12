package io.agora.scene.multicall;

import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_1;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_10;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_7;
import static io.agora.rtc.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_120x120;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_1280x720;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_160x120;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_180x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_240x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_240x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_320x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_320x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_360x360;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_424x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_480x360;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_480x480;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x360;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x480;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_840x480;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_960x720;

import java.util.Arrays;
import java.util.List;

import io.agora.rtc.video.CameraCapturerConfiguration;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class Constants {

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

    public static CameraCapturerConfiguration.CAMERA_DIRECTION cameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT;

    public static final VideoEncoderConfiguration encoderConfiguration = new VideoEncoderConfiguration(
            VD_640x360,
            FRAME_RATE_FPS_15,
            700,
            ORIENTATION_MODE_FIXED_PORTRAIT
    );

}
