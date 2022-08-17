package io.agora.scene.videocall;

import android.os.Bundle;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.agora.example.base.BaseActivity;
import io.agora.example.base.TokenGenerator;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.VideoCanvas;
import io.agora.scene.videocall.databinding.VideoCallLiveDetailActivityBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class LiveDetailActivity extends BaseActivity<VideoCallLiveDetailActivityBinding> {

    private RtcEngine rtcEngine;
    private RoomManager.RoomInfo roomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private Timer timer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        initView();
        initRoomManager();
        initRtcEngine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetTimer(false);
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

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> Toast.makeText(LiveDetailActivity.this, "User join channel successfully. The user id is " + uid, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);

                    runOnUiThread(() -> {
                        resetTimer(true);
                        // render remote view
                        TextureView renderView = RtcEngine.CreateTextureView(LiveDetailActivity.this);
                        mBinding.fullVideoContainer.setTag(uid);
                        mBinding.fullVideoContainer.removeAllViews();
                        mBinding.fullVideoContainer.addView(renderView);
                        rtcEngine.setupRemoteVideo(new VideoCanvas(renderView, Constants.RENDER_MODE_HIDDEN, uid));

                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);

                    runOnUiThread(() -> {
                        Object tag = mBinding.fullVideoContainer.getTag();
                        if (tag instanceof Integer && (Integer) tag == uid) {
                            resetTimer(false);
                            mBinding.fullVideoContainer.removeAllViews();
                        }
                    });
                }
            });
            rtcEngine.setVideoEncoderConfiguration(io.agora.scene.videocall.Constants.encoderConfiguration);
            rtcEngine.enableVideo();

            // render local view
            TextureView localRenderView = RtcEngine.CreateTextureView(this);
            mBinding.smallVideoContainer.removeAllViews();
            mBinding.smallVideoContainer.addView(localRenderView);
            rtcEngine.setupLocalVideo(new VideoCanvas(localRenderView, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(RoomManager.getCacheUserId())));

            // join channel
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishLocalVideo = true;
            options.publishLocalAudio = true;
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(this, roomInfo.roomId, uid, ret -> rtcEngine.joinChannel(ret, roomInfo.roomId, "", uid, options));


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
        mBinding.btnCamera.setOnClickListener(v -> {
            if (rtcEngine != null) {
                rtcEngine.switchCamera();
            }
        });
        mBinding.btnEnd.setOnClickListener(v -> {
            onBackPressed();
        });
        mBinding.btnMic.setActivated(true);
        mBinding.btnMic.setOnClickListener(v -> {
            boolean activated = v.isActivated();
            if (rtcEngine != null) {
                rtcEngine.enableLocalAudio(!activated);
                v.setActivated(!activated);
            }
        });
    }

    private void resetTimer(boolean start) {
        if (timer != null) {
            runOnUiThread(() -> mBinding.tvTimer.setText("00:00"));
            timer.cancel();
            timer = null;
        }
        if (start) {
            timer = new Timer("LiveTimer");
            timer.scheduleAtFixedRate(new TimerTask() {
                long currSecond = -1;
                final SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss", Locale.US);

                @Override
                public void run() {
                    currSecond++;
                    runOnUiThread(() -> mBinding.tvTimer.setText(dateFormat.format(new Date(currSecond * 1000))));
                }
            }, 0, 1000);
        }
    }
}
