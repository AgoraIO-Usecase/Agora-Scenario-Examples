package io.agora.scene.pklivebycdn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.scene.pklivebycdn.databinding.SuperappPreviewActivityBinding;
import io.agora.uiwidget.function.VideoSettingDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class PreviewActivity extends BaseActivity<SuperappPreviewActivityBinding> {
    private static final String TAG = "PreviewActivity";
    private RtcEngine rtcEngine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                .onGranted(data -> initPreview())
                .start();

        mBinding.previewControlView.setBackIcon(true, v -> finish());
        mBinding.previewControlView.setCameraIcon(true, v -> {
            if (rtcEngine != null) {
                rtcEngine.switchCamera();
            }
        });
        mBinding.previewControlView.setSettingIcon(true, v -> {
            showVideoSettingDialog();
        });
        mBinding.previewControlView.setGoLiveBtn((view, randomName) -> {
            int pushMode = mBinding.livePrepareModeChoice.getCheckedRadioButtonId() == R.id.rb_mode_direct_cdn ? RoomManager.PUSH_MODE_DIRECT_CDN : RoomManager.PUSH_MODE_RTC;

            RoomManager.getInstance().createRoom(randomName, pushMode, data -> runOnUiThread(() -> {
                Intent intent = new Intent(PreviewActivity.this, pushMode == RoomManager.PUSH_MODE_DIRECT_CDN ? RskHostActivity.class : RtcHostActivity.class);
                intent.putExtra("roomInfo", data);
                startActivity(intent);
                finish();
            }));

        });

        mBinding.livePrepareModeChoice.setOnCheckedChangeListener((group, checkedId) -> {
            int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                RadioButton childAt = (RadioButton) group.getChildAt(i);
                if (childAt.getId() == checkedId) {
                    childAt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.superapp_check_icon);
                } else {
                    childAt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });
        mBinding.livePrepareModeChoice.check(R.id.rb_mode_direct_cdn);
    }

    private void showVideoSettingDialog() {
        // 视频参数设置弹窗
        List<Size> resolutions = new ArrayList<>();
        for (VideoEncoderConfiguration.VideoDimensions sVideoDimension : io.agora.scene.pklivebycdn.Constants.sVideoDimensions) {
            resolutions.add(new Size(sVideoDimension.width, sVideoDimension.height));
        }
        List<Integer> frameRates = new ArrayList<>();
        for (VideoEncoderConfiguration.FRAME_RATE sFrameRate : io.agora.scene.pklivebycdn.Constants.sFrameRates) {
            frameRates.add(sFrameRate.getValue());
        }
        new VideoSettingDialog(PreviewActivity.this)
                .setResolutions(resolutions)
                .setFrameRates(frameRates)
                .setBitRateRange(0, 2000)
                .setDefaultValues(
                        new Size(
                                io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions.width,
                                io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions.height
                        ),
                        io.agora.scene.pklivebycdn.Constants.encoderConfiguration.frameRate,
                        io.agora.scene.pklivebycdn.Constants.encoderConfiguration.bitrate)
                .setOnValuesChangeListener(new VideoSettingDialog.OnValuesChangeListener() {
                    @Override
                    public void onResolutionChanged(Size resolution) {
                        io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions = new VideoEncoderConfiguration.VideoDimensions(resolution.getWidth(), resolution.getHeight());
                    }

                    @Override
                    public void onFrameRateChanged(int framerate) {
                        io.agora.scene.pklivebycdn.Constants.encoderConfiguration.frameRate = framerate;
                    }

                    @Override
                    public void onBitrateChanged(int bitrate) {
                        io.agora.scene.pklivebycdn.Constants.encoderConfiguration.bitrate = bitrate;
                    }
                })
                .show();
    }

    private void initPreview() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
            });

            FrameLayout surfaceViewContainer = findViewById(R.id.surface_view_container);
            SurfaceView videoView = new SurfaceView(this);
            surfaceViewContainer.removeAllViews();
            surfaceViewContainer.addView(videoView);
            rtcEngine.setupLocalVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN));

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
