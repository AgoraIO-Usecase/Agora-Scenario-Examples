package io.agora.scene.singlehostlive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
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
        public void onSuccess(RoomManager.GiftInfo data) {
            runOnUiThread(() -> {
                mMessageAdapter.addMessage(new RoomManager.MessageInfo(
                        data.userId,
                        getString(R.string.live_room_message_gift_prefix),
                        data.getIconId()
                ));
                // 播放动画
                new GiftAnimPlayDialog(HostDetailActivity.this)
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
        mBinding = SingleHostLiveHostDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName);
        mBinding.hostNameView.setIcon(roomInfo.getAndroidBgId());

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
                .setOnSendClickListener((v, text) -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(RoomManager.getCacheUserId(), text)))
                .show();
    }

    private void initRoomManager(){
        roomManager.joinRoom(roomInfo.roomId, () -> roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback));
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
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
                }
            });
            rtcEngine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(Constants.cameraDirection));
            rtcEngine.setVideoEncoderConfiguration(Constants.encoderConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = true;
        options.publishAudioTrack = true;
        rtcEngine.joinChannel(getString(R.string.rtc_app_token), roomInfo.roomId, 0, options);
    }

    private void renderLocalPreview() {
        SurfaceView videoView = new SurfaceView(this);
        mBinding.fullVideoContainer.removeAllViews();
        mBinding.fullVideoContainer.addView(videoView);
        rtcEngine.setupLocalVideo(new VideoCanvas(videoView, io.agora.rtc2.Constants.RENDER_MODE_HIDDEN));
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
