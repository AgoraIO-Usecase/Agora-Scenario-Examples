package io.agora.scene.singlehostlive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import io.agora.example.base.TokenGenerator;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.CameraCapturerConfiguration;
import io.agora.rtc.video.VideoCanvas;
import io.agora.scene.singlehostlive.databinding.SingleHostLiveHostDetailActivityBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class HostDetailActivity extends AppCompatActivity {
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;

    private SingleHostLiveHostDetailActivityBinding mBinding;
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
                new GiftAnimPlayDialog(HostDetailActivity.this)
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

    private final RoomManager.DataListCallback<RoomManager.UserInfo> userListChange = new RoomManager.DataListCallback<RoomManager.UserInfo>() {
        @Override
        public void onObtained(List<RoomManager.UserInfo> dataList) {
            runOnUiThread(() -> {
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
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = SingleHostLiveHostDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(false);
        mBinding.bottomView.setFun2Visible(false);
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

        initRoomManager();
        initRtcEngine();

        renderLocalPreview();
        joinChannel();
    }

    private void showTextInputDialog() {
        new TextInputDialog(this)
                .setOnSendClickListener((v, text) -> {
                    RoomManager.MessageInfo item = new RoomManager.MessageInfo(roomManager.getLocalUserInfo().userName, text);
                    roomManager.sendMessage(roomInfo.roomId, item);
                })
                .show();
    }

    private void initRoomManager(){
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback);
            roomManager.subscribeMessageReceiveEvent(roomInfo.roomId, messageDataCallback);
            roomManager.subscribeUserChangeEvent(roomInfo.roomId, userListChange);
            roomManager.getRoomUserList(roomInfo.roomId, userListChange);
        });
    }

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this,  getString(R.string.rtc_app_id), new IRtcEngineEventHandler(){

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
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" +  uid, getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" + uid, getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo("User-" +  uid, getString(R.string.live_room_message_user_left_suffix))));
                }
            });

            rtcEngine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            rtcEngine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(CameraCapturerConfiguration.CAPTURER_OUTPUT_PREFERENCE.CAPTURER_OUTPUT_PREFERENCE_AUTO, Constants.cameraDirection));
            rtcEngine.setVideoEncoderConfiguration(Constants.encoderConfiguration);
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinChannel() {
        rtcEngine.setClientRole(io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER);
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishLocalAudio = true;
        options.publishLocalVideo = true;
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;
        int uid = Integer.parseInt(RoomManager.getCacheUserId());
        TokenGenerator.gen(this, roomInfo.roomId, uid, ret -> {
            rtcEngine.joinChannel(ret, roomInfo.roomId, "", uid, options);
        });
    }

    private void renderLocalPreview() {
        SurfaceView videoView = RtcEngine.CreateRendererView(this);
        mBinding.fullVideoContainer.removeAllViews();
        mBinding.fullVideoContainer.addView(videoView);
        rtcEngine.setupLocalVideo(new VideoCanvas(videoView, io.agora.rtc.Constants.RENDER_MODE_HIDDEN, 0));
        rtcEngine.startPreview();
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
        roomManager.leaveRoom(roomInfo.roomId, true);
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }

}
