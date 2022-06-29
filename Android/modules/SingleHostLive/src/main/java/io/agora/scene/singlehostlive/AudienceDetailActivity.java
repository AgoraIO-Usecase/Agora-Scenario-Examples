package io.agora.scene.singlehostlive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.singlehostlive.databinding.SingleHostLiveAudienceDetailActivityBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class AudienceDetailActivity extends AppCompatActivity {

    private RtcEngine rtcEngine;
    private final RoomManager roomManager = RoomManager.getInstance();

    private SingleHostLiveAudienceDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;
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
                new GiftAnimPlayDialog(AudienceDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });

        }

        @Override
        public void onFailed(Exception e) {

        }
    };

    private final RoomManager.DataCallback<String> roomDeleteCallback = new RoomManager.DataCallback<String>() {
        @Override
        public void onSuccess(String data) {
            runOnUiThread(() -> {
                new AlertDialog.Builder(AudienceDetailActivity.this)
                        .setTitle(R.string.common_tip)
                        .setMessage(R.string.common_tip_room_closed)
                        .setPositiveButton(R.string.common_confirm, (dialog, which) -> finish())
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
        mBinding = SingleHostLiveAudienceDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
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

        initRtcEngine();
        initRoomManager();

        joinChannel();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback);
            roomManager.subscribeRoomDeleteEvent(roomInfo.roomId, roomDeleteCallback);
        });
    }

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> {
                        renderRemoteVideo(uid);
                        mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix)));
                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderRemoteVideo(int uid) {
        SurfaceView videoView = new SurfaceView(this);
        mBinding.fullVideoContainer.removeAllViews();
        mBinding.fullVideoContainer.addView(videoView);
        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, uid));
    }

    private void joinChannel(){
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;
        rtcEngine.joinChannel(getString(R.string.rtc_app_token), roomInfo.roomId, 0, options);
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
        roomManager.leaveRoom(roomInfo.roomId, false);
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }
}
