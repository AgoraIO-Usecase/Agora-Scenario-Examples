package io.agora.livepk;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

public class LivePKMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_join).setOnClickListener(v -> {
            joinRoom();
        });
    }


    private void joinRoom() {
        String roomName = ((EditText) findViewById(R.id.et_room_name)).getText().toString();
        if (TextUtils.isEmpty(roomName)) {
            Toast.makeText(this, "The room name is empty", Toast.LENGTH_LONG).show();
            return;
        }
        boolean isHost = ((Switch) findViewById(R.id.switch_host)).isChecked();

        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE, Permission.Group.MICROPHONE, Permission.Group.CAMERA)
                .onGranted(data -> {
                    startActivity(isHost ?
                            HostPKActivity.launch(this, roomName)
                            : AudienceActivity.launch(this, roomName));
                })
                .start();
    }


}