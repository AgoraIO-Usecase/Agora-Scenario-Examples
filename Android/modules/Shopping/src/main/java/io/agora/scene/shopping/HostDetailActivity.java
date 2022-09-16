package io.agora.scene.shopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Iterator;
import java.util.List;

import io.agora.example.base.TokenGenerator;
import io.agora.rtc.IRtcChannelEventHandler;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcChannel;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.CameraCapturerConfiguration;
import io.agora.rtc.video.VideoCanvas;
import io.agora.scene.shopping.databinding.ShoppingHostDetailActivityBinding;
import io.agora.scene.shopping.widget.ProductListDialog;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.OnlineUserListDialog;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class HostDetailActivity extends AppCompatActivity {
    private RtcEngine rtcEngine;
    private final RoomManager roomManager = RoomManager.getInstance();

    private ShoppingHostDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private RoomManager.PKApplyInfoModel pkApplyInfoModel;
    private RoomManager.PKApplyInfoModel exPkApplyInfoModel;
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;
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
                new GiftAnimPlayDialog(HostDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });
        }
    };
    private final RoomManager.DataCallback<RoomManager.PKApplyInfoModel> exPkApplyInfoModelDataCallback = data -> runOnUiThread(() -> {
        exPkApplyInfoModel = data;
        if (data.status == RoomManager.PKApplyInfoStatus.accept) {
            roomManager.startPKNow(roomInfo.roomId, data);
            resetExChannel(data, true);
        } else if (data.status == RoomManager.PKApplyInfoStatus.end) {
            roomManager.stopPKNow(roomInfo.roomId, data);
            resetExChannel(data, false);
        }
    });
    private final RoomManager.DataCallback<RoomManager.PKApplyInfoModel> pkApplyInfoModelDataCallback = data -> runOnUiThread(() -> {
        pkApplyInfoModel = data;
        if (data.status == RoomManager.PKApplyInfoStatus.invite) {
            new AlertDialog.Builder(this)
                    .setMessage("收到邀请，是否接受？")
                    .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                        roomManager.applyPKAccept(data);
                    })
                    .setNegativeButton(R.string.common_no, (dialog, which) -> roomManager.applyPKRefuse(data))
                    .show();
        }else if (data.status == RoomManager.PKApplyInfoStatus.accept) {
            roomManager.startPKNow(roomInfo.roomId, data);
            resetExChannel(data, true);
        } else if (data.status == RoomManager.PKApplyInfoStatus.end) {
            roomManager.stopPKNow(roomInfo.roomId, data);
            resetExChannel(data, false);
        }
    });
    private final RoomManager.DataCallback<RoomManager.MessageInfo> messageInfoDataCallback = msg -> runOnUiThread(()->{
        mMessageAdapter.addMessage(msg);
    });
    private final RoomManager.DataListCallback<RoomManager.UserInfo> userInfoDataListCallback = dataList -> runOnUiThread(()->{
        mBinding.hostUserView.setUserCount(dataList.size());
        mBinding.hostUserView.removeAllUserIcon();
        for (int i = 1; i <= 3; i++) {
            int index = dataList.size() - i;
            if(index >= 0){
                RoomManager.UserInfo userInfo = dataList.get(index);
                mBinding.hostUserView.addUserIcon(userInfo.getAvatarResId(), userInfo.userName);
            }
        }
    });
    private RtcChannel exChannel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ShoppingHostDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(true)
                .setFun1ImageResource(R.drawable.shopping_btn_shopcart)
                .setFun1ClickListener(v -> new ProductListDialog(roomInfo.roomId, false, HostDetailActivity.this)
                        .show());
        mBinding.bottomView.setFun2Visible(true)
                .setFun2ImageResource(R.drawable.live_bottom_btn_pk)
                .setFun2ClickListener(v -> {
                    showHostListDialog();
                });
        mBinding.bottomView.setupInputText(true, v -> {
            // 弹出文本输入框
            showTextInputDialog();
        });
        mBinding.bottomView.setupCloseBtn(true, v -> onBackPressed());
        mBinding.bottomView.setupMoreBtn(true, v -> showSettingDialog());

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo>() {
            @Override
            protected void onItemUpdate(LiveRoomMessageListView.MessageListViewHolder holder, RoomManager.MessageInfo item, int position) {
                holder.setupMessage(item.userName, item.content, item.giftIcon);
            }
        };
        mBinding.messageList.setAdapter(mMessageAdapter);

        mBinding.btnStopPk.setOnClickListener(v -> {
            endPK();
        });

        initRoomManager();
        initRtcEngine();
    }

    private void endPK() {
        if(pkApplyInfoModel != null){
            roomManager.applyPKEnd(pkApplyInfoModel);
            pkApplyInfoModel = null;
        }
        if(exPkApplyInfoModel != null){
            roomManager.applyPKEnd(exPkApplyInfoModel);
            exPkApplyInfoModel = null;
        }
    }

    private void showHostListDialog() {
        roomManager.getAllRooms(new RoomManager.DataListCallback<RoomManager.RoomInfo>() {
            @Override
            public void onObtained(List<RoomManager.RoomInfo> dataList) {
                if (dataList == null) {
                    return;
                }
                Iterator<RoomManager.RoomInfo> iterator = dataList.iterator();
                while (iterator.hasNext()) {
                    RoomManager.RoomInfo next = iterator.next();
                    if (next.userId.equals(RoomManager.getCacheUserId())) {
                        iterator.remove();
                    }
                }
                if (dataList.size() == 0) {
                    return;
                }
                runOnUiThread(() -> {
                    OnlineUserListDialog dialog = new OnlineUserListDialog(HostDetailActivity.this);
                    OnlineUserListDialog.AbsListItemAdapter<RoomManager.RoomInfo> adapter = new OnlineUserListDialog.AbsListItemAdapter<RoomManager.RoomInfo>() {
                        @Override
                        protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position, RoomManager.RoomInfo item) {
                            holder.binding.tvName.setText("User-" + item.userId);
                            holder.binding.ivIcon.setImageResource(RandomUtil.getIconById(item.roomId));
                            holder.binding.tvStatus.setText(R.string.online_user_list_dialog_invite);
                            holder.binding.tvStatus.setEnabled(true);
                            holder.binding.tvStatus.setActivated(true);
                            holder.binding.tvStatus.setOnClickListener(v -> {
                                roomManager.inviteToPK(roomInfo.roomId, item.roomId, item.userId, data -> {
                                    roomManager.applyPKInvite(data);
                                    roomManager.subscribePKApplyInfoEvent(data.targetRoomId, exPkApplyInfoModelDataCallback);
                                    dialog.dismiss();
                                }, data -> runOnUiThread(() -> Toast.makeText(HostDetailActivity.this, data.getMessage(), Toast.LENGTH_LONG).show()));

                            });
                        }
                    };
                    adapter.resetAll(dataList);
                    dialog.setListAdapter(adapter);
                    dialog.show();
                });
            }

        });

    }

    private void showTextInputDialog() {
        new TextInputDialog(this)
                .setOnSendClickListener((v, text) -> {
                    RoomManager.MessageInfo message = new RoomManager.MessageInfo(roomManager.getLocalUserInfo().userName, text);
                    roomManager.sendMessage(roomInfo.roomId, message);
                })
                .show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback);
            roomManager.subscribePKApplyInfoEvent(roomInfo.roomId, pkApplyInfoModelDataCallback);
            roomManager.subscribeMessageReceiveEvent(roomInfo.roomId, messageInfoDataCallback);
            roomManager.subscribeUserListChangeEvent(roomInfo.roomId, userInfoDataListCallback);
            roomManager.getRoomUserList(roomInfo.roomId, userInfoDataListCallback);
        });
    }

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onError(int err) {
                    super.onError(err);
                    runOnUiThread(() -> {
                        Toast.makeText(HostDetailActivity.this, "code=" + err + ",message=" + RtcEngine.getErrorDescription(err), Toast.LENGTH_LONG).show();
                        finish();
                    });
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid + "", getString(R.string.live_room_message_user_join_suffix))));
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
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();
            rtcEngine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            rtcEngine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(CameraCapturerConfiguration.CAPTURER_OUTPUT_PREFERENCE.CAPTURER_OUTPUT_PREFERENCE_AUTO, Constants.cameraDirection));
            rtcEngine.setVideoEncoderConfiguration(Constants.encoderConfiguration);

            SurfaceView videoView = RtcEngine.CreateRendererView(this);
            mBinding.localVideoContainer.removeAllViews();
            mBinding.localVideoContainer.addView(videoView);
            rtcEngine.setupLocalVideo(new VideoCanvas(videoView, io.agora.rtc.Constants.RENDER_MODE_HIDDEN, 0));

            rtcEngine.setClientRole(io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishLocalAudio = true;
            options.publishLocalVideo = true;
            options.autoSubscribeVideo = true;
            options.autoSubscribeAudio = true;
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(HostDetailActivity.this, roomInfo.roomId, uid, ret -> rtcEngine.joinChannel(ret, roomInfo.roomId, null, uid, options));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetExChannel(RoomManager.PKApplyInfoModel data, boolean join) {
        String exChannelId = roomInfo.roomId.equals(data.roomId) ? data.targetRoomId : data.roomId;
        if(join){

            exChannel = rtcEngine.createRtcChannel(exChannelId);
            exChannel.setClientRole(io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE);
            exChannel.setRtcChannelEventHandler(new IRtcChannelEventHandler() {
                @Override
                public void onUserJoined(RtcChannel rtcChannel, int uid, int elapsed) {
                    super.onUserJoined(rtcChannel, uid, elapsed);
                    runOnUiThread(() -> {
                        mBinding.pkVideoContainer.setVisibility(View.VISIBLE);
                        mBinding.ivPkIcon.setVisibility(View.VISIBLE);
                        mBinding.btnStopPk.setVisibility(View.VISIBLE);
                        SurfaceView videoView = RtcEngine.CreateRendererView(HostDetailActivity.this);
                        mBinding.pkVideoContainer.removeAllViews();
                        mBinding.pkVideoContainer.addView(videoView);

                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, io.agora.rtc.Constants.RENDER_MODE_HIDDEN, exChannelId, uid));
                    });
                }

                @Override
                public void onUserOffline(RtcChannel rtcChannel, int uid, int reason) {
                    super.onUserOffline(rtcChannel, uid, reason);
                    runOnUiThread(() -> {
                        mBinding.pkVideoContainer.setVisibility(View.GONE);
                        mBinding.pkVideoContainer.removeAllViews();
                        mBinding.ivPkIcon.setVisibility(View.GONE);
                        mBinding.btnStopPk.setVisibility(View.GONE);
                        endPK();
                    });
                }

                @Override
                public void onLeaveChannel(RtcChannel rtcChannel, IRtcEngineEventHandler.RtcStats stats) {
                    super.onLeaveChannel(rtcChannel, stats);
                    runOnUiThread(() -> {
                        mBinding.pkVideoContainer.setVisibility(View.GONE);
                        mBinding.pkVideoContainer.removeAllViews();
                        mBinding.ivPkIcon.setVisibility(View.GONE);
                        mBinding.btnStopPk.setVisibility(View.GONE);
                        endPK();
                    });
                }
            });
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishLocalVideo = false;
            options.publishLocalAudio = false;
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            TokenGenerator.gen(HostDetailActivity.this, exChannelId, 0, ret -> exChannel.joinChannel(ret, "", 0, options));

        }else{
            mBinding.pkVideoContainer.setVisibility(View.GONE);
            mBinding.pkVideoContainer.removeAllViews();
            mBinding.ivPkIcon.setVisibility(View.GONE);
            mBinding.btnStopPk.setVisibility(View.GONE);
            roomManager.unSubscribePKApplyInfoEvent(data.targetRoomId, exPkApplyInfoModelDataCallback);
            if(exChannel != null){
                exChannel.leaveChannel();
                exChannel = null;
            }
        }

    }

    private void showSettingDialog() {
        new LiveToolsDialog(HostDetailActivity.this)
                .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> {
                    if(rtcEngine != null){
                        rtcEngine.switchCamera();
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_close_room)
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.dismiss();
                    HostDetailActivity.super.onBackPressed();
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void finish() {
        roomManager.leaveRoom(roomInfo);
        if(exChannel != null){
            exChannel.leaveChannel();
            exChannel = null;
        }
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }

}
