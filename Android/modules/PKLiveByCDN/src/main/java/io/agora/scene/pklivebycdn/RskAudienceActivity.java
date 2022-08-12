package io.agora.scene.pklivebycdn;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

import io.agora.example.base.BaseActivity;
import io.agora.mediaplayer.Constants;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.PlayerUpdatedInfo;
import io.agora.mediaplayer.data.SrcInfo;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.pklivebycdn.databinding.SuperappAudienceDetailActivityBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RskAudienceActivity extends BaseActivity<SuperappAudienceDetailActivityBinding> {
    private RoomManager.RoomInfo mRoomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;

    private IMediaPlayer mediaPlayer;
    private IMediaPlayerObserver mediaPlayerObserver = new IMediaPlayerObserver() {
        @Override
        public void onPlayerStateChanged(Constants.MediaPlayerState state, Constants.MediaPlayerError error) {
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
        public void onPlayerEvent(Constants.MediaPlayerEvent eventCode, long elapsedTime, String message) {

        }

        @Override
        public void onMetaData(Constants.MediaPlayerMetadataType type, byte[] data) {

        }

        @Override
        public void onPlayBufferUpdated(long playCachedBuffer) {

        }

        @Override
        public void onPreloadEvent(String src, Constants.MediaPlayerPreloadEvent event) {

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

        mBinding.remoteVideoControl.setVisibility(View.GONE);

        initRoomManager();
        initRtcManager();
    }

    private void initRoomManager(){
        roomManager.joinRoom(mRoomInfo.roomId, false, () -> {
            roomManager.login(RskAudienceActivity.this, mRoomInfo.roomId, data -> runOnUiThread(()->{
                mBinding.hostNameView.setName(data.userName);
                mBinding.hostNameView.setIcon(data.getUserIcon());
            }));
            roomManager.subscriptRoomInfoEvent(mRoomInfo.roomId, null, data -> runOnUiThread(this::showRoomExitDialog));
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

    private void initRtcManager(){
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {});
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();

            mediaPlayer = rtcEngine.createMediaPlayer();
            mediaPlayer.setPlayerOption("fps_probe_size", 0);
            mediaPlayer.registerPlayerObserver(mediaPlayerObserver);

            String pullUrl = String.format(Locale.US, io.agora.scene.pklivebycdn.Constants.AGORA_CDN_CHANNEL_PULL_PREFIX, mRoomInfo.roomId);
            mediaPlayer.openWithAgoraCDNSrc(pullUrl, 0);

            SurfaceView videoView = new SurfaceView(this);
            mBinding.fullVideoContainer.removeAllViews();
            mBinding.fullVideoContainer.addView(videoView);
            rtcEngine.setupLocalVideo(new VideoCanvas(
                    videoView,
                    io.agora.rtc2.Constants.RENDER_MODE_HIDDEN,
                    io.agora.rtc2.Constants.VIDEO_MIRROR_MODE_AUTO,
                    io.agora.rtc2.Constants.VIDEO_SOURCE_MEDIA_PLAYER,
                    mediaPlayer.getMediaPlayerId(), 0
            ));

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
        RtcEngine.destroy();
    }
}
