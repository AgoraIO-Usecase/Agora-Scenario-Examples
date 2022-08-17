package io.agora.scene.singlehostlive;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.uiwidget.basic.TitleBar;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomListActivity extends AppCompatActivity {
    private final String TAG = "RoomListActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_host_live_room_list_activity);
        StatusBarUtil.hideStatusBar(getWindow(), false);

        RoomManager.getInstance().init(this, getString(R.string.sync_app_id), data -> {
            runOnUiThread(() -> Toast.makeText(RoomListActivity.this, data.getMessage(), Toast.LENGTH_SHORT).show());
        });

        RoomListView roomListView = findViewById(R.id.room_list_view);
        roomListView.setListAdapter(new RoomListView.AbsRoomListAdapter<RoomManager.RoomInfo>() {

            @Override
            protected void onItemUpdate(RoomListView.RoomListItemViewHolder holder, RoomManager.RoomInfo item) {
                holder.bgView.setBackgroundResource(item.getAndroidBgId());
                holder.participantsLayout.setVisibility(View.GONE);
                holder.roomName.setText(item.roomName);
                holder.roomInfo.setText(item.roomId);
                holder.itemView.setOnClickListener(v -> gotoAudiencePage(item));
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
        titleBar.setTitleName(getResources().getString(R.string.single_host_live_app_name), 0);
        titleBar.setUserIcon(false, 0, null);
        titleBar.setDeliverVisible(false);
        titleBar.setBackIcon(!TextUtils.isEmpty(getIntent().getStringExtra("from")), R.drawable.title_bar_back_white, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView startLiveIv = findViewById(R.id.btn_start_live);
        startLiveIv.setOnClickListener(v -> gotoPreviewPage());
    }


    private void gotoPreviewPage() {
        startActivity(new Intent(RoomListActivity.this, PreviewActivity.class));
    }

    private void gotoAudiencePage(RoomManager.RoomInfo roomInfo) {
        Intent intent = new Intent(RoomListActivity.this, AudienceDetailActivity.class);
        intent.putExtra("roomInfo", roomInfo);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        RoomManager.getInstance().destroy();
        super.onDestroy();
    }
}
