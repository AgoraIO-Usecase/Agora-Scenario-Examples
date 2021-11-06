package io.agora.livepk.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.livepk.R;
import io.agora.livepk.databinding.ActivityVideoBinding;
import io.agora.livepk.manager.RtcManager;
import io.agora.livepk.manager.RtmManager;
import io.agora.livepk.model.RoomInfo;
import io.agora.livepk.model.UserInfo;
import io.agora.livepk.util.UserUtil;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

import static io.agora.livepk.Constants.SYNC_COLLECTION_ROOM_INFO;
import static io.agora.livepk.Constants.SYNC_COLLECTION_USER_INFO;
import static io.agora.livepk.Constants.SYNC_SCENE_ID;

public class AudienceActivity extends DataBindBaseActivity<ActivityVideoBinding> {
    private static final String TAG = "AudienceActivity";

    private static final String EXTRA_ROOM_INFO = "roomInfo";

    private final RtcManager rtcManager = new RtcManager();
    private RoomInfo mRoomInfo;
    private UserInfo mUserInfo;

    public static Intent launch(Context context, RoomInfo roomInfo) {
        Intent intent = new Intent(context, AudienceActivity.class);
        intent.putExtra(EXTRA_ROOM_INFO, roomInfo);
        return intent;
    }

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {
        mRoomInfo = (RoomInfo)getIntent().getSerializableExtra(EXTRA_ROOM_INFO);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_video;
    }

    @Override
    protected void iniView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setTitle(mRoomInfo.roomName);
        setStatusBarStyle(true);
        setStatusBarTransparent();

        int userProfileIcon = UserUtil.getUserProfileIcon(mRoomInfo.roomId);

        mDataBinding.ivLoadingBg.setVisibility(View.VISIBLE);
        mDataBinding.ivLoadingBg.setImageResource(userProfileIcon);

        Glide.with(this)
                .load(userProfileIcon)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(mDataBinding.ivRoomAvatar);

        mDataBinding.tvRoomName.setText(mRoomInfo.roomName);
        mDataBinding.liveBottomBtnMore.setVisibility(View.GONE);
        updateRoomUserCountTv();
    }

    @Override
    protected void iniListener() {
        mDataBinding.liveBottomBtnClose.setOnClickListener(v -> onBackPressed());
    }

    @Override
    protected void iniData() {
        initRTCManager();
        initSyncManager();
        setupVideoPlayer();
        enterRoom();
    }


    private void setupVideoPlayer(){
        mDataBinding.flLocalFullContainer.setVisibility(View.VISIBLE);
        // remove old video view
        mDataBinding.flLocalFullContainer.removeAllViews();

        rtcManager.startDirectCDNPlayer(mDataBinding.flLocalFullContainer, mRoomInfo.roomId, () -> {
            runOnUiThread(() -> mDataBinding.ivLoadingBg.setVisibility(View.GONE));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtcManager.release();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        leaveRoom(this::finish);
    }

    private void updateRoomUserCountTv(){
        mDataBinding.liveParticipantCountText.setText(mRoomInfo.userCount + "");
    }



    //============================RTCManager Logic======================================


    private void initRTCManager(){
        rtcManager.init(this, getAgoraAppId(), new RtcManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {

            }

            @Override
            public void onSuccess() {

            }

            @Override
            public void onUserJoined(int uid) {

            }
        });
    }

    private void startRTCPK() {
        rtcManager.startRtcStreaming(mRoomInfo.roomId, false);
    }

    private void stopRTCPK(){
        rtcManager.stopRtcStreaming();
    }

    private String getAgoraAppId() {
        String appId = getString(R.string.agora_app_id);
        if (TextUtils.isEmpty(appId)) {
            throw new RuntimeException("the app id is empty");
        }
        return appId;
    }





    //============================SyncManager Logic======================================


    private void initSyncManager(){
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_ROOM_INFO)
                .document(mRoomInfo.roomId)
                .subscribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(IObject item) {
                        onRoomInfoChanged(item);
                    }

                    @Override
                    public void onUpdated(IObject item) {
                        onRoomInfoChanged(item);
                    }

                    @Override
                    public void onDeleted(IObject item) {

                    }

                    @Override
                    public void onSubscribeError(SyncManagerException ex) {

                    }
                });
    }

    private void onRoomInfoChanged(IObject item) {
        RoomInfo roomInfo = item.toObject(RoomInfo.class);
        RoomInfo oldRoomInfo = mRoomInfo;
        mRoomInfo = roomInfo;
        boolean newPkStatue = !TextUtils.isEmpty(roomInfo.userIdPK);
        boolean oldPkStatue = !TextUtils.isEmpty(oldRoomInfo.userIdPK);

        if (newPkStatue != oldPkStatue) {
            if (newPkStatue) {
                // 开始PK
                if(mUserInfo != null && mUserInfo.userId.equals(roomInfo.userIdPK)){
                    startRTCPK();
                }
            } else {
                // 停止PK
                if(mUserInfo != null && mUserInfo.userId.equals(oldRoomInfo.userIdPK)){
                    stopRTCPK();
                }
            }
        }

        int newUserCount = roomInfo.userCount;
        int oldUserCount = oldRoomInfo.userCount;
        if(newUserCount != oldUserCount){
            // 更新房间内人数
            runOnUiThread(this::updateRoomUserCountTv);
        }
    }

    private void enterRoom() {
        mUserInfo = new UserInfo();
        mUserInfo.roomId = mRoomInfo.roomId;
        mUserInfo.userId = UUID.randomUUID().toString();
        mUserInfo.userName = "张三";
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_USER_INFO)
                .add(mUserInfo.toMap(), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        Toast.makeText(AudienceActivity.this, exception.toString(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void leaveRoom(Runnable success){
        if(mUserInfo == null){
            if(success != null){
                success.run();
            }
            return;
        }
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_USER_INFO)
                .document(mUserInfo.userId)
                .delete(new SyncManager.Callback() {
                    @Override
                    public void onSuccess() {
                        if(success != null){
                            success.run();
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        Toast.makeText(AudienceActivity.this, exception.toString(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
