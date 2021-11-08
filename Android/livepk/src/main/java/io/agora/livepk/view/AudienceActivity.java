package io.agora.livepk.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.UUID;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.livepk.R;
import io.agora.livepk.databinding.ActivityVideoBinding;
import io.agora.livepk.manager.RtcManager;
import io.agora.livepk.model.RoomInfo;
import io.agora.livepk.model.UserInfo;
import io.agora.livepk.util.UserUtil;
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

        mDataBinding.remoteCallCloseBtn.setOnClickListener(v -> {
           stopPK();
        });
    }

    @Override
    protected void iniListener() {
        mDataBinding.liveBottomBtnClose.setOnClickListener(v -> onBackPressed());
        mDataBinding.liveBottomBtnMore.setOnClickListener(v-> showMoreChoiceDialog());
    }

    @Override
    protected void iniData() {
        initRTCManager();
        initSyncManager();
        enterRoom();
        setupVideoPlayer();
    }

    private void showMoreChoiceDialog() {
        final int itemPadding = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        final int gridSpan = 4;
        final int[] toolIcons = {
                R.drawable.icon_rotate,
                R.drawable.action_sheet_tool_speaker
        };
        final String[] toolNames = getResources().getStringArray(R.array.live_room_action_sheet_tool_names);

        final class ViewHolder extends RecyclerView.ViewHolder{
            final ImageView iconIv;
            final TextView toolNameTv;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconIv = itemView.findViewById(R.id.live_room_action_sheet_tool_item_icon);
                toolNameTv = itemView.findViewById(R.id.live_room_action_sheet_tool_item_name);
            }
        }

        RelativeLayout dialogView = new RelativeLayout(this);
        LayoutInflater.from(this).inflate(R.layout.action_tool, dialogView, true);
        RecyclerView recyclerView = dialogView.findViewById(R.id.live_room_action_sheet_tool_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, gridSpan));
        recyclerView.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.action_tool_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                holder.toolNameTv.setText(toolNames[position]);
                holder.iconIv.setImageResource(toolIcons[position]);
                switch (position){
                    case 1:
                        // 静音
                        holder.iconIv.setActivated(!RtcManager.isMuteLocalAudio);
                        break;
                }

                holder.iconIv.setOnClickListener(v -> {
                    switch (position){
                        case 0:
                            // 翻转摄像头
                            rtcManager.switchCamera();
                            break;
                        case 1:
                            // 静音
                            rtcManager.muteLocalAudio(!RtcManager.isMuteLocalAudio);
                            holder.iconIv.setActivated(!RtcManager.isMuteLocalAudio);
                            break;
                    }
                });
            }

            @Override
            public int getItemCount() {
                return toolNames.length;
            }
        });
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = itemPadding;
                outRect.bottom = itemPadding;
            }
        });

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        hideStatusBar(dialog.getWindow(), false);
        dialog.show();
    }

    private void hideStatusBar(Window window, boolean darkText) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);

        int flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && darkText) {
            flag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }

        window.getDecorView().setSystemUiVisibility(flag |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }


    private void setupVideoPlayer(){
        mDataBinding.flLocalFullContainer.setVisibility(View.VISIBLE);
        // remove old video view
        mDataBinding.flLocalFullContainer.removeAllViews();

        mDataBinding.ivLoadingBg.setVisibility(View.GONE);
        rtcManager.renderPlayerView(mDataBinding.flLocalFullContainer,  () -> {
            runOnUiThread(() -> mDataBinding.ivLoadingBg.setVisibility(View.GONE));
        });
        rtcManager.openPlayerSrc(mRoomInfo.roomId, mRoomInfo.mode == RoomInfo.PUSH_MODE_DIRECT_CDN && !checkRoomIsPKing());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtcManager.release();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(checkMeIsPKing()){
            showPKEndDialog();
        }else{
            leaveRoom(this::finish);
        }
    }

    private void updateRoomUserCountTv(){
        mDataBinding.liveParticipantCountText.setText(mRoomInfo.userCount + "");
    }

    private void showPKEndDialog() {
        new AlertDialog.Builder(AudienceActivity.this)
                .setTitle("PK")
                .setMessage("Currently in PK, whether to stop PK?")
                .setPositiveButton(R.string.cmm_ok, (dialog, which) -> {
                    dialog.dismiss();
                    stopPK();
                })
                .setNegativeButton(R.string.cmm_cancel, (dialog, which) -> dialog.dismiss())
                .show();
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
                runOnUiThread(() -> {
                    mDataBinding.remoteCallLayout.setVisibility(View.VISIBLE);
                    rtcManager.renderRemoteVideo(mDataBinding.remoteCallVideoLayout, uid, null);
                });
            }

            @Override
            public void onUserLeaved(int uid, int reason) {

            }
        });
    }

    private void startRTCPK() {
        rtcManager.startRtcStreaming(mRoomInfo.roomId, false);
        rtcManager.stopPlayer();
        runOnUiThread(() -> {
            mDataBinding.liveBottomBtnMore.setVisibility(View.VISIBLE);
            mDataBinding.flLocalFullContainer.removeAllViews();
            mDataBinding.ivLoadingBg.setVisibility(View.GONE);
            rtcManager.renderLocalVideo(mDataBinding.flLocalFullContainer, null);
        });
    }

    private void stopRTCPK(){
        rtcManager.stopRtcStreaming(mRoomInfo.roomId, null);
        runOnUiThread(() -> {
            mDataBinding.liveBottomBtnMore.setVisibility(View.GONE);
            mDataBinding.remoteCallVideoLayout.removeAllViews();
            mDataBinding.remoteCallLayout.setVisibility(View.GONE);
            setupVideoPlayer();
        });
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
                        runOnUiThread(() -> onRoomInfoChanged(item));
                    }

                    @Override
                    public void onUpdated(IObject item) {
                        runOnUiThread(() -> onRoomInfoChanged(item));
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
                if (mUserInfo != null && mUserInfo.userId.equals(roomInfo.userIdPK)) {
                    Toast.makeText(this, "Start PK", Toast.LENGTH_LONG).show();
                    startRTCPK();
                } else {
                    if (mRoomInfo.mode == RoomInfo.PUSH_MODE_DIRECT_CDN) {
                        setupVideoPlayer();
                    }
                }
            } else {
                // 停止PK
                if(mUserInfo != null && mUserInfo.userId.equals(oldRoomInfo.userIdPK)){
                    Toast.makeText(this, "Stop PK", Toast.LENGTH_LONG).show();
                    stopRTCPK();
                } else {
                    if (mRoomInfo.mode == RoomInfo.PUSH_MODE_DIRECT_CDN) {
                        setupVideoPlayer();
                    }
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
        mUserInfo.userId = UserUtil.getLocalUserId();
        mUserInfo.userName = UserUtil.getLocalUserName(this);
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

    private void stopPK() {
        RoomInfo newRoomInfo = new RoomInfo(mRoomInfo);
        newRoomInfo.userIdPK = "";
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_ROOM_INFO)
                .document(mRoomInfo.roomId)
                .update(newRoomInfo.toMap(), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {

                    }
                });
    }

    private boolean checkMeIsPKing() {
        return !TextUtils.isEmpty(mUserInfo.userId) && mUserInfo.userId.equals(mRoomInfo.userIdPK);
    }

    private boolean checkRoomIsPKing() {
        return !TextUtils.isEmpty(mRoomInfo.userIdPK);
    }
}
