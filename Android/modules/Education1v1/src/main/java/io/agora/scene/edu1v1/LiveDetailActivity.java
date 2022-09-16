package io.agora.scene.edu1v1;

import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import io.agora.board.fast.FastRoom;
import io.agora.board.fast.Fastboard;
import io.agora.board.fast.model.FastRegion;
import io.agora.board.fast.model.FastRoomOptions;
import io.agora.example.base.BaseActivity;
import io.agora.example.base.TokenGenerator;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.edu1v1.databinding.Edu1v1LiveDetailActivityBinding;
import io.agora.uiwidget.function.LiveToolsDialog;

public class LiveDetailActivity extends BaseActivity<Edu1v1LiveDetailActivityBinding> {

    private RtcEngine rtcEngine;
    private RoomManager.RoomInfo roomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private LiveToolsDialog settingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        initView();
        initRoomManager();
        initRtcEngine();
        initFastBoard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomManager.leaveRoom(roomInfo.roomId, roomInfo.userId.equals(RoomManager.getCacheUserId()));
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (roomInfo.userId.equals(RoomManager.getCacheUserId())) {
            new AlertDialog.Builder(LiveDetailActivity.this)
                    .setTitle(R.string.common_tip)
                    .setMessage(R.string.common_tip_close_room)
                    .setPositiveButton(R.string.common_confirm, (dialog, which) -> super.onBackPressed())
                    .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void initFastBoard() {
        Fastboard fastboard = mBinding.fastboard.getFastboard();
        FastRoomOptions roomOptions = new FastRoomOptions(
                io.agora.scene.edu1v1.Constants.BOARD_APP_ID,
                io.agora.scene.edu1v1.Constants.BOARD_ROOM_UUID,
                io.agora.scene.edu1v1.Constants.BOARD_ROOM_TOKEN,
                RoomManager.getCacheUserId(),
                FastRegion.CN_HZ
        );

        FastRoom fastRoom = fastboard.createFastRoom(roomOptions);
        fastRoom.join();
    }

    private void initRtcEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = this;
            config.mAppId = getString(R.string.rtc_app_id);
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> Toast.makeText(LiveDetailActivity.this, "User join channel successfully. The user id is " + uid, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);

                    runOnUiThread(() -> {
                        mBinding.tvWaitingRemote.setVisibility(View.GONE);
                        // render remote view
                        TextureView renderView = new TextureView(LiveDetailActivity.this);
                        mBinding.remoteVideoContainer.setTag(uid);
                        mBinding.remoteVideoContainer.removeAllViews();
                        mBinding.remoteVideoContainer.addView(renderView);
                        rtcEngine.setupRemoteVideo(new VideoCanvas(renderView, Constants.RENDER_MODE_HIDDEN, uid));

                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);

                    runOnUiThread(() -> {
                        Object tag = mBinding.remoteVideoContainer.getTag();
                        if (tag instanceof Integer && (Integer) tag == uid) {
                            mBinding.tvWaitingRemote.setVisibility(View.VISIBLE);
                            mBinding.remoteVideoContainer.removeAllViews();
                            rtcEngine.setupRemoteVideo(new VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, uid));
                        }
                    });
                }
            };
            rtcEngine = RtcEngine.create(config);
            rtcEngine.setVideoEncoderConfiguration(io.agora.scene.edu1v1.Constants.encoderConfiguration);
            rtcEngine.enableVideo();

            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + 100 + ","
                    + "\"serviceType\":" + 12 + ","
                    + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                    + "}"
                    + "}");

            // render local view
            TextureView localRenderView = new TextureView(this);
            mBinding.localVideoContainer.removeAllViews();
            mBinding.localVideoContainer.addView(localRenderView);
            rtcEngine.setupLocalVideo(new VideoCanvas(localRenderView, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(RoomManager.getCacheUserId())));

            // join channel
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.publishCameraTrack = true;
            options.publishMicrophoneTrack = true;
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(this, roomInfo.roomId, uid, ret -> {
                rtcEngine.joinChannel(ret, roomInfo.roomId, uid, options);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeRoomDeleteEvent(roomInfo.roomId, data -> {
                runOnUiThread(() -> new AlertDialog.Builder(LiveDetailActivity.this)
                        .setTitle(R.string.common_tip)
                        .setMessage(R.string.common_tip_room_closed)
                        .setCancelable(false)
                        .setPositiveButton(R.string.common_confirm, (dialog, which) -> finish())
                        .show());
            });
        });
    }

    private void initView() {
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");
        mBinding.bottomView
                .setupCloseBtn(true, v -> onBackPressed())
                .setupMoreBtn(true, v -> showSettingDialog())
                .setFun1Visible(false)
                .setFun2Visible(false)
                .setupInputText(false, null);
    }

    private void showSettingDialog() {
        if (settingDialog == null) {
            settingDialog = new LiveToolsDialog(this)
                    .addToolItem(LiveToolsDialog.TOOL_ITEM_SPEAKER, true, (view, item) -> {
                        if (rtcEngine != null) {
                            rtcEngine.enableLocalAudio(item.activated);
                        }
                    })
                    .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> {
                        if (rtcEngine != null) {
                            rtcEngine.switchCamera();
                        }
                    });
        }
        settingDialog.show();
    }

}
