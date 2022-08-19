package io.agora.scene.interactiveblog;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import io.agora.rtc2.RtcEngine;
import io.agora.uiwidget.function.PreviewControlView;
import io.agora.uiwidget.utils.StatusBarUtil;

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PreviewActivity";
    private RtcEngine rtcEngine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interactive_blog_preview_activity);
        StatusBarUtil.hideStatusBar(getWindow(), false);

        PreviewControlView previewControlView = findViewById(R.id.preview_control_view);
        previewControlView.setBackIcon(true, v -> finish());
        previewControlView.setCameraIcon(false, null);
        previewControlView.setSettingIcon(false, null);
        previewControlView.setGoLiveBtn((view, randomName) -> {
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.RECORD_AUDIO)
                    .onGranted(data -> createRoom(randomName))
                    .start();
        });
    }

    private void createRoom(String randomName) {
        RoomManager.getInstance().createRoom(new RoomManager.RoomInfo(randomName), data -> {
            Intent intent = new Intent(PreviewActivity.this, LiveDetailActivity.class);
            intent.putExtra("roomInfo", data);
            startActivity(intent);
            finish();
        });
    }

}
