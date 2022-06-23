package io.agora.scene.pklivebycdn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.scene.pklivebycdn.databinding.SuperappPreviewActivityBinding;
import io.agora.uiwidget.function.VideoSettingDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class PreviewActivity extends BaseActivity<SuperappPreviewActivityBinding> {
    private static final String TAG = "PreviewActivity";
    private final RtcManager rtcManager = new RtcManager();

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
            rtcManager.switchCamera();
        });
        mBinding.previewControlView.setSettingIcon(true, v -> {
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
        mBinding.previewControlView.setGoLiveBtn((view, randomName) -> {
            int pushMode = mBinding.livePrepareModeChoice.getCheckedRadioButtonId() == R.id.rb_mode_direct_cdn ? RoomManager.PUSH_MODE_DIRECT_CDN : RoomManager.PUSH_MODE_RTC;

            RoomManager.getInstance().createRoom(randomName, pushMode, new RoomManager.DataCallback<RoomManager.RoomInfo>() {
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

        mBinding.livePrepareModeChoice.setOnCheckedChangeListener((group, checkedId) -> {
            int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                RadioButton childAt = (RadioButton) group.getChildAt(i);
                if(childAt.getId() == checkedId){
                    childAt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.superapp_check_icon);
                }else{
                    childAt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });
        mBinding.livePrepareModeChoice.check(R.id.rb_mode_direct_cdn);
    }

    private void initPreview() {
        rtcManager.init(this, getString(R.string.superapp_agora_app_id), null);

        FrameLayout surfaceViewContainer = findViewById(R.id.surface_view_container);
        rtcManager.renderLocalVideo(surfaceViewContainer);
    }

    @Override
    public void finish() {
        rtcManager.release();
        super.finish();
    }
}
