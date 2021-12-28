package io.agora.scene.singlehostlive;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

import io.agora.uiwidget.basic.TitleBar;
import io.agora.uiwidget.function.RoomListView;

public class RoomListActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_room_list_activity);

        RoomListView roomListView = findViewById(R.id.room_list_view);
        roomListView.setListAdapter(new RoomListView.AbsRoomListAdapter<RoomInfo>() {

            @Override
            protected void onItemUpdate(RoomListView.RoomListItemViewHolder holder, RoomInfo item) {
                holder.bgView.setBackgroundColor(item.bgColor);
                holder.participantsCount.setText(String.valueOf(item.userCount));
                holder.roomName.setText(item.roomName);
            }

            @Override
            protected void onRefresh() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int startPos = mDataList.size();
                        List<RoomInfo> addList = Arrays.asList(
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000"))
                        );
                        mDataList.addAll(addList);
                        notifyItemRangeInserted(startPos, addList.size());
                        triggerDataListUpdateRun();
                    }
                }, 1000);
            }

            @Override
            protected void onLoadMore() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int startPos = mDataList.size();
                        List<RoomInfo> addList = Arrays.asList(
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000")),
                                new RoomInfo("1111", 10, Color.parseColor("#FF0000"))
                        );
                        mDataList.addAll(addList);
                        notifyItemRangeInserted(startPos, addList.size());
                        triggerDataListUpdateRun();
                    }
                }, 1000);
            }
        });

        TitleBar titleBar = findViewById(R.id.title_bar);
        titleBar.setTitleName(getResources().getString(R.string.app_name), 0);
        titleBar.setBgDrawable(io.agora.uiwidget.R.drawable.title_bar_bg_colorful);
        titleBar.setUserIcon(true, 0, v -> {
            startActivity(new Intent(RoomListActivity.this, UserProfileActivity.class));
        });

        ImageView startLiveIv = findViewById(R.id.btn_start_live);
        startLiveIv.setOnClickListener(v -> {
            startActivity(new Intent(RoomListActivity.this, PreviewActivity.class));
        });
    }
}
