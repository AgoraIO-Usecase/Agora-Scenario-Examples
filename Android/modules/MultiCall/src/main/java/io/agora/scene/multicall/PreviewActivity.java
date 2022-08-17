package io.agora.scene.multicall;

import android.content.Intent;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.uiwidget.function.PreviewControlView;
import io.agora.uiwidget.function.VideoSettingDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PreviewActivity";
    private RtcEngine rtcEngine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_call_preview_activity);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                .onGranted(data -> initPreview())
                .start();


        PreviewControlView previewControlView = findViewById(R.id.preview_control_view);
        previewControlView.setBackIcon(true, v -> finish());
        previewControlView.setCameraIcon(true, v -> {
            if (rtcEngine != null) {
                rtcEngine.switchCamera();
            }
        });
        previewControlView.setSettingIcon(true, v -> showVideoSettingDialog());
        previewControlView.setGoLiveBtn((view, randomName) -> createRoom(randomName));
    }

    private void createRoom(String randomName) {
        RoomManager.getInstance().createRoom(new RoomManager.RoomInfo(randomName), data -> {
            Intent intent = new Intent(PreviewActivity.this, LiveDetailActivity.class);
            intent.putExtra("roomInfo", data);
            startActivity(intent);
            finish();
        });
    }

    private void showVideoSettingDialog() {
        // 视频参数设置弹窗
        List<Size> resolutions = new ArrayList<>();
        for (VideoEncoderConfiguration.VideoDimensions sVideoDimension : Constants.sVideoDimensions) {
            resolutions.add(new Size(sVideoDimension.width, sVideoDimension.height));
        }
        List<Integer> frameRates = new ArrayList<>();
        for (VideoEncoderConfiguration.FRAME_RATE sFrameRate : Constants.sFrameRates) {
            frameRates.add(sFrameRate.getValue());
        }
        new VideoSettingDialog(PreviewActivity.this)
                .setResolutions(resolutions)
                .setFrameRates(frameRates)
                .setBitRateRange(0, 2000)
                .setDefaultValues(new Size(Constants.encoderConfiguration.dimensions.width, Constants.encoderConfiguration.dimensions.height),
                        Constants.encoderConfiguration.frameRate, Constants.encoderConfiguration.bitrate)
                .setOnValuesChangeListener(new VideoSettingDialog.OnValuesChangeListener() {
                    @Override
                    public void onResolutionChanged(Size resolution) {
                        Constants.encoderConfiguration.dimensions = new VideoEncoderConfiguration.VideoDimensions(resolution.getWidth(), resolution.getHeight());
                    }

                    @Override
                    public void onFrameRateChanged(int framerate) {
                        Constants.encoderConfiguration.frameRate = framerate;
                    }

                    @Override
                    public void onBitrateChanged(int bitrate) {
                        Constants.encoderConfiguration.bitrate = bitrate;
                    }
                })
                .show();
    }

    private void initPreview() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {});

            FrameLayout surfaceViewContainer = findViewById(R.id.surface_view_container);
            surfaceViewContainer.removeAllViews();
            SurfaceView videoView = new SurfaceView(this);
            surfaceViewContainer.addView(videoView);
            rtcEngine.setupLocalVideo(new VideoCanvas(videoView, io.agora.rtc2.Constants.RENDER_MODE_HIDDEN));

            rtcEngine.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        rtcEngine.stopPreview();
        RtcEngine.destroy();
        super.finish();
    }

}
