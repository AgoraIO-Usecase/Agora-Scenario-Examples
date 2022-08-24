package io.agora.scene.pklivebycdn;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

import io.agora.example.base.BaseActivity;
import io.agora.example.base.TokenGenerator;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.PlayerUpdatedInfo;
import io.agora.mediaplayer.data.SrcInfo;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.pklivebycdn.databinding.SuperappAudienceDetailActivityBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RtcAudienceActivity extends BaseActivity<SuperappAudienceDetailActivityBinding> {
    private RoomManager.RoomInfo mRoomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;
    private boolean isLinking = false;

    private IMediaPlayer mediaPlayer;
    private IMediaPlayerObserver mediaPlayerObserver = new IMediaPlayerObserver() {
        @Override
        public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState state, io.agora.mediaplayer.Constants.MediaPlayerError error) {
            Log.d("MediaPlayer", "MediaPlayer onPlayerStateChanged -- url=" + mediaPlayer.getPlaySrc() + "state=" + state + ", error=" + error);
            if (state == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
                if (mediaPlayer != null) {
                    mediaPlayer.play();
                }
            }
        }

        @Override
        public void onPositionChanged(long position) {

        }

        @Override
        public void onPlayerEvent(io.agora.mediaplayer.Constants.MediaPlayerEvent eventCode, long elapsedTime, String message) {

        }

        @Override
        public void onMetaData(io.agora.mediaplayer.Constants.MediaPlayerMetadataType type, byte[] data) {

        }

        @Override
        public void onPlayBufferUpdated(long playCachedBuffer) {

        }

        @Override
        public void onPreloadEvent(String src, io.agora.mediaplayer.Constants.MediaPlayerPreloadEvent event) {

        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onAgoraCDNTokenWillExpire() {

        }

        @Override
        public void onPlayerSrcInfoChanged(SrcInfo from, SrcInfo to) {

        }

        @Override
        public void onPlayerInfoUpdated(PlayerUpdatedInfo info) {

        }

        @Override
        public void onAudioVolumeIndication(int volume) {

        }
    };

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
        roomManager.joinRoom(mRoomInfo.roomId, false, ()->{
            roomManager.login(RtcAudienceActivity.this, mRoomInfo.roomId, data -> runOnUiThread(()->{
                mBinding.hostNameView.setName(data.userName);
                mBinding.hostNameView.setIcon(data.getUserIcon());
            }));
            roomManager.getRoomPKInfo(mRoomInfo.roomId, pkInfo -> runOnUiThread(()->{
                if (pkInfo.isPKing() && pkInfo.userIdPK.equals(RoomManager.getCacheUserId())) {
                    startLinking();
                } else {
                    openMediaPlayer();
                }
            }));
            roomManager.subscriptRoomInfoEvent(mRoomInfo.roomId, pkInfo -> runOnUiThread(()->{
                if (pkInfo.isPKing()) {
                    // 开始连麦
                    if (RoomManager.getCacheUserId().equals(pkInfo.userIdPK)) {
                        startLinking();
                    }
                } else {
                    // 结束连麦
                    stopLinking();
                }
            }), data -> runOnUiThread(this::showRoomExitDialog));
        });
    }

    private void showRoomExitDialog(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_room_closed)
                .setCancelable(false)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    onBackPressed();
                })
                .show();
    }

    private void startLinking() {
        if(isLinking){
            return;
        }
        isLinking = true;
        mediaPlayer.stop();

        rtcEngine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(io.agora.scene.pklivebycdn.Constants.currCameraDirection));
        rtcEngine.setVideoEncoderConfiguration(io.agora.scene.pklivebycdn.Constants.encoderConfiguration);

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        TokenGenerator.gen(this, mRoomInfo.roomId, 0, ret -> {
            rtcEngine.joinChannel(ret, mRoomInfo.roomId, 0, options);
        });

        SurfaceView videoView = new SurfaceView(this);
        mBinding.fullVideoContainer.removeAllViews();
        mBinding.fullVideoContainer.addView(videoView);
        rtcEngine.setupLocalVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN));

        mBinding.remoteVideoControl.setVisibility(View.VISIBLE);
    }

    private void stopLinking(){
        if(!isLinking){
            return;
        }
        isLinking = false;

        rtcEngine.leaveChannel();

        openMediaPlayer();

        mBinding.remoteVideoControl.setVisibility(View.GONE);
    }

    private void openMediaPlayer() {
        mediaPlayer.open(getPullUrl(), 0);

        SurfaceView videoView = new SurfaceView(this);
        mBinding.fullVideoContainer.removeAllViews();
        mBinding.fullVideoContainer.addView(videoView);
        rtcEngine.setupLocalVideo(new VideoCanvas(videoView,
                Constants.RENDER_MODE_HIDDEN,
                Constants.VIDEO_MIRROR_MODE_AUTO,
                Constants.VIDEO_SOURCE_MEDIA_PLAYER,
                mediaPlayer.getMediaPlayerId(), 0));
    }

    private String getPullUrl(){
        return String.format(Locale.US, io.agora.scene.pklivebycdn.Constants.AGORA_CDN_CHANNEL_PULL_PREFIX, mRoomInfo.roomId);
    }

    private void initRtcManager(){
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> {

                        SurfaceView videoView = new SurfaceView(RtcAudienceActivity.this);
                        mBinding.remoteVideoControl.getVideoContainer().removeAllViews();
                        mBinding.remoteVideoControl.getVideoContainer().addView(videoView);
                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, uid));
                    });
                }
            });
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();

            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + 100 + ","
                    + "\"serviceType\":" + 11 + ","
                    + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                    + "}"
                    + "}");

            mediaPlayer = rtcEngine.createMediaPlayer();
            mediaPlayer.registerPlayerObserver(mediaPlayerObserver);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomManager.logout(mRoomInfo.roomId);
        roomManager.leaveRoom(mRoomInfo.roomId, false);

        rtcEngine.stopPreview();
        mediaPlayer.unRegisterPlayerObserver(mediaPlayerObserver);
        mediaPlayer.stop();
        mediaPlayer.destroy();
        stopLinking();
        RtcEngine.destroy();
    }
}
