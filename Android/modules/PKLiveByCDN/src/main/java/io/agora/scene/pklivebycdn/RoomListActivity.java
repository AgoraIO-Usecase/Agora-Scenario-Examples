package io.agora.scene.pklivebycdn;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import io.agora.example.base.BaseActivity;
import io.agora.scene.pklivebycdn.databinding.SuperappRoomListActivityBinding;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomListActivity extends BaseActivity<SuperappRoomListActivityBinding> {
    private final String TAG = "RoomListActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RoomManager.getInstance().init(this, getString(R.string.rtm_app_id), getString(R.string.rtm_app_token));
        StatusBarUtil.hideStatusBar(getWindow(), false);

        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                .start();

        mBinding.roomListView.setListAdapter(new RoomListView.AbsRoomListAdapter<RoomManager.RoomInfo>() {
            @Override
            protected void onItemUpdate(RoomListView.RoomListItemViewHolder holder, RoomManager.RoomInfo item) {
                holder.roomName.setText(item.roomName);
                holder.roomInfo.setText(item.roomId);
                holder.bgView.setBackgroundResource(item.getBgResId());
                holder.participantsLayout.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> {
                    gotoAudiencePage(item);
                });
            }

            @Override
            protected void onRefresh() {
                RoomManager.getInstance().getAllRooms(dataList -> {
                    mDataList.clear();
                    mDataList.addAll(dataList);
                    notifyDataSetChanged();
                    triggerDataListUpdateRun();
                });
            }

            @Override
            protected void onLoadMore() {

            }
        });

        mBinding.titleBar.setTitleName(getResources().getString(R.string.superapp_app_name), 0);
        mBinding.titleBar.setUserIcon(false, 0, null);
        mBinding.titleBar.setDeliverVisible(false);
        mBinding.titleBar.setBackIcon(!TextUtils.isEmpty(getIntent().getStringExtra("from")), R.drawable.title_bar_back_white, new View.OnClickListener() {
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
        Intent intent = new Intent(RoomListActivity.this, roomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN ? RskAudienceActivity.class: RtcAudienceActivity.class);
        intent.putExtra("roomInfo", roomInfo);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        RoomManager.getInstance().destroy();
        super.onDestroy();
    }
}
