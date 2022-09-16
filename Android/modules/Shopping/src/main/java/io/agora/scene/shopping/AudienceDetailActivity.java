package io.agora.scene.shopping;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

import io.agora.example.base.TokenGenerator;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcChannelEventHandler;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcChannel;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.VideoCanvas;
import io.agora.scene.shopping.databinding.ShoppingAudienceDetailActivityBinding;
import io.agora.scene.shopping.widget.ProductListDialog;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class AudienceDetailActivity extends AppCompatActivity {

    private RtcEngine rtcEngine;
    private final RoomManager roomManager = RoomManager.getInstance();

    private ShoppingAudienceDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;
    private final RoomManager.DataCallback<String> roomDeleteCallback = new RoomManager.DataCallback<String>() {
        @Override
        public void onObtained(String data) {
            if (data.equals(roomInfo.roomId)) {
                runOnUiThread(() -> showRoomEndDialog());
            }
        }
    };
    private final RoomManager.DataCallback<RoomManager.GiftInfo> giftInfoDataCallback = new RoomManager.DataCallback<RoomManager.GiftInfo>() {
        @Override
        public void onObtained(RoomManager.GiftInfo data) {
            runOnUiThread(() -> {
                mMessageAdapter.addMessage(new RoomManager.MessageInfo(
                        "User-" + data.userId,
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
    private final RoomManager.DataCallback<RoomManager.MessageInfo> messageInfoDataCallback = msg -> runOnUiThread(() -> {
        mMessageAdapter.addMessage(msg);
    });
    private final RoomManager.DataListCallback<RoomManager.UserInfo> userInfoDataListCallback = dataList -> runOnUiThread(() -> {
        mBinding.hostUserView.setUserCount(dataList.size());
        mBinding.hostUserView.removeAllUserIcon();
        for (int i = 1; i <= 3; i++) {
            int index = dataList.size() - i;
            if (index >= 0) {
                RoomManager.UserInfo userInfo = dataList.get(index);
                mBinding.hostUserView.addUserIcon(userInfo.getAvatarResId(), userInfo.userName);
            }
        }
    });
    private RtcChannel pkChannel;
    private final RoomManager.DataCallback<RoomManager.PKInfoModel> pkInfoModelDataCallback = data -> runOnUiThread(() -> {
        if (data == null) {
            return;
        }
        if (data.status == RoomManager.PKApplyInfoStatus.accept) {
            // 开始PK
            int pkUid = new Random(System.currentTimeMillis()).nextInt(10000) + 200000;
            String pkChannelId = data.roomId;

            pkChannel = rtcEngine.createRtcChannel(pkChannelId);
            pkChannel.setRtcChannelEventHandler(new IRtcChannelEventHandler() {
                @Override
                public void onUserJoined(RtcChannel rtcChannel, int uid, int elapsed) {
                    super.onUserJoined(rtcChannel, uid, elapsed);
                    runOnUiThread(() -> {
                        mBinding.pkVideoContainer.setVisibility(View.VISIBLE);
                        mBinding.ivPkIcon.setVisibility(View.VISIBLE);

                        SurfaceView videoView = RtcEngine.CreateRendererView(AudienceDetailActivity.this);
                        mBinding.pkVideoContainer.removeAllViews();
                        mBinding.pkVideoContainer.addView(videoView);
                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN,pkChannelId, uid));
                    });
                }

            });


            pkChannel.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            options.publishLocalAudio = false;
            options.publishLocalVideo = false;
            TokenGenerator.gen(AudienceDetailActivity.this, pkChannelId, pkUid, ret -> pkChannel.joinChannel(ret, "", pkUid, options));
        } else if (data.status == RoomManager.PKApplyInfoStatus.end) {
            // 结束PK
            mBinding.pkVideoContainer.setVisibility(View.GONE);
            mBinding.pkVideoContainer.removeAllViews();
            mBinding.ivPkIcon.setVisibility(View.GONE);
            if (pkChannel != null) {
                pkChannel.leaveChannel();
                pkChannel = null;
            }
        }
    });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ShoppingAudienceDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");
        mBinding.hostNameView.setIcon(roomInfo.getAndroidBgId());

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(true)
                .setFun1ImageResource(R.drawable.live_bottom_btn_gift)
                .setFun1ClickListener(v -> showGiftGridDialog());
        mBinding.bottomView.setFun2Visible(true)
                .setFun2ImageResource(R.drawable.shopping_btn_shopcart)
                .setFun2ClickListener(v -> {
                    ProductListDialog productListDialog = new ProductListDialog(roomInfo.roomId, true, AudienceDetailActivity.this);
                    productListDialog.setOnDetailVideoListener((contain, show) -> {
                        if (show) {
                            SurfaceView view = new SurfaceView(AudienceDetailActivity.this);
                            contain.removeAllViews();
                            contain.addView(view);
                            rtcEngine.setupRemoteVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.userId)));
                        } else {
                            contain.removeAllViews();
                            SurfaceView view = new SurfaceView(AudienceDetailActivity.this);
                            mBinding.localVideoContainer.removeAllViews();
                            mBinding.localVideoContainer.addView(view);
                            rtcEngine.setupRemoteVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.userId)));
                        }
                    });
                    productListDialog.show();
                });
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
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback);
            roomManager.subscribePKInfoEvent(roomInfo.roomId, pkInfoModelDataCallback);
            roomManager.subscriptRoomEvent(roomInfo.roomId, null, roomDeleteCallback);
            roomManager.subscribeMessageReceiveEvent(roomInfo.roomId, messageInfoDataCallback);
            roomManager.subscribeUserListChangeEvent(roomInfo.roomId, userInfoDataListCallback);
            roomManager.getRoomUserList(roomInfo.roomId, userInfoDataListCallback);
            roomManager.getPkInfo(roomInfo.roomId, pkInfoModelDataCallback);
        });
    }

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> {
                        SurfaceView videoView = RtcEngine.CreateRendererView(AudienceDetailActivity.this);
                        mBinding.localVideoContainer.removeAllViews();
                        mBinding.localVideoContainer.addView(videoView);
                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, uid));

                        mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid + "", getString(R.string.live_room_message_user_join_suffix)));
                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid + "", getString(R.string.live_room_message_user_left_suffix))));
                }
            });
            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + 100 + ","
                    + "\"serviceType\":" + 12 + ","
                    + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                    + "}"
                    + "}");
            rtcEngine.enableAudio();
            rtcEngine.enableVideo();
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);

            rtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeVideo = true;
            options.autoSubscribeAudio = true;
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(AudienceDetailActivity.this, roomInfo.roomId, uid, ret -> rtcEngine.joinChannel(ret, roomInfo.roomId, "", uid, options));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRoomEndDialog() {
        if (isDestroyed()) {
            return;
        }
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
                .setOnSendClickListener((v, text) -> {
                    RoomManager.MessageInfo message = new RoomManager.MessageInfo(roomManager.getLocalUserInfo().userName, text);
                    roomManager.sendMessage(roomInfo.roomId, message);
                })
                .show();
    }

    @Override
    public void finish() {
        roomManager.leaveRoom(roomInfo);
        if (pkChannel != null) {
            pkChannel.leaveChannel();
            pkChannel = null;
        }
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }
}
