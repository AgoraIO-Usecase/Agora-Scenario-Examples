package io.agora.livepk.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.livepk.R;
import io.agora.livepk.databinding.ActivityVideoBinding;
import io.agora.livepk.manager.RtcManager;
import io.agora.livepk.model.RoomInfo;
import io.agora.livepk.model.UserInfo;
import io.agora.livepk.util.DataCallback;
import io.agora.livepk.util.UserUtil;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

import static io.agora.livepk.Constants.SYNC_COLLECTION_ROOM_INFO;
import static io.agora.livepk.Constants.SYNC_COLLECTION_USER_INFO;
import static io.agora.livepk.Constants.SYNC_SCENE_ID;

public class HostPKActivity extends DataBindBaseActivity<ActivityVideoBinding> {
    private static final String TAG = "HostPKActivity";

    private static final String EXTRA_ROOM_INFO = "roomInfo";

    public static Intent launch(Context context, RoomInfo roomInfo) {
        Intent intent = new Intent(context, HostPKActivity.class);
        intent.putExtra(EXTRA_ROOM_INFO, roomInfo);
        return intent;
    }

    private final RtcManager rtcManager = new RtcManager();
    private RoomInfo mRoomInfo;

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {
        mRoomInfo = ((RoomInfo) getIntent().getSerializableExtra(EXTRA_ROOM_INFO));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_video;
    }

    @Override
    protected void iniView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setStatusBarStyle(true);
        setStatusBarTransparent();
        setTitle(mRoomInfo.roomName);

        int userProfileIcon = UserUtil.getUserProfileIcon(getLocalRoomId());

        mDataBinding.ivLoadingBg.setVisibility(View.VISIBLE);
        mDataBinding.ivLoadingBg.setImageResource(userProfileIcon);

        Glide.with(this)
                .load(userProfileIcon)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(mDataBinding.ivRoomAvatar);

        mDataBinding.tvRoomName.setText(mRoomInfo.roomName);
        updateRoomUserCountTv();
    }

    private void updateRoomUserCountTv() {
        mDataBinding.liveParticipantCountText.setText(mRoomInfo.userCount + "");
    }

    @Override
    protected void iniListener() {
        mDataBinding.liveBottomBtnClose.setOnClickListener(v -> onBackPressed());
        mDataBinding.llParticipant.setOnClickListener(v -> loadRoomUserInfoList());
    }

    @Override
    protected void iniData() {
        initSyncManager();

        initRTCManager();
        setupLocalFullVideo();
        startStreaming();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        checkIsPKing(getLocalRoomId(),
                () -> cleanCurrRoomInfo(this::finish),
                data -> showPKEndDialog());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtcManager.release();
    }

    //============================UI Logic===============================================


    private void showOnLineUserListDialog(List<UserInfo> userInfoList) {
        if (userInfoList == null || userInfoList.size() <= 0) {
            return;
        }
        RelativeLayout dialogView = new RelativeLayout(this);
        LayoutInflater.from(this).inflate(R.layout.action_room_all_user_list, dialogView, true);
        RecyclerView recyclerView = dialogView.findViewById(R.id.live_room_action_sheet_all_user_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false));
        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivUserIcon;
            final TextView tvUserName;
            final TextView tvInvite;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserIcon = itemView.findViewById(R.id.live_room_action_sheet_online_user_item_icon);
                tvUserName = itemView.findViewById(R.id.live_room_action_sheet_online_user_item_name);
                tvInvite = itemView.findViewById(R.id.live_room_action_sheet_online_user_item_status);
            }

        }

        recyclerView.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.action_room_online_user_invite_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                UserInfo info = userInfoList.get(position);

                Glide.with(holder.itemView)
                        .load(UserUtil.getUserProfileIcon(info.userId))
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(holder.ivUserIcon);

                holder.tvUserName.setText(info.userName);
                holder.tvInvite.setActivated(true);
                holder.tvInvite.setOnClickListener(v -> {
                    // 开始PK
                    startPKWith(info.userId);
                });
            }

            @Override
            public int getItemCount() {
                return userInfoList.size();
            }
        });
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.live_room_dialog);
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

    private void showPKEndDialog() {
        new AlertDialog.Builder(HostPKActivity.this)
                .setTitle("PK")
                .setMessage("Currently in PK, whether to stop PK?")
                .setPositiveButton(R.string.cmm_ok, (dialog, which) -> {
                    dialog.dismiss();
                    stopPK();
                })
                .setNegativeButton(R.string.cmm_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void setupLocalFullVideo() {
        mDataBinding.flLocalFullContainer.setVisibility(View.VISIBLE);

        // remove old video view
        mDataBinding.flLocalFullContainer.removeAllViews();
        mDataBinding.ivLoadingBg.setVisibility(View.GONE);
        rtcManager.renderLocalVideo(mDataBinding.flLocalFullContainer, () -> mDataBinding.ivLoadingBg.setVisibility(View.GONE));
    }

    private void setupRemoteVideo(int uid) {
        runOnUiThread(() -> {
            mDataBinding.remoteCallLayout.setVisibility(View.VISIBLE);
            rtcManager.renderRemoteVideo(mDataBinding.remoteCallVideoLayout, uid, () -> runOnUiThread(() -> {

            }));
        });

    }


    private String getLocalRoomId() {
        return mRoomInfo.roomId;
    }

    private void resetRemoteViewLayout(String remoteUserName){
        runOnUiThread(() -> {
            mDataBinding.remoteCallPeerName.setText(remoteUserName);
            mDataBinding.remoteCallCloseBtn.setOnClickListener(v -> stopPK());
        });
    }

    //==========================RTCManager Logic===========================================

    private void initRTCManager() {
        rtcManager.init(this, getAgoraAppId(), new RtcManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {
                if (!TextUtils.isEmpty(message)) {
                    runOnUiThread(() -> {
                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }

            @Override
            public void onSuccess() {

            }

            @Override
            public void onUserJoined(int uid) {
                setupRemoteVideo(uid);
            }
        });
    }

    private void startStreaming() {
        int mode = mRoomInfo.mode;
        if (mode == RoomInfo.PUSH_MODE_DIRECT_CDN) {
            startStreamingByCDN();
        } else {
            startStreamingByRtc();
        }
    }

    private void startStreamingByCDN() {
        rtcManager.stopRtcStreaming();
        rtcManager.startDirectCDNStreaming(getLocalRoomId());
    }

    private void startStreamingByRtc() {
        rtcManager.startRtcStreaming(getLocalRoomId(), true);
    }

    private void startRTCPK() {
        if(mRoomInfo.mode == RoomInfo.PUSH_MODE_DIRECT_CDN){
            rtcManager.stopDirectCDNStreaming(this::startStreamingByRtc);
        }
    }

    private void stopRTCPK() {
        if(mRoomInfo.mode == RoomInfo.PUSH_MODE_DIRECT_CDN){
            startStreamingByCDN();
        }
    }

    private String getAgoraAppId() {
        String appId = getString(R.string.agora_app_id);
        if (TextUtils.isEmpty(appId)) {
            throw new RuntimeException("the app id is empty");
        }
        return appId;
    }


    //=========================SyncManager Logic===========================================


    private void initSyncManager() {
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_ROOM_INFO)
                .document(getLocalRoomId())
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
                        Toast.makeText(HostPKActivity.this, "initSyncManager subscribe error: " + ex.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_USER_INFO)
                .subcribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(IObject item) {
                        UserInfo info = item.toObject(UserInfo.class);
                        if (info.roomId.equals(mRoomInfo.roomId)) {
                            // 用户进来
                            RoomInfo roomInfo = new RoomInfo(mRoomInfo);
                            roomInfo.userCount++;
                            SyncManager.Instance()
                                    .getScene(SYNC_SCENE_ID)
                                    .collection(SYNC_COLLECTION_ROOM_INFO)
                                    .document(getLocalRoomId())
                                    .update(roomInfo.toMap(), null);
                        }
                    }

                    @Override
                    public void onUpdated(IObject item) {

                    }

                    @Override
                    public void onDeleted(IObject item) {
                        UserInfo info = item.toObject(UserInfo.class);
                        if (info.roomId.equals(mRoomInfo.roomId)) {
                            // 用户离开
                            RoomInfo roomInfo = new RoomInfo(mRoomInfo);
                            roomInfo.userCount--;
                            SyncManager.Instance()
                                    .getScene(SYNC_SCENE_ID)
                                    .collection(SYNC_COLLECTION_ROOM_INFO)
                                    .document(getLocalRoomId())
                                    .update(roomInfo.toMap(), null);
                        }
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
                startRTCPK();
                getUserInfoById(roomInfo.userIdPK, data -> {
                    resetRemoteViewLayout(data.userName);
                });
            } else {
                // 停止PK
                stopRTCPK();
            }
        }

        int newUserCount = roomInfo.userCount;
        int oldUserCount = oldRoomInfo.userCount;
        if (newUserCount != oldUserCount) {
            // 更新房间内人数
            runOnUiThread(HostPKActivity.this::updateRoomUserCountTv);
        }
    }

    private void loadRoomUserInfoList() {
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_USER_INFO)
                .get(new SyncManager.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        List<UserInfo> userInfos = new ArrayList<>();
                        for (IObject iObject : result) {
                            UserInfo item = iObject.toObject(UserInfo.class);
                            if (item.roomId.equals(mRoomInfo.roomId)) {
                                userInfos.add(item);
                            }
                        }
                        // 显示在线用户弹窗
                        runOnUiThread(() -> showOnLineUserListDialog(userInfos));

                    }

                    @Override
                    public void onFail(SyncManagerException ex) {
                        Toast.makeText(HostPKActivity.this, "loadRoomUserInfoList get error: " + ex.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void startPKWith(String pkUserId) {
        checkIsPKing(pkUserId,
                () -> {
                    RoomInfo newRoomInfo = new RoomInfo(mRoomInfo);
                    newRoomInfo.userIdPK = pkUserId;
                    SyncManager.Instance()
                            .getScene(SYNC_SCENE_ID)
                            .collection(SYNC_COLLECTION_ROOM_INFO)
                            .document(getLocalRoomId())
                            .update(newRoomInfo.toMap(), new SyncManager.DataItemCallback() {
                                @Override
                                public void onSuccess(IObject result) {

                                }

                                @Override
                                public void onFail(SyncManagerException exception) {

                                }
                            });

                },
                data -> Toast.makeText(HostPKActivity.this, "The host " + pkUserId + " is kping with " + data, Toast.LENGTH_LONG).show());
    }

    private void stopPK() {
        RoomInfo newRoomInfo = new RoomInfo(mRoomInfo);
        newRoomInfo.userIdPK = "";
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_ROOM_INFO)
                .document(getLocalRoomId())
                .update(newRoomInfo.toMap(), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(IObject result) {

                    }

                    @Override
                    public void onFail(SyncManagerException exception) {

                    }
                });
    }


    private void checkIsPKing(String channelId, Runnable idle, DataCallback<String> pking) {
        if (channelId.equals(mRoomInfo.roomId)) {
            if (!TextUtils.isEmpty(mRoomInfo.userIdPK)) {
                if (pking != null) {
                    pking.onSuccess(mRoomInfo.userIdPK);
                }
            } else {
                if (idle != null) {
                    idle.run();
                }
            }
        } else {
            SyncManager.Instance()
                    .getScene(SYNC_SCENE_ID)
                    .collection(SYNC_COLLECTION_ROOM_INFO)
                    .get(new SyncManager.DataListCallback() {
                        @Override
                        public void onSuccess(List<IObject> result) {
                            RoomInfo pkItem = null;
                            for (IObject iObject : result) {
                                RoomInfo item = iObject.toObject(RoomInfo.class);
                                if (channelId.equals(item.userIdPK)) {
                                    pkItem = item;
                                    break;
                                }
                            }
                            if (pkItem != null) {
                                if (pking != null) {
                                    pking.onSuccess(pkItem.roomId);
                                }
                            } else {
                                if (idle != null) {
                                    idle.run();
                                }
                            }
                        }

                        @Override
                        public void onFail(SyncManagerException exception) {
                            Toast.makeText(HostPKActivity.this, exception.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void cleanCurrRoomInfo(Runnable success) {
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_ROOM_INFO)
                .document(getLocalRoomId())
                .delete(new SyncManager.Callback() {
                    @Override
                    public void onSuccess() {
                        if (success != null) {
                            success.run();
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {
                        Toast.makeText(HostPKActivity.this, "deleteRoomInfo failed exception: " + exception.toString(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void getUserInfoById(String userId, DataCallback<UserInfo> callback) {
        SyncManager.Instance()
                .getScene(SYNC_SCENE_ID)
                .collection(SYNC_COLLECTION_USER_INFO)
                .get(new SyncManager.DataListCallback() {
                    @Override
                    public void onSuccess(List<IObject> result) {
                        for (IObject iObject : result) {
                            UserInfo info = iObject.toObject(UserInfo.class);
                            if (info.userId.equals(userId)) {
                                if (callback != null) {
                                    callback.onSuccess(info);
                                }
                                break;
                            }
                        }
                    }

                    @Override
                    public void onFail(SyncManagerException exception) {

                    }
                });
    }

}
