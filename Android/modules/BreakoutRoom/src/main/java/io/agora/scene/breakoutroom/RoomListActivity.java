package io.agora.scene.breakoutroom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.uiwidget.basic.TitleBar;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomListActivity extends AppCompatActivity {
    private final String TAG = "RoomListActivity";
    private RoomListView roomListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.breakout_room_list_activity);
        StatusBarUtil.hideStatusBar(getWindow(), false);

        RoomManager.getInstance().init(this, getString(R.string.sync_app_id), data -> {
            runOnUiThread(() -> Toast.makeText(RoomListActivity.this, data.getMessage(), Toast.LENGTH_SHORT).show());
        });

        roomListView = findViewById(R.id.room_list_view);
        roomListView.setListAdapter(new RoomListView.AbsRoomListAdapter<RoomManager.RoomInfo>() {

            @Override
            protected void onItemUpdate(RoomListView.RoomListItemViewHolder holder, RoomManager.RoomInfo item) {
                holder.bgView.setBackgroundResource(item.getAndroidBgId());
                holder.participantsLayout.setVisibility(View.GONE);
                holder.roomName.setText(item.id);
                holder.roomInfo.setText(item.userId);
                holder.itemView.setOnClickListener(v -> gotoLiveDetailPage(item));
            }

            @Override
            protected void onRefresh() {
                RoomManager.getInstance().getAllRooms(dataList -> runOnUiThread(() -> {
                    mDataList.clear();
                    mDataList.addAll(dataList);
                    notifyDataSetChanged();
                    triggerDataListUpdateRun();
                }));
            }

            @Override
            protected void onLoadMore() {

            }
        });

        TitleBar titleBar = findViewById(R.id.title_bar);
        titleBar.setTitleName(!TextUtils.isEmpty(getIntent().getStringExtra("label")) ? getIntent().getStringExtra("label") : getResources().getString(R.string.breakout_room_app_name), 0);
        titleBar.setUserIcon(false, 0, null);
        titleBar.setDeliverVisible(false);
        titleBar.setBackIcon(!TextUtils.isEmpty(getIntent().getStringExtra("from")), R.drawable.title_bar_back_white, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView startLiveIv = findViewById(R.id.btn_start_live);
        startLiveIv.setOnClickListener(v -> showRoomCreateDialog());
    }


    private void showRoomCreateDialog() {
        EditText etChannelName = new EditText(this);
        etChannelName.setHint(R.string.preview_control_name_enter_hint);
        new AlertDialog.Builder(this)
                .setTitle(R.string.breakout_room_create_room)
                .setView(etChannelName)
                .setPositiveButton(R.string.common_create, (dialog, which) -> {
                    String channelName = etChannelName.getText().toString();
                    if (TextUtils.isEmpty(channelName)) {
                        Toast.makeText(RoomListActivity.this, R.string.breakout_room_name_empty_tip, Toast.LENGTH_SHORT).show();
                    } else {
                        RoomManager.getInstance().createRoom(channelName, data -> {
                            roomListView.refreshData();
                        });
                    }
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void gotoLiveDetailPage(RoomManager.RoomInfo roomInfo) {
        Intent intent = new Intent(RoomListActivity.this, LiveDetailActivity.class);
        intent.putExtra("roomInfo", roomInfo);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        RoomManager.getInstance().destroy();
        super.onDestroy();
    }
}
