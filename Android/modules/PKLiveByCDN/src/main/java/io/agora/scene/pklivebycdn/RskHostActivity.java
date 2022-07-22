package io.agora.scene.pklivebycdn;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Locale;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DirectCdnStreamingError;
import io.agora.rtc2.DirectCdnStreamingMediaOptions;
import io.agora.rtc2.DirectCdnStreamingState;
import io.agora.rtc2.DirectCdnStreamingStats;
import io.agora.rtc2.IDirectCdnStreamingEventHandler;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.pklivebycdn.databinding.SuperappHostDetailActivityBinding;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RskHostActivity extends BaseActivity<SuperappHostDetailActivityBinding> {
    private RoomManager.RoomInfo mRoomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;
    private RoomManager.DataListCallback<RoomManager.UserInfo> userInfoDataListCallback = dataList -> runOnUiThread(()->{
        mBinding.userView.setUserCount(dataList.size());
        mBinding.userView.removeAllUserIcon();
        for (int i = 1; i <= 3; i++) {
            int index = dataList.size() - i;
            if(index >= 0){
                mBinding.userView.addUserIcon(dataList.get(index).getUserIcon(), null);
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        mRoomInfo = ((RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo"));

        mBinding.hostNameView.setName(mRoomInfo.roomName + "(" + mRoomInfo.roomId + ")");
        mBinding.hostNameView.setIcon(mRoomInfo.getBgResId());

        mBinding.userView.setVisibility(View.GONE);

        mBinding.bottomView.setFun1Visible(false)
                .setFun2Visible(false)
                .setupInputText(false, null)
                .setupCloseBtn(true, v -> onBackPressed())
                .setupMoreBtn(true, v -> {
                    // 显示工具弹窗
                    new LiveToolsDialog(RskHostActivity.this)
                            .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> {
                                if(rtcEngine != null){
                                    rtcEngine.switchCamera();
                                }
                            })
                            .show();
                });

        initRoomManager();
        initRtcManager();
    }

    private void initRoomManager() {
        roomManager.joinRoom(mRoomInfo.roomId, true, ()-> {
            roomManager.subscriptUserChangeEvent(mRoomInfo.roomId, userInfoDataListCallback);
            roomManager.getRoomUserList(mRoomInfo.roomId, userInfoDataListCallback);
        });
    }

    private void initRtcManager() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> {
                        SurfaceView videoView = new SurfaceView(RskHostActivity.this);
                        mBinding.remoteVideoControl.removeAllViews();
                        mBinding.remoteVideoControl.addView(videoView);
                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, uid));
                    });
                }
            });
            rtcEngine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(io.agora.scene.pklivebycdn.Constants.currCameraDirection));
            rtcEngine.setVideoEncoderConfiguration(io.agora.scene.pklivebycdn.Constants.encoderConfiguration);

            SurfaceView videoView = new SurfaceView(this);
            mBinding.fullVideoContainer.removeAllViews();
            mBinding.fullVideoContainer.addView(videoView);
            rtcEngine.setupLocalVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN));
            rtcEngine.startPreview();

            rtcEngine.setDirectCdnStreamingVideoConfiguration(io.agora.scene.pklivebycdn.Constants.encoderConfiguration);
            String publishUrl = String.format(Locale.US, io.agora.scene.pklivebycdn.Constants.AGORA_CDN_CHANNEL_PUSH_PREFIX, mRoomInfo.roomId);
            DirectCdnStreamingMediaOptions options = new DirectCdnStreamingMediaOptions();
            options.publishCameraTrack = true;
            options.publishMicrophoneTrack = true;
            rtcEngine.startDirectCdnStreaming(new IDirectCdnStreamingEventHandler() {
                @Override
                public void onDirectCdnStreamingStateChanged(DirectCdnStreamingState state,
                                                             DirectCdnStreamingError error,
                                                             String message) {
                    if(error != DirectCdnStreamingError.OK){
                        runOnUiThread(() -> Toast.makeText(RskHostActivity.this, "startDirectCdnStreaming error=" + error + ",message=" + message + ",state=" + state, Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void onDirectCdnStreamingStats(DirectCdnStreamingStats stats) {

                }

            }, publishUrl, options);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onBackPressed() {
        // 判断是否正在连麦，连麦中的话弹出结束连麦弹窗
        roomManager.getRoomPKInfo(mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.PKInfo>() {
            @Override
            public void onObtained(RoomManager.PKInfo pkInfo) {
                if (pkInfo.isPKing()) {
                    roomManager.stopLink(mRoomInfo.roomId);
                } else {
                    finish();
                }

            }
        });
    }

    @Override
    public void finish() {
        roomManager.leaveRoom(mRoomInfo.roomId, true);
        rtcEngine.stopPreview();
        rtcEngine.stopDirectCdnStreaming();
        RtcEngine.destroy();
        super.finish();
    }
}
