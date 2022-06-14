package io.agora.scene.pklivebycdn;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import io.agora.example.base.BaseActivity;
import io.agora.scene.pklivebycdn.databinding.SuperappAudienceDetailActivityBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class AudienceDetailActivity extends BaseActivity<SuperappAudienceDetailActivityBinding> {
    private RoomManager.RoomInfo mRoomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private final RtcManager rtcManager = new RtcManager();
    private boolean isLinking = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);
        mRoomInfo = ((RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo"));

        mBinding.bottomView.setFun1Visible(false)
                .setFun2Visible(false)
                .setupInputText(false, null)
                .setupCloseBtn(true, v -> finish())
                .setupMoreBtn(false, null);

        mBinding.remoteVideoControl.setOnCloseClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomManager.stopLink(mRoomInfo.roomId);
            }
        });

        initRoomManager();
        initRtcManager();
    }

    private void initRoomManager(){
        roomManager.joinRoom(mRoomInfo.roomId, false);
        roomManager.localUserEnterRoom(AudienceDetailActivity.this, mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.UserInfo>() {
            @Override
            public void onSuccess(RoomManager.UserInfo data) {
                mBinding.hostNameView.setName(data.userName);
                mBinding.hostNameView.setIcon(data.getUserIcon());
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AudienceDetailActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        roomManager.subscriptRoomDestroyEvent(mRoomInfo.roomId, new RoomManager.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                onBackPressed();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AudienceDetailActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        roomManager.getRoomPKInfo(mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.PKInfo>() {
            @Override
            public void onSuccess(RoomManager.PKInfo pkInfo) {
                rtcManager.openPlayerSrc(mRoomInfo.roomId, !pkInfo.isPKing() && mRoomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AudienceDetailActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        roomManager.subscriptPKInfoEvent(mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.PKInfo>() {
            @Override
            public void onSuccess(RoomManager.PKInfo pkInfo) {
                if(pkInfo.isPKing()){
                    // 开始连麦
                    if (RoomManager.getCacheUserId().equals(pkInfo.userIdPK)) {
                        rtcManager.stopPlayer();
                        // 连麦对象是自己
                        rtcManager.startRtcStreaming(mRoomInfo.roomId, getString(R.string.rtc_app_token), RoomManager.getCacheUserId(), false);
                        // 渲染本地和远端视图
                        rtcManager.renderLocalVideo(mBinding.fullVideoContainer);
                        mBinding.remoteVideoControl.setVisibility(View.VISIBLE);
                        isLinking = true;
                    } else if (mRoomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN) {
                        // 只是切换到连麦
                        rtcManager.openPlayerSrc(mRoomInfo.roomId, false);
                    }
                }else{
                    // 结束连麦
                    if(isLinking){
                        isLinking = false;
                        mBinding.remoteVideoControl.setVisibility(View.GONE);
                        rtcManager.stopRtcStreaming(mRoomInfo.roomId, () -> {
                            rtcManager.renderPlayerView(mBinding.fullVideoContainer, null);
                            rtcManager.openPlayerSrc(mRoomInfo.roomId, mRoomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN);
                        });
                    }else if (mRoomInfo.liveMode == RoomManager.PUSH_MODE_DIRECT_CDN){
                        rtcManager.openPlayerSrc(mRoomInfo.roomId, true);
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AudienceDetailActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initRtcManager(){
        rtcManager.init(this, getString(R.string.superapp_agora_app_id), new RtcManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {

            }

            @Override
            public void onSuccess() {

            }

            @Override
            public void onUserJoined(int uid) {
                rtcManager.renderRemoteVideo(mBinding.remoteVideoControl.getVideoContainer(), uid);
            }

            @Override
            public void onUserLeaved(int uid, int reason) {

            }
        });
        rtcManager.renderPlayerView(mBinding.fullVideoContainer, null);
    }

    @Override
    public void onBackPressed() {
        roomManager.getRoomPKInfo(mRoomInfo.roomId, new RoomManager.DataCallback<RoomManager.PKInfo>() {
            @Override
            public void onSuccess(RoomManager.PKInfo pkInfo) {
                if(RoomManager.getCacheUserId().equals(pkInfo.userIdPK)){
                    roomManager.stopLink(mRoomInfo.roomId);
                }else{
                    finish();
                }
            }

            @Override
            public void onFailed(Exception e) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomManager.localUserExitRoom(mRoomInfo.roomId);
        roomManager.leaveRoom(mRoomInfo.roomId, false);
    }
}
