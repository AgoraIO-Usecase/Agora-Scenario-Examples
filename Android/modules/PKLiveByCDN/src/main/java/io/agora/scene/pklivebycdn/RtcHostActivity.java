package io.agora.scene.pklivebycdn;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.util.List;
import java.util.Locale;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.live.LiveTranscoding;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.pklivebycdn.databinding.SuperappHostDetailActivityBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.OnlineUserListDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RtcHostActivity extends BaseActivity<SuperappHostDetailActivityBinding> {
    private RoomManager.RoomInfo mRoomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;
    private LiveTranscoding rtmpTranscoding;
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

        mBinding.userView.setTotalLayoutClickListener(v -> {
            // 显示在线用户列表弹窗
            roomManager.getRoomUserList(mRoomInfo.roomId, new RoomManager.DataListCallback<RoomManager.UserInfo>() {
                @Override
                public void onObtained(List<RoomManager.UserInfo> dataList) {
                    showOnlineUserListDialog(dataList);
                }
            });
        });

        mBinding.bottomView.setFun1Visible(false)
                .setFun2Visible(false)
                .setupInputText(false, null)
                .setupCloseBtn(true, v -> onBackPressed())
                .setupMoreBtn(true, v -> {
                    // 显示工具弹窗
                    new LiveToolsDialog(RtcHostActivity.this)
                            .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> {
                                if (rtcEngine != null) {
                                    rtcEngine.switchCamera();
                                }
                            })
                            .show();
                });

        mBinding.remoteVideoControl.setOnCloseClickListener(v -> roomManager.stopLink(mRoomInfo.roomId));

        initRoomManager();
        initRtcManager();
    }

    private void showOnlineUserListDialog(List<RoomManager.UserInfo> dataList) {
        OnlineUserListDialog dialog = new OnlineUserListDialog(RtcHostActivity.this);
        dialog.setListAdapter(
                new OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo>() {
                    @Override
                    protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position, RoomManager.UserInfo item) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(),
                                BitmapFactory.decodeResource(getResources(), item.getUserIcon()));
                        drawable.setCircular(true);
                        holder.binding.ivIcon.setImageDrawable(drawable);
                        holder.binding.tvName.setText(item.userName);
                        holder.binding.tvStatus.setOnClickListener(v1 -> {
                            // 开始连麦
                            roomManager.startLinkWith(mRoomInfo.roomId, item.userId);
                            dialog.dismiss();
                        });
                    }
                }.resetAll(dataList)
        ).show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(mRoomInfo.roomId, true);
        roomManager.subscriptRoomInfoEvent(mRoomInfo.roomId, pkInfo -> {
            if (!pkInfo.isPKing()) {
                // 结束连麦
                // 隐藏远程视图
                mBinding.remoteVideoControl.setVisibility(View.GONE);

            } else {
                // 开始连麦
                // 显示远程视图
                mBinding.remoteVideoControl.setVisibility(View.VISIBLE);
            }
        }, null);
        roomManager.subscriptUserChangeEvent(mRoomInfo.roomId, userInfoDataListCallback);
        roomManager.getRoomUserList(mRoomInfo.roomId, userInfoDataListCallback);
    }

    private void initRtcManager() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);

                    String publishUrl = String.format(Locale.US, io.agora.scene.pklivebycdn.Constants.AGORA_CDN_CHANNEL_PUSH_PREFIX, mRoomInfo.roomId);

                    int width = Math.min(io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions.height, io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions.width);
                    int height = Math.max(io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions.height, io.agora.scene.pklivebycdn.Constants.encoderConfiguration.dimensions.width);
                    rtmpTranscoding = new LiveTranscoding();
                    rtmpTranscoding.width = width;
                    rtmpTranscoding.height = height;
                    rtmpTranscoding.videoBitrate = io.agora.scene.pklivebycdn.Constants.encoderConfiguration.bitrate;
                    rtmpTranscoding.videoFramerate = io.agora.scene.pklivebycdn.Constants.encoderConfiguration.frameRate;

                    LiveTranscoding.TranscodingUser user = new LiveTranscoding.TranscodingUser();
                    user.uid = uid;
                    user.x = user.y = 0;
                    user.width = width;
                    user.height = height;
                    rtmpTranscoding.addUser(user);
                    rtcEngine.startRtmpStreamWithTranscoding(publishUrl, rtmpTranscoding);
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);

                    if (rtmpTranscoding != null) {
                        LiveTranscoding.TranscodingUser user = new LiveTranscoding.TranscodingUser();
                        user.x = rtmpTranscoding.width / 2;
                        user.y = 0;
                        user.width = rtmpTranscoding.width / 2;
                        user.height = rtmpTranscoding.height / 2;
                        user.uid = uid;
                        user.zOrder = 1;
                        rtmpTranscoding.addUser(user);
                        rtcEngine.updateRtmpTranscoding(rtmpTranscoding);
                    }

                    runOnUiThread(() -> {
                        SurfaceView videoView = new SurfaceView(RtcHostActivity.this);
                        mBinding.remoteVideoControl.getVideoContainer().removeAllViews();
                        mBinding.remoteVideoControl.getVideoContainer().addView(videoView);
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

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.publishCameraTrack = true;
            options.publishAudioTrack = true;
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            rtcEngine.joinChannel(getString(R.string.rtc_app_token), mRoomInfo.roomId, 0, options);
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
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }
}
