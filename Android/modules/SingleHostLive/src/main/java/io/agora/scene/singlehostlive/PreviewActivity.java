package io.agora.scene.singlehostlive;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

import io.agora.example.base.BaseUtil;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.uiwidget.function.PreviewControlView;
import io.agora.uiwidget.function.VideoSettingDialog;

public class PreviewActivity extends AppCompatActivity {

    private final String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private RtcEngine rtcEngine;

    private final BaseUtil.PermissionResultCallback<String[]> resultCallback = this::initPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_preview_activity);
        initView();
        BaseUtil.checkPermissionBeforeNextOP(this, permissions, resultCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void initView(){
        PreviewControlView previewControlView = findViewById(R.id.preview_control_view);
        previewControlView.setBackIcon(true, v -> finish());
        previewControlView.setCameraIcon(true, v -> {
            if(rtcEngine != null){
                rtcEngine.switchCamera();
            }
        });
        previewControlView.setBeautyIcon(false, null);
        previewControlView.setSettingIcon(true, v -> {
            // 视频参数设置弹窗
            new VideoSettingDialog(PreviewActivity.this)
                    .setResolutions(Arrays.asList(new Size(360, 640), new Size(720, 1080)))
                    .setFrameRates(Arrays.asList(10, 20, 30))
                    .setBitRateRange(0, 2000)
                    .setDefaultValues(new Size(360, 640), 10, 700)
                    .setOnValuesChangeListener(new VideoSettingDialog.OnValuesChangeListener() {
                        @Override
                        public void onResolutionChanged(Size resolution) {

                        }

                        @Override
                        public void onFrameRateChanged(int framerate) {

                        }

                        @Override
                        public void onBitrateChanged(int bitrate) {

                        }
                    })
                    .show();
        });
        previewControlView.setGoLiveBtn((view, randomName) -> {
            startActivity(new Intent(PreviewActivity.this, HostDetailActivity.class));
            finish();
        });
    }

    private void initPreview() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {});
            rtcEngine.enableVideo();
            FrameLayout surfaceViewContainer = findViewById(R.id.surface_view_container);
            SurfaceView videoView = RtcEngine.CreateRendererView(this);
            surfaceViewContainer.addView(videoView);
            rtcEngine.setupLocalVideo(new VideoCanvas(videoView));
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
