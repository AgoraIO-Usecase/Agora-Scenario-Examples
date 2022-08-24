package io.agora.scene.interactiveblog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;

import io.agora.scene.interactiveblog.databinding.InteractiveBlogLiveAudienceSeatBinding;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogLiveBroadcastSeatBinding;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogLiveDetailActivityBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class LiveDetailActivity extends AppCompatActivity implements ChatManager.OnEventListener {

    private final ChatManager chatManager = ChatManager.getInstance();

    private InteractiveBlogLiveDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private ListAdapter<RoomManager.UserInfo, BindingViewHolder<InteractiveBlogLiveBroadcastSeatBinding>> broadcastsAdapter;
    private ListAdapter<RoomManager.UserInfo, BindingViewHolder<InteractiveBlogLiveAudienceSeatBinding>> audiencesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = InteractiveBlogLiveDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        getWindow().getDecorView().setKeepScreenOn(true);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        chatManager.setOnEventListener(this);
        initView();
        initChatManager();
    }

    private void initChatManager() {
        chatManager.joinRoom(this, roomInfo);
        chatManager.tryToFlushUserList();
    }

    @Override
    protected void onDestroy() {
        chatManager.removeOnEventListener(this);
        super.onDestroy();
    }

    private void initView() {
        // 房间信息
        mBinding.tvName.setText(roomInfo.roomName);
        mBinding.ivUser.setImageResource(RandomUtil.getIconById(roomInfo.userId));
        mBinding.ivMin.setOnClickListener(v -> finish());


        DiffUtil.ItemCallback<RoomManager.UserInfo> userDiffCallback = new DiffUtil.ItemCallback<RoomManager.UserInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull RoomManager.UserInfo oldItem,
                                           @NonNull RoomManager.UserInfo newItem) {
                return oldItem.userId.equals(newItem.userId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull RoomManager.UserInfo oldItem, @NonNull RoomManager.UserInfo newItem) {
                return oldItem.userId.equals(newItem.userId) && oldItem.isEnableAudio == newItem.isEnableAudio;
            }
        };

        // Broadcast列表
        broadcastsAdapter = new ListAdapter<RoomManager.UserInfo, BindingViewHolder<InteractiveBlogLiveBroadcastSeatBinding>>(userDiffCallback) {
            @NonNull
            @Override
            public BindingViewHolder<InteractiveBlogLiveBroadcastSeatBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new BindingViewHolder<>(InteractiveBlogLiveBroadcastSeatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<InteractiveBlogLiveBroadcastSeatBinding> holder, int position) {
                RoomManager.UserInfo userInfo = getItem(position);
                holder.binding.ivUser.setImageResource(RandomUtil.getIconById(userInfo.userId));
                holder.binding.tvName.setText(userInfo.userName);
                holder.binding.ivVoice.setActivated(userInfo.isEnableAudio);
                holder.binding.ivMaster.setVisibility(userInfo.userId.equals(roomInfo.userId) ? View.VISIBLE : View.GONE);
                holder.binding.getRoot().setOnClickListener(v -> chatManager.showBroadcastMenuDialog(userInfo));
            }
        };
        mBinding.rvSpeakers.setAdapter(broadcastsAdapter);

        // audience列表
        audiencesAdapter = new ListAdapter<RoomManager.UserInfo, BindingViewHolder<InteractiveBlogLiveAudienceSeatBinding>>(userDiffCallback) {
            @NonNull
            @Override
            public BindingViewHolder<InteractiveBlogLiveAudienceSeatBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new BindingViewHolder<>(InteractiveBlogLiveAudienceSeatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<InteractiveBlogLiveAudienceSeatBinding> holder, int position) {
                RoomManager.UserInfo userInfo = getItem(position);
                holder.binding.ivUser.setImageResource(RandomUtil.getIconById(userInfo.userId));
                holder.binding.tvName.setText(userInfo.userName);
                holder.binding.getRoot().setOnClickListener(v -> {
                    chatManager.showUserInviteDialog(userInfo);
                });
            }
        };
        mBinding.rvListeners.setAdapter(audiencesAdapter);

        // 底部控制
        mBinding.llExit.setOnClickListener(v -> {
            chatManager.leaveRoom();
            finish();
        });
        mBinding.ivAudio.setActivated(false);
        mBinding.ivAudio.setOnClickListener(v -> {
            boolean activated = mBinding.ivAudio.isActivated();
            mBinding.ivAudio.setActivated(!activated);
            chatManager.enableLocalAudio(!activated);
        });
        mBinding.ivHandUp.setOnClickListener(v -> {
            chatManager.handUp();
        });
        mBinding.flNews.setOnClickListener(v -> {
            mBinding.tvHandUpNewsCount.setVisibility(View.GONE);
            chatManager.showHandUpListDialog();
        });

        updateLayout();
    }

    @Override
    public void onBackPressed() {
        if (chatManager.localIsRoomOwner()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.common_tip)
                    .setMessage(R.string.common_tip_close_room)
                    .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                        dialog.dismiss();
                        LiveDetailActivity.super.onBackPressed();
                    })
                    .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAudiencesChanged(List<RoomManager.UserInfo> userInfoList) {
        audiencesAdapter.submitList(userInfoList);
    }

    @Override
    public void onAudienceHandUpChanged(List<RoomManager.UserInfo> userInfoList) {
        if (chatManager.localIsRoomOwner()) {
            mBinding.tvHandUpNewsCount.setVisibility(userInfoList.size() > 0 ? View.VISIBLE : View.GONE);
            mBinding.tvHandUpNewsCount.setText(userInfoList.size() + "");
        }
    }

    @Override
    public void onBroadcastsChanged(List<RoomManager.UserInfo> userInfoList) {
        broadcastsAdapter.submitList(userInfoList);
    }

    @Override
    public void onLocalUserChanged(RoomManager.UserInfo userInfo) {
        updateLayout();
    }

    private void updateLayout() {
        RoomManager.UserInfo userInfo = chatManager.getLocalUserInfo();
        if (userInfo == null) {
            return;
        }
        boolean isBroadcast = userInfo.status == RoomManager.Status.ACCEPT;
        mBinding.ivAudio.setVisibility(isBroadcast ? View.VISIBLE : View.GONE);
        mBinding.ivHandUp.setVisibility(isBroadcast ? View.GONE : View.VISIBLE);
        mBinding.flNews.setVisibility(chatManager.localIsRoomOwner() ? View.VISIBLE : View.GONE);
        mBinding.ivAudio.setActivated(userInfo.isEnableAudio);
    }

    @Override
    public void onRoomDestroyed() {
        new AlertDialog.Builder(LiveDetailActivity.this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_room_closed)
                .setCancelable(false)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    @Override
    public Context getContext() {
        return this;
    }

}
