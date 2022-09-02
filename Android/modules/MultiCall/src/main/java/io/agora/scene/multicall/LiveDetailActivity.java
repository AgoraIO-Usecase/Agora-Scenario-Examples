package io.agora.scene.multicall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.example.base.TokenGenerator;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.multicall.databinding.MultiCallLiveCallingLayoutBinding;
import io.agora.scene.multicall.databinding.MultiCallLiveDetailActivityBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class LiveDetailActivity extends AppCompatActivity {
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;

    private MultiCallLiveDetailActivityBinding mBinding;
    private MultiCallLiveCallingLayoutBinding[] mCallingLayouts;
    private RoomManager.RoomInfo roomInfo;
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
                new GiftAnimPlayDialog(LiveDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });
        }
    };
    private final RoomManager.DataCallback<RoomManager.MessageInfo> messageDataCallback = new RoomManager.DataCallback<RoomManager.MessageInfo>() {
        @Override
        public void onObtained(RoomManager.MessageInfo data) {
            runOnUiThread(() -> {
                mMessageAdapter.addMessage(data);
            });
        }
    };
    private final RoomManager.DataCallback<RoomManager.UserInfo> userChangeEvent = data -> {

        if (data.status == RoomManager.Status.INVITING) {
            if (data.userId.equals(RoomManager.getCacheUserId())) {
                return;
            }
            if (!localIsRoomOwner()) {
                return;
            }
            runOnUiThread(() -> {
                new AlertDialog.Builder(LiveDetailActivity.this)
                        .setTitle(R.string.common_tip)
                        .setMessage(R.string.multi_call_accept_apply_or_not)
                        .setPositiveButton(R.string.common_yes, (dialog, which) -> roomManager.acceptUser(roomInfo.roomId, data))
                        .setNegativeButton(R.string.common_no, (dialog, which) -> roomManager.refuseUser(roomInfo.roomId, data))
                        .show();
            });
        } else if (data.status == RoomManager.Status.ACCEPT) {
            runOnUiThread(() -> {
                upSeat(data);
                if (data.userId.equals(RoomManager.getCacheUserId()) && !localIsRoomOwner()) {
                    becomeBroadcast(true);
                }
            });
        } else if (data.status == RoomManager.Status.REFUSE || data.status == RoomManager.Status.END) {
            runOnUiThread(() -> {
                downSeat(data.userId);
                if (data.userId.equals(RoomManager.getCacheUserId()) && !localIsRoomOwner()) {
                    becomeBroadcast(false);
                }
            });
        }
    };
    private final RoomManager.DataCallback<String> userDelete = objectId -> {
        runOnUiThread(() -> downSeat(objectId));
    };
    private final RoomManager.DataCallback<String> roomDelete = objectId -> {
        runOnUiThread(() -> {
            new AlertDialog.Builder(LiveDetailActivity.this)
                    .setTitle(R.string.common_tip)
                    .setMessage(R.string.common_tip_room_closed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .show();
        });
    };
    private ChannelMediaOptions channelMediaOptions;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = MultiCallLiveDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        getWindow().getDecorView().setKeepScreenOn(true);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        initView();
        initRtcEngine();
        joinChannel();
        renderMainPreview();
        initRoomManager();
    }

    private void initView() {
        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");
        mBinding.hostUserView.setUserCount(1);
        mBinding.hostUserView.addUserIcon(R.drawable.user_profile_image_1, 0);

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(!localIsRoomOwner())
                .setFun1ImageResource(R.drawable.live_bottom_btn_gift)
                .setFun1ClickListener(v -> showGiftGridDialog());
        mBinding.bottomView.setFun2Visible(false);
        mBinding.bottomView.setupInputText(true, v -> {
            // 弹出文本输入框
            showTextInputDialog();
        });
        mBinding.bottomView.setupCloseBtn(true, v -> onBackPressed());
        mBinding.bottomView.setupMoreBtn(localIsRoomOwner(), v -> showSettingDialog());

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo>() {
            @Override
            protected void onItemUpdate(LiveRoomMessageListView.MessageListViewHolder holder, RoomManager.MessageInfo item, int position) {
                holder.setupMessage(item.userName, item.content, item.giftIcon);
            }
        };
        mBinding.messageList.setAdapter(mMessageAdapter);

        mCallingLayouts = new MultiCallLiveCallingLayoutBinding[]{
                mBinding.videoPlaceCalling01,
                mBinding.videoPlaceCalling02,
                mBinding.videoPlaceCalling03,
                mBinding.videoPlaceCalling04
        };
        for (MultiCallLiveCallingLayoutBinding callingLayout : mCallingLayouts) {
            callingLayout.getRoot().setOnClickListener(v -> {
                if (localIsRoomOwner()) {
                    Object tag = callingLayout.getRoot().getTag();
                    if (tag instanceof RoomManager.UserInfo) {
                        // 封麦
                        new AlertDialog.Builder(LiveDetailActivity.this)
                                .setTitle(R.string.common_tip)
                                .setMessage(R.string.multi_call_end_linking)
                                .setPositiveButton(R.string.common_confirm, (dialog, which) -> roomManager.endUser(roomInfo.roomId, (RoomManager.UserInfo) tag))
                                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                } else {
                    if (roomManager.getLocalUserInfo().status != RoomManager.Status.ACCEPT) {
                        // 发起上座申请
                        new AlertDialog.Builder(LiveDetailActivity.this)
                                .setTitle(R.string.common_tip)
                                .setMessage(R.string.multi_call_apply_linking)
                                .setPositiveButton(R.string.common_confirm, (dialog, which) -> roomManager.inviteUser(roomInfo.roomId, roomManager.getLocalUserInfo()))
                                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        Object tag = callingLayout.getRoot().getTag();
                        if (tag instanceof RoomManager.UserInfo) {
                            if (((RoomManager.UserInfo) tag).userId.equals(RoomManager.getCacheUserId())) {
                                // 下麦
                                new AlertDialog.Builder(LiveDetailActivity.this)
                                        .setTitle(R.string.common_tip)
                                        .setMessage(R.string.multi_call_close_linking)
                                        .setPositiveButton(R.string.common_confirm, (dialog, which) -> roomManager.endUser(roomInfo.roomId, roomManager.getLocalUserInfo()))
                                        .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                                        .show();
                            }
                        }
                    }
                }

            });
        }
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
                    RoomManager.MessageInfo item = new RoomManager.MessageInfo(roomManager.getLocalUserInfo().userName, text);
                    roomManager.sendMessage(roomInfo.roomId, item);
                })
                .show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId,
                (data) -> {
                    roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback);
                    roomManager.subscribeMessageReceiveEvent(roomInfo.roomId, messageDataCallback);
                    roomManager.subscribeUserChangeEvent(roomInfo.roomId, userChangeEvent, userDelete);
                    roomManager.subscribeRoomDeleteEvent(roomInfo.roomId, roomDelete);
                    roomManager.getUserList(roomInfo.roomId, dataList -> {
                        for (RoomManager.UserInfo userInfo : dataList) {
                            if (userInfo.status == RoomManager.Status.ACCEPT) {
                                runOnUiThread(() -> upSeat(userInfo));
                            }
                        }
                    });
                }, data -> {
                    Toast.makeText(LiveDetailActivity.this, "Join Room error", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {

                @Override
                public void onError(int err) {
                    super.onError(err);
                    runOnUiThread(() -> {
                        Toast.makeText(LiveDetailActivity.this, "code=" + err + ",message=" + RtcEngine.getErrorDescription(err), Toast.LENGTH_LONG).show();
                        finish();
                    });
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid, getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid, getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid, getString(R.string.live_room_message_user_left_suffix))));
                }

            });
            rtcEngine.enableVideo();
            rtcEngine.enableVideo();
            rtcEngine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(Constants.cameraDirection));
            rtcEngine.setVideoEncoderConfiguration(Constants.encoderConfiguration);
            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + 100 + ","
                    + "\"serviceType\":" + 12 + ","
                    + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                    + "}"
                    + "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinChannel() {
        channelMediaOptions = new ChannelMediaOptions();
        channelMediaOptions.clientRoleType =
                localIsRoomOwner() ? io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER : io.agora.rtc2.Constants.CLIENT_ROLE_AUDIENCE;
        channelMediaOptions.publishCameraTrack = localIsRoomOwner();
        channelMediaOptions.publishMicrophoneTrack = localIsRoomOwner();
        channelMediaOptions.autoSubscribeVideo = true;
        channelMediaOptions.autoSubscribeAudio = true;
        int uid = Integer.parseInt(RoomManager.getCacheUserId());
        TokenGenerator.gen(this, roomInfo.roomId, uid, ret -> rtcEngine.joinChannel(ret, roomInfo.roomId, uid, channelMediaOptions));
    }

    private void becomeBroadcast(boolean publish) {
        channelMediaOptions.clientRoleType =
                publish ? io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER : io.agora.rtc2.Constants.CLIENT_ROLE_AUDIENCE;
        channelMediaOptions.publishCameraTrack = publish;
        channelMediaOptions.publishMicrophoneTrack = publish;
        rtcEngine.updateChannelMediaOptions(channelMediaOptions);
    }

    private void renderMainPreview() {
        SurfaceView videoView = new SurfaceView(this);
        mBinding.videoPlaceCallingLocal.removeAllViews();
        mBinding.videoPlaceCallingLocal.addView(videoView);

        if (localIsRoomOwner()) {
            rtcEngine.setupLocalVideo(new VideoCanvas(videoView, io.agora.rtc2.Constants.RENDER_MODE_HIDDEN));
            rtcEngine.startPreview();
        } else {
            rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, io.agora.rtc2.Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.userId)));
        }
    }

    private void showSettingDialog() {
        new LiveToolsDialog(LiveDetailActivity.this)
                .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> {
                    if (rtcEngine != null) {
                        rtcEngine.switchCamera();
                    }
                })
                .addToolItem(LiveToolsDialog.TOOL_ITEM_VIDEO, channelMediaOptions.publishCameraTrack, (view, item) -> {
                    channelMediaOptions.publishCameraTrack = item.activated;
                    rtcEngine.updateChannelMediaOptions(channelMediaOptions);
                    rtcEngine.enableLocalVideo(item.activated);
                })
                .addToolItem(LiveToolsDialog.TOOL_ITEM_SPEAKER, channelMediaOptions.publishMicrophoneTrack, (view, item) -> {
                    channelMediaOptions.publishMicrophoneTrack = item.activated;
                    rtcEngine.updateChannelMediaOptions(channelMediaOptions);
                    rtcEngine.enableLocalAudio(item.activated);
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        if (localIsRoomOwner()) {
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
    public void finish() {
        roomManager.endUser(roomInfo.roomId, roomManager.getLocalUserInfo());
        if (localIsRoomOwner()) {
            roomManager.destroyRoom(roomInfo.roomId);
        } else {
            roomManager.leaveRoom(roomInfo.roomId);
        }
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }

    private MultiCallLiveCallingLayoutBinding getIdleSeat() {
        for (MultiCallLiveCallingLayoutBinding callingLayoutBinding : mCallingLayouts) {
            if (callingLayoutBinding.getRoot().getTag() == null) {
                return callingLayoutBinding;
            }
        }
        return null;
    }

    private MultiCallLiveCallingLayoutBinding getSeatByUserId(String userId) {
        for (MultiCallLiveCallingLayoutBinding callingLayoutBinding : mCallingLayouts) {
            Object tag = callingLayoutBinding.getRoot().getTag();
            if (tag instanceof RoomManager.UserInfo) {
                if (((RoomManager.UserInfo) tag).userId.equals(userId) || ((RoomManager.UserInfo) tag).objectId.equals(userId)) {
                    return callingLayoutBinding;
                }
            }
        }
        return null;
    }

    private void upSeat(RoomManager.UserInfo userInfo) {
        MultiCallLiveCallingLayoutBinding seat = getSeatByUserId(userInfo.userId);
        if (seat == null) {
            seat = getIdleSeat();
            if (seat == null) {
                return;
            }
        }
        seat.getRoot().setTag(userInfo);

        if (userInfo.userId.equals(RoomManager.getCacheUserId())) {
            SurfaceView renderView = new SurfaceView(this);
            seat.videoContainer.removeAllViews();
            seat.videoContainer.addView(renderView);
            rtcEngine.setupLocalVideo(new VideoCanvas(renderView,
                    io.agora.rtc2.Constants.RENDER_MODE_HIDDEN));
            rtcEngine.startPreview();

        } else {
            SurfaceView renderView = new SurfaceView(this);
            seat.videoContainer.removeAllViews();
            seat.videoContainer.addView(renderView);
            rtcEngine.setupRemoteVideo(new VideoCanvas(renderView,
                    io.agora.rtc2.Constants.RENDER_MODE_HIDDEN, Integer.parseInt(userInfo.userId)));
        }
    }

    private void downSeat(String userId) {
        MultiCallLiveCallingLayoutBinding seat = getSeatByUserId(userId);
        if (seat == null) {
            return;
        }
        seat.videoContainer.removeAllViews();
        seat.getRoot().setTag(null);
        if (userId.equals(RoomManager.getCacheUserId())) {
            rtcEngine.stopPreview();
        }
    }

    private boolean localIsRoomOwner() {
        return roomInfo.userId.equals(RoomManager.getCacheUserId());
    }

}
