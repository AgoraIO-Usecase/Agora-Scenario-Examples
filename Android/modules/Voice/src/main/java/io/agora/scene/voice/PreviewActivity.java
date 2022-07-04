package io.agora.scene.voice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.scene.voice.widgets.BackgroundDialog;
import io.agora.uiwidget.function.PreviewControlView;
import io.agora.uiwidget.utils.StatusBarUtil;

public class PreviewActivity extends AppCompatActivity {
    private ImageView bgIv;
    private int backgroundImaRes = BackgroundDialog.BG_PIC_RES[0];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_preview_activity);
        StatusBarUtil.hideStatusBar(getWindow(), false);

        bgIv = findViewById(R.id.iv_background);
        bgIv.setImageResource(backgroundImaRes);

        PreviewControlView previewControlView = findViewById(R.id.preview_control_view);
        previewControlView.setBackIcon(true, v -> finish());
        previewControlView.setCameraIcon(true, R.drawable.voice_preview_ic_bg, v -> {
            showBgSelectDialog();
        });
        previewControlView.setBeautyIcon(false, null);
        previewControlView.setSettingIcon(false, null);
        previewControlView.setGoLiveBtn((view, randomName) -> {
            RoomManager.getInstance().createRoom(randomName, backgroundImaRes, data -> {
                Intent intent = new Intent(PreviewActivity.this, RoomDetailActivity.class);
                intent.putExtra("roomInfo", data);
                startActivity(intent);
                finish();
            });
        });
    }

    private void showBgSelectDialog(){
        BackgroundDialog dialog = new BackgroundDialog(this);
        dialog.setOnBackgroundActionListener((index, res) -> {
            backgroundImaRes = res;
            bgIv.setImageResource(backgroundImaRes);
        });
        dialog.show();
    }


}
