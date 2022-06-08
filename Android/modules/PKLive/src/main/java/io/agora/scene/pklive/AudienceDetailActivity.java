package io.agora.scene.pklive;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

import io.agora.scene.pklive.databinding.PkLiveAudienceDetailActivityBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;

public class AudienceDetailActivity extends AppCompatActivity {

    private final RtcManager rtcManager = new RtcManager();
    private final RoomManager roomManager = RoomManager.getInstance();

    private PkLiveAudienceDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;
    private final RoomManager.DataCallback<String> roomDeleteCallback = new RoomManager.DataCallback<String>() {
        @Override
        public void onObtained(String data) {
            if(data.equals(roomInfo.roomId)){
                runOnUiThread(() -> showRoomEndDialog());
            }
        }
    };
    private final RoomManager.DataCallback<RoomManager.GiftInfo> giftInfoDataCallback = new RoomManager.DataCallback<RoomManager.GiftInfo>() {
        @Override
        public void onObtained(RoomManager.GiftInfo data) {
            runOnUiThread(() -> {
                mMessageAdapter.addMessage(new RoomManager.MessageInfo(
                        data.userId,
                        getString(R.string.live_room_message_gift_prefix),
                        data.getIconId()
                ));
                // 播放动画
                new GiftAnimPlayDialog(AudienceDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });
        }
    };
    private final RoomManager.DataCallback<RoomManager.PKInfoModel> pkInfoModelDataCallback = data -> runOnUiThread(() -> {
        if(data == null){
            return;
        }
        if (data.status == RoomManager.PKApplyInfoStatus.accept) {
            // 开始PK
            rtcManager.joinChannel(data.roomId, "", getString(R.string.rtc_app_token), false, new RtcManager.OnChannelListener() {
                @Override
                public void onError(int code, String message) {

                }

                @Override
                public void onJoinSuccess(int uid) {

                }

                @Override
                public void onUserJoined(String channelId, int uid) {
                    runOnUiThread(() -> {
                        mBinding.pkVideoContainer.setVisibility(View.VISIBLE);
                        mBinding.ivPkIcon.setVisibility(View.VISIBLE);
                        rtcManager.renderRemoteVideo(mBinding.pkVideoContainer, channelId, uid);
                    });
                }

                @Override
                public void onUserOffline(String channelId, int uid) {

                }
            });
        } else if (data.status == RoomManager.PKApplyInfoStatus.end) {
            // 结束PK
            rtcManager.leaveChannel(data.roomId);
            mBinding.pkVideoContainer.setVisibility(View.GONE);
            mBinding.pkVideoContainer.removeAllViews();
            mBinding.ivPkIcon.setVisibility(View.GONE);
        }
    });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = PkLiveAudienceDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        // 房间信息
        mBinding.hostNameView.setName(RandomUtil.randomUserName(this));
        mBinding.hostNameView.setIcon(roomInfo.getAndroidBgId());

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(true)
                .setFun1ImageResource(R.drawable.live_bottom_btn_gift)
                .setFun1ClickListener(v -> showGiftGridDialog());
        mBinding.bottomView.setFun2Visible(false);
        mBinding.bottomView.setupInputText(true, v -> showTextInputDialog());
        mBinding.bottomView.setupCloseBtn(true, v -> finish());
        mBinding.bottomView.setupMoreBtn(false, null);

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo>() {
            @Override
            protected void onItemUpdate(LiveRoomMessageListView.MessageListViewHolder holder, RoomManager.MessageInfo item, int position) {
                holder.setupMessage(item.userName, item.content, item.giftIcon);
            }
        };
        mBinding.messageList.setAdapter(mMessageAdapter);

        initRtcManager();
        initRoomManager();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, new WeakReference<>(giftInfoDataCallback));
            roomManager.subscribePKInfoEvent(roomInfo.roomId, new WeakReference<>(pkInfoModelDataCallback));
            roomManager.subscriptRoomEvent(roomInfo.roomId, null, new WeakReference<>(roomDeleteCallback));
            roomManager.getPkInfo(roomInfo.roomId, pkInfoModelDataCallback);
        });
    }

    private void initRtcManager() {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);
        rtcManager.joinChannel(roomInfo.roomId, RoomManager.getCacheUserId(), getString(R.string.rtc_app_token), false, new RtcManager.OnChannelListener() {
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
                    rtcManager.renderRemoteVideo(mBinding.localVideoContainer, channelId, uid);
                    mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix)));
                });
            }

            @Override
            public void onUserOffline(String channelId, int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
            }
        });
    }

    private void showRoomEndDialog(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_room_closed)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
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
                .setOnSendClickListener((v, text) -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(RoomManager.getCacheUserId(), text)))
                .show();
    }

    @Override
    public void finish() {
        rtcManager.release();
        super.finish();
    }
}
