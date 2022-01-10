package io.agora.sample.singlehostlive;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.uiwidget.function.PreviewControlView;
import io.agora.uiwidget.function.VideoSettingDialog;

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PreviewActivity";
    private RtcManager rtcManager = new RtcManager();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_host_live_preview_activity);
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                .onGranted(data -> initPreview())
                .start();


        PreviewControlView previewControlView = findViewById(R.id.preview_control_view);
        previewControlView.setBackIcon(true, v -> finish());
        previewControlView.setCameraIcon(true, v -> {
            rtcManager.switchCamera();
        });
        previewControlView.setBeautyIcon(false, null);
        previewControlView.setSettingIcon(true, v -> {
            // 视频参数设置弹窗
            List<Size> resolutions = new ArrayList<>();
            for (VideoEncoderConfiguration.VideoDimensions sVideoDimension : RtcManager.sVideoDimensions) {
                resolutions.add(new Size(sVideoDimension.width, sVideoDimension.height));
            }
            List<Integer> frameRates = new ArrayList<>();
            for (VideoEncoderConfiguration.FRAME_RATE sFrameRate : RtcManager.sFrameRates) {
                frameRates.add(sFrameRate.getValue());
            }
            new VideoSettingDialog(PreviewActivity.this)
                    .setResolutions(resolutions)
                    .setFrameRates(frameRates)
                    .setBitRateRange(0, 2000)
                    .setDefaultValues(new Size(RtcManager.encoderConfiguration.dimensions.width, RtcManager.encoderConfiguration.dimensions.height),
                            RtcManager.encoderConfiguration.frameRate, RtcManager.encoderConfiguration.bitrate)
                    .setOnValuesChangeListener(new VideoSettingDialog.OnValuesChangeListener() {
                        @Override
                        public void onResolutionChanged(Size resolution) {
                            RtcManager.encoderConfiguration.dimensions = new VideoEncoderConfiguration.VideoDimensions(resolution.getWidth(), resolution.getHeight());
                        }

                        @Override
                        public void onFrameRateChanged(int framerate) {
                            RtcManager.encoderConfiguration.frameRate = framerate;
                        }

                        @Override
                        public void onBitrateChanged(int bitrate) {
                            RtcManager.encoderConfiguration.bitrate = bitrate;
                        }
                    })
                    .show();
        });
        previewControlView.setGoLiveBtn((view, randomName) -> {
            RoomManager.getInstance().createRoom(randomName, new RoomManager.DataCallback<RoomManager.RoomInfo>() {
                @Override
                public void onSuccess(RoomManager.RoomInfo data) {
                    Intent intent = new Intent(PreviewActivity.this, HostDetailActivity.class);
                    intent.putExtra("roomInfo", data);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailed(Exception e) {
                    Log.e(TAG, "", e);
                }
            });

        });
    }

    private void initPreview() {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);

        FrameLayout surfaceViewContainer = findViewById(R.id.surface_view_container);
        rtcManager.renderLocalVideo(surfaceViewContainer, null);

    }

    @Override
    public void finish() {
        rtcManager.release();
        super.finish();
    }

}
