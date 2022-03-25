package io.agora.sample.club;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.agora.example.base.BaseActivity;
import io.agora.sample.club.databinding.ClubRoomDetailActivityBinding;
import io.agora.sample.club.databinding.ClubRoomDetailMsgItemBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.UIUtil;

public class RoomDetailActivity extends BaseActivity<ClubRoomDetailActivityBinding> {

    private final RtcManager rtcManager = RtcManager.getInstance();
    private final RoomManager roomManager = RoomManager.getInstance();

    private final FrameLayout[] seatLayouts = new FrameLayout[8];

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

        @Override
        public void onFailed(Exception e) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        mBinding.titleBar
                .setBgDrawable(R.drawable.club_main_title_bar_bg)
                .setDeliverVisible(false)
                .setTitleName(roomInfo.roomName, getResources().getColor(R.color.club_title_bar_text_color))
                .setBackIcon(true, R.drawable.club_ic_arrow_24, v -> finish());

        // 座位列表
        seatLayouts[0] = UIUtil.setViewCircle(mBinding.seat01.videoContainer);
        seatLayouts[1] = UIUtil.setViewCircle(mBinding.seat02.videoContainer);
        seatLayouts[2] = UIUtil.setViewCircle(mBinding.seat03.videoContainer);
        seatLayouts[3] = UIUtil.setViewCircle(mBinding.seat04.videoContainer);
        seatLayouts[4] = UIUtil.setViewCircle(mBinding.seat05.videoContainer);
        seatLayouts[5] = UIUtil.setViewCircle(mBinding.seat06.videoContainer);
        seatLayouts[6] = UIUtil.setViewCircle(mBinding.seat07.videoContainer);
        seatLayouts[7] = UIUtil.setViewCircle(mBinding.seat08.videoContainer);

        // 底部按钮栏
        mBinding.bottomView
                .setupCloseBtn(false, null)
                .setupMoreBtn(false, null)
                // 文本输入
                .setupInputText(true, v -> showTextInputDialog())
                // 摄像头开关
                .setFun1Visible(true)
                .setFun1ImageResource(R.drawable.club_room_detail_ic_cam)
                .setFun1Activated(true)
                .setFun1ClickListener(v -> {
                    mBinding.bottomView.setFun1Activated(!mBinding.bottomView.isFun1Activated());
                })
                // 麦克风开关
                .setFun2Visible(true)
                .setFun2ImageResource(R.drawable.club_room_detail_ic_mic)
                .setFun2Activated(true)
                .setFun2ClickListener(v -> {
                    mBinding.bottomView.setFun2Activated(!mBinding.bottomView.isFun2Activated());
                })
                // 礼物
                .setFun3Visible(true)
                .setFun3ImageResource(R.drawable.club_room_detail_gift)
                .setFun3ClickListener(v -> showGiftGridDialog());

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.AbsMessageAdapter<RoomManager.MessageInfo, ClubRoomDetailMsgItemBinding>() {

            @Override
            protected void onItemUpdate(BindingViewHolder<ClubRoomDetailMsgItemBinding> holder, RoomManager.MessageInfo item, int position) {
                holder.binding.ivUserAvatar.setImageResource(RandomUtil.randomLiveRoomIcon());
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

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, new WeakReference<>(giftInfoDataCallback)));
    }

    private void initRtcManager() {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);
        rtcManager.renderLocalVideo(seatLayouts[0], null);
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
                runOnUiThread(() -> {
                    // rtcManager.renderRemoteVideo(mBinding.fullVideoContainer, uid);
                    mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix)));
                });
            }

            @Override
            public void onUserOffline(String channelId, int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
            }
        });
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

    @Override
    public void finish() {
        rtcManager.release();
        super.finish();
    }
}
