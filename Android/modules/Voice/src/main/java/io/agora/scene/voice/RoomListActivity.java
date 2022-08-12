package io.agora.scene.voice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import io.agora.example.base.BaseActivity;
import io.agora.scene.voice.databinding.VoiceRoomListActivityBinding;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomListActivity extends BaseActivity<VoiceRoomListActivityBinding> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        RoomManager.getInstance().init(this, getString(R.string.rtm_app_id), getString(R.string.rtm_app_token),
                ex -> runOnUiThread(() -> Toast.makeText(RoomListActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show()));

        mBinding.titleBar
                .setTitleName(getString(R.string.voice_room_list_title), Color.WHITE)
                .setBackIcon(false, 0, null)
                .setDeliverVisible(false)
                .setBackIcon(!TextUtils.isEmpty(getIntent().getStringExtra("from")), R.drawable.title_bar_back_white, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
        mBinding.roomListView.setListAdapter(new RoomListView.AbsRoomListAdapter<RoomManager.RoomInfo>() {
            @Override
            protected void onItemUpdate(RoomListView.RoomListItemViewHolder holder, RoomManager.RoomInfo item) {
                holder.bgView.setBackgroundResource(R.drawable.random_icon2_01);
                holder.participantsLayout.setVisibility(View.GONE);
                holder.roomName.setText(item.roomName);
                holder.roomInfo.setText(item.roomId);
                holder.itemView.setOnClickListener(v -> gotoRoomDetailPage(item));
            }

            @Override
            protected void onRefresh() {
                RoomManager.getInstance().getAllRooms(dataList -> runOnUiThread(() -> {
                    removeAll();
                    insertAll(dataList);
                    triggerDataListUpdateRun();
                }));
            }

            @Override
            protected void onLoadMore() {

            }
        });

        mBinding.btnStartLive.setOnClickListener(v -> checkPermission(this::gotoPreviewPage));

    }

    @SuppressLint("WrongConstant")
    private void checkPermission(Runnable granted) {
        String[] permissions = {Permission.RECORD_AUDIO};
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

    private void gotoPreviewPage(){
        Intent intent = new Intent(this, PreviewActivity.class);
        startActivity(intent);
    }


    private void gotoRoomDetailPage(RoomManager.RoomInfo roomInfo) {
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
