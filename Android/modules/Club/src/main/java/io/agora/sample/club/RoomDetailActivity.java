package io.agora.sample.club;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.agora.example.base.BaseActivity;
import io.agora.sample.club.databinding.ClubRoomDetailActivityBinding;
import io.agora.sample.club.databinding.ClubRoomDetailMsgItemBinding;
import io.agora.sample.club.databinding.ClubRoomDetailRoomListDialogBinding;
import io.agora.sample.club.databinding.ClubRoomDetailSeatItemBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.OnlineUserListDialog;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.UIUtil;

public class RoomDetailActivity extends BaseActivity<ClubRoomDetailActivityBinding> {

    private final RtcManager rtcManager = RtcManager.getInstance();
    private final RoomManager roomManager = RoomManager.getInstance();

    private final ClubRoomDetailSeatItemBinding[] seatLayouts = new ClubRoomDetailSeatItemBinding[8];

    private RoomManager.RoomInfo roomInfo;
    private LiveRoomMessageListView.AbsMessageAdapter<RoomManager.MessageInfo, ClubRoomDetailMsgItemBinding> mMessageAdapter;
    private final RoomManager.DataCallback<RoomManager.GiftInfo> giftInfoDataCallback = new RoomManager.DataCallback<RoomManager.GiftInfo>() {
        @Override
        public void onSuccess(RoomManager.GiftInfo data) {
            runOnUiThread(() -> {
                mMessageAdapter.addMessage(new RoomManager.MessageInfo(
                        data.userId,
                        getString(R.string.live_room_message_gift_prefix),
                        data.getIconId()
                ));
                // 播放动画
                new GiftAnimPlayDialog(RoomDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });
        }
    };
    private final RoomManager.DataCallback<RoomManager.UserInfo> userAddOrUpdateCallback = data -> runOnUiThread(() -> {
        updateUserView(data);
    });

    private final RoomManager.DataCallback<RoomManager.UserInfo> userDeleteCallback = data -> runOnUiThread(() -> {
        removeUserSeatLayout(data.userId);
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        mBinding.titleBar
                .setBgDrawable(R.drawable.club_main_title_bar_bg)
                .setDeliverVisible(false)
                .setTitleName(String.format(Locale.US, "%s(%s)", roomInfo.roomName, roomInfo.roomId), getResources().getColor(R.color.club_title_bar_text_color))
                .setBackIcon(true, R.drawable.club_ic_arrow_24, v -> finish())
                .setUserIcon(true, R.drawable.club_room_detail_more, v -> {
                    // 显示播放同资源的房间列表
                    roomManager.getAllRooms(dataList -> {
                        List<RoomManager.RoomInfo> sameRooms = new ArrayList<>();
                        for (RoomManager.RoomInfo info : dataList) {
                            if (info.videoUrl.equals(roomInfo.videoUrl)) {
                                sameRooms.add(info);
                            }
                        }
                        runOnUiThread(() -> {
                            if (sameRooms.size() > 0) {
                                showSameRoomsDialog(sameRooms);
                            } else {
                                Toast.makeText(RoomDetailActivity.this, "No same room exist.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                });
        mBinding.ivFullLarge.setOnClickListener(v -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        mBinding.ivFullBack.setOnClickListener(v -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        // 座位列表
        initSeatLayout(mBinding.seat01, 0);
        initSeatLayout(mBinding.seat02, 1);
        initSeatLayout(mBinding.seat03, 2);
        initSeatLayout(mBinding.seat04, 3);
        initSeatLayout(mBinding.seat05, 4);
        initSeatLayout(mBinding.seat06, 5);
        initSeatLayout(mBinding.seat07, 6);
        initSeatLayout(mBinding.seat08, 7);

        // 底部按钮栏
        mBinding.bottomView
                .setupCloseBtn(false, null)
                .setupMoreBtn(false, null)
                // 文本输入
                .setupInputText(true, v -> showTextInputDialog())
                // 摄像头开关
                //.setFun1Visible(false)
                .setFun1Visible(true)
                .setFun1ImageResource(R.drawable.club_room_detail_ic_cam)
                .setFun1Activated(false)
                .setFun1ClickListener(v -> {
                    if(isLocalInSeat()){
                        boolean activated = !mBinding.bottomView.isFun1Activated();
                        mBinding.bottomView.setFun1Activated(activated);
                        roomManager.openUserVideo(roomInfo.roomId, new RoomManager.UserInfo(), activated);
                        rtcManager.enableLocalVideo(activated);
                    }else{
                        Toast.makeText(RoomDetailActivity.this, "Please take your seat first", Toast.LENGTH_SHORT).show();
                    }

                })
                // 麦克风开关
                //.setFun2Visible(false)
                .setFun2Visible(true)
                .setFun2ImageResource(R.drawable.club_room_detail_ic_mic)
                .setFun2Activated(false)
                .setFun2ClickListener(v -> {
                    if(isLocalInSeat()){
                        boolean activated = !mBinding.bottomView.isFun2Activated();
                        mBinding.bottomView.setFun2Activated(activated);
                        rtcManager.enableLocalAudio(activated);
                    }else{
                        Toast.makeText(RoomDetailActivity.this, "Please take your seat first", Toast.LENGTH_SHORT).show();
                    }
                })
                // 礼物
                .setFun3Visible(true)
                .setFun3ImageResource(R.drawable.club_room_detail_gift)
                .setFun3ClickListener(v -> showGiftGridDialog());

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.AbsMessageAdapter<RoomManager.MessageInfo, ClubRoomDetailMsgItemBinding>() {

            @Override
            protected void onItemUpdate(BindingViewHolder<ClubRoomDetailMsgItemBinding> holder, RoomManager.MessageInfo item, int position) {
                holder.binding.ivUserAvatar.setImageResource(RandomUtil.getIconById(item.userName));
                holder.binding.tvUserName.setText(item.userName);

                SpannableString contentSs = new SpannableString(item.content + " ");
                if (item.giftIcon != View.NO_ID) {
                    contentSs.setSpan(new ImageSpan(holder.itemView.getContext(), item.giftIcon), item.content.length(), item.content.length() + 1, SpannableString.SPAN_EXCLUSIVE_INCLUSIVE);
                }
                holder.binding.tvContent.setText(contentSs);
            }
        };
        mBinding.messageList.setAdapter(mMessageAdapter);

        initRtcManager();
        initRoomManager();
    }

    private void showSameRoomsDialog(List<RoomManager.RoomInfo> sameRooms) {
        ClubRoomDetailRoomListDialogBinding dialogBinding = ClubRoomDetailRoomListDialogBinding.inflate(LayoutInflater.from(this));
        dialogBinding.listview.setListAdapter(new RoomListAdapter() {
            @Override
            protected void onRefresh() {
                removeAll();
                insertAll(sameRooms);
                triggerDataListUpdateRun();
            }

            @Override
            protected void onItemClicked(View v, RoomManager.RoomInfo item) {
                showExitDialog(() -> {
                    finish();
                    gotoNextPageSafe(v, item);
                });
            }
        });
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialog);
        dialog.setContentView(dialogBinding.getRoot());
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        showExitDialog(RoomDetailActivity.super::onBackPressed);
    }

    private void showExitDialog(Runnable exit){
        new AlertDialog.Builder(this)
                .setTitle(R.string.club_room_detail_leave_room)
                .setMessage(R.string.club_room_detail_leave_room_msg)
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    if(exit != null){
                        exit.run();
                    }
                })
                .setNeutralButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void initSeatLayout(ClubRoomDetailSeatItemBinding p, int i) {
        UIUtil.setViewCircle(p.videoContainer);
        if (i >= 0 && i < seatLayouts.length) {
            seatLayouts[i] = p;
        }
        if (isLocalRoomOwner()) {
            p.getRoot().setOnClickListener(v -> showSeatOptionDialog(p));
        }
    }

    private void showInviteDialog(RoomManager.UserInfo data) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.club_room_detail_accept_invite)
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    roomManager.acceptUser(roomInfo.roomId, data);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.common_no, (dialog, which) -> {
                    roomManager.refuseUser(roomInfo.roomId, data);
                    dialog.dismiss();
                })
                .show();
    }

    private void showSeatOptionDialog(ClubRoomDetailSeatItemBinding p) {
        // 邀请：显示在线用户列表
        // 封麦：将用户下麦掉，改成end
        Object tag = p.getRoot().getTag();
        if (tag instanceof RoomManager.UserInfo) {
            if (((RoomManager.UserInfo) tag).userId.equals(roomInfo.userId)) {
                Toast.makeText(RoomDetailActivity.this, "The seat is owned by host", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        new AlertDialog.Builder(this)
                .setItems(R.array.club_room_detail_seat_options, (dialog, which) -> {
                    if (which == 0) {
                        showOnlineUserDialog();
                    } else {
                        // 封麦
                        if (tag instanceof RoomManager.UserInfo) {
                            roomManager.endUser(roomInfo.roomId, (RoomManager.UserInfo) tag);
                        }
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showOnlineUserDialog() {
        roomManager.getUserList(roomInfo.roomId, dataList -> runOnUiThread(() -> {
            List<RoomManager.UserInfo> showUserList = new ArrayList<>();
            for (RoomManager.UserInfo userInfo : dataList) {
                if (!userInfo.userId.equals(RoomManager.getCacheUserId()) && userInfo.status != RoomManager.Status.ACCEPT) {
                    showUserList.add(userInfo);
                }
            }
            if (showUserList.size() <= 0) {
                return;
            }

            OnlineUserListDialog dialog = new OnlineUserListDialog(RoomDetailActivity.this);
            OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo> adapter =
                    new OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo>() {
                        @Override
                        protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder,
                                                    int position, RoomManager.UserInfo item) {
                            holder.binding.tvName.setText(item.userName);
                            RoundedBitmapDrawable iconDrawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeResource(getResources(), item.getAvatarResId()));
                            iconDrawable.setCircular(true);
                            holder.binding.ivIcon.setImageDrawable(iconDrawable);

                            holder.binding.tvStatus.setOnClickListener(v -> {
                                // 发起邀请
                                roomManager.inviteUser(roomInfo.roomId, item);
                                dialog.dismiss();
                            });
                        }
                    };
            dialog.setListAdapter(adapter);
            dialog.show();
            adapter.resetAll(showUserList);
        }));
    }

    private void updateUserView(RoomManager.UserInfo data) {
        switch (data.status) {
            case RoomManager.Status.INVITE:
                if (data.userId.equals(RoomManager.getCacheUserId())) {
                    // 显示是否接受邀请弹窗
                    showInviteDialog(data);
                }
                break;
            case RoomManager.Status.ACCEPT:
                ClubRoomDetailSeatItemBinding userSeatLayout = getUserSeatLayout(data);
                if (userSeatLayout != null) {
                    userSeatLayout.ivCover.setImageResource(data.getAvatarResId());

                    int uid = Integer.parseInt(data.userId);
                    if(!data.userId.equals(RoomManager.getCacheUserId())){
                        rtcManager.playRemoteAudio(uid, true);
                    }

                    if(data.hasVideo){
                        if (data.userId.equals(RoomManager.getCacheUserId())) {
                            rtcManager.renderLocalVideo(userSeatLayout.videoContainer, null);
                        } else {
                            rtcManager.renderRemoteVideo(userSeatLayout.videoContainer, uid);
                        }
                    }else{
                        userSeatLayout.videoContainer.removeAllViews();
                    }

                }
                break;
            case RoomManager.Status.REFUSE:
                if (isLocalRoomOwner()) {
                    // 显示被拒绝弹窗
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.common_tip)
                            .setMessage(getString(R.string.club_room_detail_refuse_tip, data.userName))
                            .setPositiveButton(R.string.common_yes, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                break;
            case RoomManager.Status.END:
                removeUserSeatLayout(data.userId);
                rtcManager.playRemoteAudio(Integer.parseInt(data.userId), false);
        }
    }

    private ClubRoomDetailSeatItemBinding getUserSeatLayout(RoomManager.UserInfo userInfo) {
        for (ClubRoomDetailSeatItemBinding seatLayout : seatLayouts) {
            if (seatLayout == null) {
                continue;
            }
            Object tag = seatLayout.getRoot().getTag();
            if (tag instanceof RoomManager.UserInfo && ((RoomManager.UserInfo) tag).userId.equals(userInfo.userId)) {
                seatLayout.getRoot().setTag(userInfo);
                return seatLayout;
            }
        }
        ClubRoomDetailSeatItemBinding idleSeatLayout = getIdleSeatLayout();
        if (idleSeatLayout != null) {
            idleSeatLayout.getRoot().setTag(userInfo);
        }
        return idleSeatLayout;
    }

    private void removeUserSeatLayout(String userId) {
        for (ClubRoomDetailSeatItemBinding seatLayout : seatLayouts) {
            if (seatLayout == null) {
                continue;
            }
            Object tag = seatLayout.getRoot().getTag();
            if (tag instanceof RoomManager.UserInfo && ((RoomManager.UserInfo) tag).userId.equals(userId)) {
                seatLayout.videoContainer.removeAllViews();
                seatLayout.ivCover.setImageDrawable(null);
                seatLayout.getRoot().setTag(null);
                break;
            }
        }
    }


    private ClubRoomDetailSeatItemBinding getIdleSeatLayout() {
        for (ClubRoomDetailSeatItemBinding seatLayout : seatLayouts) {
            if (seatLayout == null) {
                continue;
            }
            Object tag = seatLayout.getRoot().getTag();
            if (tag == null || "".equals(tag)) {
                return seatLayout;
            }
        }
        return null;
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId,
                list -> runOnUiThread(() -> {
                    if (isLocalRoomOwner()) {
                        boolean containOwner = false;
                        for (RoomManager.UserInfo userInfo : list) {
                            if (userInfo.userId.equals(roomInfo.userId) && userInfo.status == RoomManager.Status.ACCEPT) {
                                containOwner = true;
                                break;
                            }
                        }
                        if (!containOwner) {
                            roomManager.acceptUser(roomInfo.roomId, new RoomManager.UserInfo());
                        }
                    }
                    roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, new WeakReference<>(giftInfoDataCallback));
                    roomManager.subscribeUserChangeEvent(roomInfo.roomId, new WeakReference<>(userAddOrUpdateCallback), new WeakReference<>(userDeleteCallback));
                    for (RoomManager.UserInfo userInfo : list) {
                        updateUserView(userInfo);
                    }
                }),
                ex -> runOnUiThread(() -> {
                    Toast.makeText(RoomDetailActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }));
    }


    private void initRtcManager() {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);
        rtcManager.joinChannel(roomInfo.roomId, RoomManager.getCacheUserId(), getString(R.string.rtc_app_token), true, new RtcManager.OnChannelListener() {
            @Override
            public void onError(int code, String message) {

            }

            @Override
            public void onJoinSuccess(int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
            }

            @Override
            public void onUserJoined(String channelId, int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
            }

            @Override
            public void onUserOffline(String channelId, int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
            }
        });
        rtcManager.enableLocalAudio(false);
        rtcManager.enableLocalVideo(false);
        rtcManager.renderPlayerVideo(mBinding.portraitPlayerContainer);
        rtcManager.openPlayerVideo(roomInfo.videoUrl);
    }

    private void showGiftGridDialog() {
        new GiftGridDialog(this)
                .setOnGiftSendClickListener((dialog, item, position) -> {
                    dialog.dismiss();
                    RoomManager.GiftInfo giftInfo = new RoomManager.GiftInfo();
                    giftInfo.setIconNameById(item.icon_res);
                    giftInfo.setGifNameById(item.anim_res);
                    giftInfo.title = getString(item.name_res);
                    giftInfo.coin = item.coin_point;
                    giftInfo.userId = RoomManager.getCacheUserId();
                    roomManager.sendGift(roomInfo.roomId, giftInfo);
                })
                .show();
    }

    private void showTextInputDialog() {
        new TextInputDialog(this)
                .setOnSendClickListener((v, text) ->
                        mMessageAdapter.addMessage(new RoomManager.MessageInfo(RoomManager.getCacheUserId(), text)))
                .show();
    }

    private boolean isLocalRoomOwner() {
        return roomInfo.userId.equals(RoomManager.getCacheUserId());
    }

    private boolean isLocalInSeat(){
        String userId = RoomManager.getCacheUserId();
        for (ClubRoomDetailSeatItemBinding seatLayout : seatLayouts) {
            if (seatLayout == null) {
                continue;
            }
            Object tag = seatLayout.getRoot().getTag();
            if (tag instanceof RoomManager.UserInfo && ((RoomManager.UserInfo) tag).userId.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mBinding.portraitRootLayout.setVisibility(View.VISIBLE);
            mBinding.landscapeLayout.setVisibility(View.GONE);
            rtcManager.renderPlayerVideo(mBinding.portraitPlayerContainer);
        } else {
            mBinding.portraitRootLayout.setVisibility(View.GONE);
            mBinding.landscapeLayout.setVisibility(View.VISIBLE);
            rtcManager.renderPlayerVideo(mBinding.landscapePlayerContainer);
        }
    }

    @Override
    public void finish() {
        if (isLocalRoomOwner()) {
            roomManager.destroyRoom(roomInfo.roomId);
        } else {
            roomManager.leaveRoom(roomInfo.roomId);
        }
        rtcManager.release();
        super.finish();
    }
}
