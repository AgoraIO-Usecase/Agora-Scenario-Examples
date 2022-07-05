package io.agora.scene.club;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.Locale;
import java.util.Random;

import io.agora.example.base.BaseActivity;
import io.agora.scene.club.RoomManager.RoomInfo;
import io.agora.scene.club.databinding.ClubRoomListActivityBinding;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomListActivity extends BaseActivity<ClubRoomListActivityBinding> {
    private final static String[] VIDEO_URLS = new String[]{
//            "https://webdemo.agora.io/agora-web-showcase/examples/Agora-Custom-VideoSource-Web/assets/sample.mp4",
            "https://webdemo-pull-hdl.agora.io/lbhd/sample1.flv",
            "https://webdemo-pull-hdl.agora.io/lbhd/sample2.flv",
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        RoomManager.getInstance().init(this, getString(R.string.rtm_app_id), getString(R.string.rtm_app_token),
                ex -> runOnUiThread(()->Toast.makeText(RoomListActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show()));

        mBinding.titleBar
                .setBgDrawable(R.drawable.club_main_title_bar_bg)
                .setDeliverVisible(false)
                .setTitleName(getString(R.string.club_room_list_title), getResources().getColor(R.color.club_title_bar_text_color))
                .setBackIcon(true, R.drawable.club_ic_arrow_24, v -> finish());
        mBinding.btnStartLive.setOnClickListener(v -> checkPermission(this::showRoomCreateDialog));
        mBinding.roomListView.setListAdapter(new RoomListAdapter(){
            @Override
            protected void onRefresh() {
                RoomManager.getInstance().getAllRooms(dataList -> runOnUiThread(() -> {
                    removeAll();
                    insertAll(dataList);
                    triggerDataListUpdateRun();
                }));
            }
        });
    }

    @SuppressLint("WrongConstant")
    private void checkPermission(Runnable granted) {
        String[] permissions = {Permission.CAMERA, Permission.RECORD_AUDIO};
        if (AndPermission.hasPermissions(this, permissions)) {
            if (granted != null) {
                granted.run();
            }
            return;
        }
        AndPermission.with(this)
                .runtime()
                .permission(permissions)
                .onGranted(data -> {
                    if (granted != null) {
                        granted.run();
                    }
                })
                .onDenied(data -> Toast.makeText(RoomListActivity.this, "The permission request failed.", Toast.LENGTH_SHORT).show())
                .start();
    }


    private void showRoomCreateDialog() {
        String roomId = RoomManager.getRandomRoomId();

        View contentView = LayoutInflater.from(this).inflate(R.layout.club_room_create_dialog, null);
        EditText contentEt = contentView.findViewById(R.id.et_content);
        ImageView refreshIv = contentView.findViewById(R.id.iv_refresh);
        TextView roomIdTv = contentView.findViewById(R.id.tv_roomId);
        refreshIv.setOnClickListener(v -> contentEt.setText(RandomUtil.randomLiveRoomName(RoomListActivity.this)));
        contentEt.setText(RandomUtil.randomLiveRoomName(RoomListActivity.this));
        roomIdTv.setText(String.format(Locale.US, "roomId: %s", roomId));

        new AlertDialog.Builder(this)
                .setTitle(R.string.club_room_create_dialog_title)
                .setView(contentView)
                .setPositiveButton(R.string.common_create, (dialog, which) -> {
                    String roomName = contentEt.getText().toString();
                    if (TextUtils.isEmpty(roomName)) {
                        Toast.makeText(RoomListActivity.this, "Room name should not be null.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    // 创建房间
                    RoomInfo roomInfo = new RoomInfo(roomName);
                    roomInfo.roomId = roomId;
                    roomInfo.videoUrl = VIDEO_URLS[new Random().nextInt(VIDEO_URLS.length)];
                    RoomManager.getInstance().createRoom(roomInfo, this::gotoRoomDetailPage);
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();

    }

    private void gotoRoomDetailPage(RoomInfo roomInfo) {
        Intent intent = new Intent(this, RoomDetailActivity.class);
        intent.putExtra("roomInfo", roomInfo);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        RoomManager.getInstance().destroy();
        super.onDestroy();
    }
}
