package io.agora.scene.interactiveblog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.List;
import java.util.Locale;

import io.agora.example.base.BaseActivity;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogRoomListActivityBinding;
import io.agora.uiwidget.basic.TitleBar;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomListActivity extends BaseActivity<InteractiveBlogRoomListActivityBinding> implements ChatManager.OnEventListener{
    private final String TAG = "RoomListActivity";

    private final ChatManager chatManager = ChatManager.getInstance();
    private int broadcastCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);

        RoomManager.getInstance().init(this, getString(R.string.sync_app_id), data -> {
            runOnUiThread(() -> Toast.makeText(RoomListActivity.this, data.getMessage(), Toast.LENGTH_SHORT).show());
        });

        mBinding.roomListView.setListAdapter(new RoomListView.AbsRoomListAdapter<RoomManager.RoomInfo>() {

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
        titleBar.setTitleName(!TextUtils.isEmpty(getIntent().getStringExtra("label"))? getIntent().getStringExtra("label"): getResources().getString(R.string.interactive_blog_app_name), 0);
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
        AndPermission.with(this)
                .runtime()
                .permission(Permission.RECORD_AUDIO)
                .onGranted(data -> startActivity(new Intent(RoomListActivity.this, PreviewActivity.class)))
                .start();
    }

    private void gotoAudiencePage(RoomManager.RoomInfo roomInfo) {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.RECORD_AUDIO)
                .onGranted(data -> {
                    Intent intent = new Intent(RoomListActivity.this, LiveDetailActivity.class);
                    intent.putExtra("roomInfo", roomInfo);
                    startActivity(intent);
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        chatManager.removeOnEventListener(this);
        chatManager.leaveRoom();
        RoomManager.getInstance().destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.roomListView.refreshData();
        resetMinLayout();
    }

    private void resetMinLayout() {
        if (!chatManager.isRoomJoined()) {
            mBinding.llMin.setVisibility(View.GONE);
            mBinding.btnStartLive.setVisibility(View.VISIBLE);
            return;
        }
        mBinding.btnStartLive.setVisibility(View.GONE);
        mBinding.llMin.setVisibility(View.VISIBLE);
        mBinding.llMin.setOnClickListener(v -> gotoAudiencePage(chatManager.getRoomInfo()));
        mBinding.ivExit.setOnClickListener(v -> {
            chatManager.leaveRoom();
            chatManager.removeOnEventListener(this);
            mBinding.btnStartLive.setVisibility(View.VISIBLE);
            mBinding.llMin.setVisibility(View.GONE);
            mBinding.roomListView.refreshData();
        });
        mBinding.ivHandUp.setVisibility(chatManager.getLocalUserInfo().status == RoomManager.Status.ACCEPT ? View.GONE: View.VISIBLE);
        mBinding.ivAudio.setVisibility(chatManager.getLocalUserInfo().status == RoomManager.Status.ACCEPT ? View.VISIBLE: View.GONE);
        mBinding.ivAudio.setActivated(chatManager.getLocalUserInfo().isEnableAudio);
        mBinding.ivNews.setVisibility(chatManager.localIsRoomOwner() ? View.VISIBLE : View.GONE);
        mBinding.ivNews.setOnClickListener(v -> chatManager.showHandUpListDialog());
        mBinding.ivAudio.setOnClickListener(v -> {
            boolean activated = mBinding.ivAudio.isActivated();
            mBinding.ivAudio.setActivated(!activated);
            chatManager.enableLocalAudio(!activated);
        });
        mBinding.ivHandUp.setOnClickListener(v -> chatManager.handUp());
        chatManager.setOnEventListener(this);
        chatManager.tryToFlushUserList();
    }


    @Override
    public void onAudiencesChanged(List<RoomManager.UserInfo> userInfoList) {
        mBinding.tvNumbers.setText(String.format(Locale.US, "%d/%d", broadcastCount, broadcastCount+ userInfoList.size()));
    }

    @Override
    public void onAudienceHandUpChanged(List<RoomManager.UserInfo> userInfoList) {
        mBinding.tvHandUpNewsCount.setVisibility(userInfoList.size()> 0? View.VISIBLE : View.GONE);
        mBinding.tvHandUpNewsCount.setText(userInfoList.size() + "");
    }

    @Override
    public void onBroadcastsChanged(List<RoomManager.UserInfo> userInfoList) {
        broadcastCount = userInfoList.size();

        ImageView[] userIvs = new ImageView[]{
                mBinding.ivUser1,
                mBinding.ivUser2,
                mBinding.ivUser3
        };
        for (ImageView userIv : userIvs) {
            userIv.setVisibility(View.GONE);
        }
        int length = Math.min(3, userInfoList.size());
        for (int i = 0; i < length; i++) {
            RoomManager.UserInfo userInfo = userInfoList.get(i);
            userIvs[i].setVisibility(View.VISIBLE);
            userIvs[i].setImageResource(RandomUtil.getIconById(userInfo.userId));
        }
    }

    @Override
    public void onLocalUserChanged(RoomManager.UserInfo userInfo) {
        mBinding.ivHandUp.setVisibility(userInfo.status == RoomManager.Status.ACCEPT ? View.GONE: View.VISIBLE);
        mBinding.ivAudio.setVisibility(userInfo.status == RoomManager.Status.ACCEPT ? View.VISIBLE: View.GONE);
        mBinding.ivAudio.setActivated(userInfo.isEnableAudio);
    }

    @Override
    public void onRoomDestroyed() {
        chatManager.removeOnEventListener(this);
        mBinding.btnStartLive.setVisibility(View.VISIBLE);
        mBinding.llMin.setVisibility(View.GONE);
    }

    @Override
    public Context getContext() {
        return this;
    }

}
