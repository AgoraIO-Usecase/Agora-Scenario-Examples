package io.agora.scene.interactiveblog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.agora.example.base.TokenGenerator;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogDialogBroadcastMenuBinding;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogDialogHandUpBinding;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogDialogHandUpItemBinding;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogDialogUserInviteBinding;
import io.agora.scene.interactiveblog.databinding.InteractiveBlogDialogUserInvitedBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.utils.RandomUtil;

public class ChatManager {

    private volatile static ChatManager INSTANCE;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final RoomManager roomManager = RoomManager.getInstance();
    private OnEventListener onEventListener;
    private RoomManager.RoomInfo roomInfo;
    private RtcEngine rtcEngine;
    private volatile boolean roomJoined = false;

    public static ChatManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ChatManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ChatManager();
                }
            }
        }
        return INSTANCE;
    }

    private ChatManager() {}

    public void setOnEventListener(OnEventListener eventListener) {
        if (eventListener == null) {
            return;
        }
        onEventListener = eventListener;
    }

    public void removeOnEventListener(OnEventListener eventListener) {
        if (onEventListener == eventListener) {
            onEventListener = null;
        }
    }

    public void tryToFlushUserList() {
        fetchAllUsers();
    }

    public void joinRoom(Context context, RoomManager.RoomInfo roomInfo) {
        if (this.roomInfo != null
                && !this.roomInfo.roomId.equals(roomInfo.roomId)) {
            leaveRoom();
        }
        if (this.roomInfo == null) {
            this.roomInfo = roomInfo;
            initRoomManager();
            initRtcEngine(context);
        }
    }

    public void leaveRoom() {
        if (this.roomInfo == null) {
            return;
        }
        unInitRoomManager();
        unInitRtcEngine();
        roomInfo = null;
    }

    public void handUp() {
        if (isRoomJoined()) {
            roomManager.requestUser(roomInfo.roomId, roomManager.getLocalUserInfo());
            showToast(R.string.interactive_blog_request_handup_success);
        }
    }

    public void showHandUpListDialog() {
        if (!isRoomJoined() || onEventListener == null) {
            return;
        }
        roomManager.getUserList(roomInfo.roomId, dataList -> {
            List<RoomManager.UserInfo> handUpUsers = new ArrayList<>();
            for (RoomManager.UserInfo userInfo : dataList) {
                if (userInfo.status == RoomManager.Status.REQUEST) {
                    handUpUsers.add(userInfo);
                }
            }
            runOnUiThread(() -> {
                showHandUpUserDialogInner(handUpUsers);
            });
        });
    }

    public void showUserInviteDialog(RoomManager.UserInfo userInfo) {
        if (!isRoomJoined() || onEventListener == null) {
            return;
        }
        if (!localIsRoomOwner()) {
            return;
        }
        Context context = onEventListener.getContext();
        if (context == null) {
            return;
        }
        BottomSheetDialog userInviteDialog = new BottomSheetDialog(context, R.style.BottomSheetDialog);
        userInviteDialog.setCanceledOnTouchOutside(true);

        InteractiveBlogDialogUserInviteBinding userInviteViewBinding = InteractiveBlogDialogUserInviteBinding.inflate(LayoutInflater.from(context));
        userInviteViewBinding.ivUser.setImageResource(RandomUtil.getIconById(userInfo.userId));
        userInviteViewBinding.tvName.setText(userInfo.userName);
        userInviteViewBinding.btFuntion.setOnClickListener(v1 -> {
            roomManager.inviteUser(roomInfo.roomId, userInfo);
            userInviteDialog.dismiss();
        });

        userInviteDialog.setContentView(userInviteViewBinding.getRoot());
        userInviteDialog.show();
    }

    public void showBroadcastMenuDialog(RoomManager.UserInfo userInfo) {
        if (!isRoomJoined() || onEventListener == null) {
            return;
        }
        if (userInfo.userId.equals(roomInfo.userId)) {
            return;
        }
        if (!localIsRoomOwner() && !userInfo.userId.equals(getLocalUserInfo().userId)) {
            return;
        }
        Context context = onEventListener.getContext();
        if (context == null) {
            return;
        }
        BottomSheetDialog broadcastMenuDialog = new BottomSheetDialog(context, R.style.BottomSheetDialog);
        broadcastMenuDialog.setCanceledOnTouchOutside(true);

        InteractiveBlogDialogBroadcastMenuBinding broadcastMenuViewBinding = InteractiveBlogDialogBroadcastMenuBinding.inflate(LayoutInflater.from(context));
        broadcastMenuViewBinding.ivUser.setImageResource(RandomUtil.getIconById(userInfo.userId));
        broadcastMenuViewBinding.tvName.setText(userInfo.userName);
        broadcastMenuViewBinding.ivAudio.setActivated(userInfo.isEnableAudio);
        broadcastMenuViewBinding.btSeatoff.setOnClickListener(v -> {
            roomManager.endUser(roomInfo.roomId, userInfo);
            broadcastMenuDialog.dismiss();
        });
        broadcastMenuViewBinding.btAudio.setText(userInfo.isEnableAudio ? R.string.interactive_blog_room_dialog_close_audio : R.string.interactive_blog_room_dialog_open_audio);
        broadcastMenuViewBinding.btAudio.setOnClickListener(v -> {
            roomManager.enableAudio(roomInfo.roomId, userInfo, !userInfo.isEnableAudio);
            broadcastMenuDialog.dismiss();
        });

        broadcastMenuDialog.setContentView(broadcastMenuViewBinding.getRoot());
        broadcastMenuDialog.show();
    }

    public RoomManager.UserInfo getLocalUserInfo() {
        if (isRoomJoined()) {
            return roomManager.getLocalUserInfo();
        }
        return null;
    }

    public boolean isRoomJoined() {
        return roomInfo != null && roomJoined;
    }

    public RoomManager.RoomInfo getRoomInfo() {
        return roomInfo;
    }

    public boolean localIsRoomOwner() {
        return roomInfo != null && roomInfo.userId.equals(RoomManager.getCacheUserId());
    }

    public void enableLocalAudio(boolean enable) {
        if (isRoomJoined()) {
            roomManager.enableAudio(roomInfo.roomId, getLocalUserInfo(), enable);
        }
    }


    // >>>>>>>>>>>>>>>> RoomManger >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, data -> {
            roomJoined = true;
            roomManager.subscribeRoomDeleteEvent(roomInfo.roomId,
                    roomId -> {
                        leaveRoom();
                        runOnUiThread(() -> {
                            if (onEventListener != null) {
                                onEventListener.onRoomDestroyed();
                            }
                        });
                    });
            roomManager.subscribeUserChangeEvent(roomInfo.roomId,
                    addOrUpdateUser -> {
                        if (addOrUpdateUser.userId.equals(roomManager.getLocalUserInfo().userId)) {
                            Log.d("LocalUserInfo", "ChatManager subscribeUserChangeEvent userInfo=" + addOrUpdateUser + ", onEventListener=" + onEventListener);
                            if (onEventListener != null) {
                                runOnUiThread(() -> {
                                    if (onEventListener != null) {
                                        // INVITING deal
                                        if (addOrUpdateUser.status == RoomManager.Status.INVITING) {
                                            showUserInvitedDialogInner();
                                        }

                                        publishAudio(addOrUpdateUser.status == RoomManager.Status.ACCEPT && addOrUpdateUser.isEnableAudio);
                                        onEventListener.onLocalUserChanged(addOrUpdateUser);
                                    }
                                });
                            }
                        } else if (localIsRoomOwner()) {
                            if (onEventListener != null) {
                                runOnUiThread(() -> {
                                    if (onEventListener != null) {
                                        // REFUSE/ACCEPT deal
                                        // if (addOrUpdateUser.status == RoomManager.Status.REFUSE) {
                                        //     showToast(R.string.interactive_blog_invite_refuse, addOrUpdateUser.userName);
                                        // } else if (addOrUpdateUser.status == RoomManager.Status.ACCEPT) {
                                        //     showToast(R.string.interactive_blog_invite_agree, addOrUpdateUser.userName);
                                        // }
                                    }
                                });
                            }
                        }
                        fetchAllUsers();
                    },
                    deleteUser -> {
                        fetchAllUsers();
                    });
            if (localIsRoomOwner()) {
                roomManager.addUserInfo(roomInfo.roomId, RoomManager.Status.ACCEPT, roomManager.getLocalUserInfo());
            } else {
                roomManager.addUserInfo(roomInfo.roomId, RoomManager.Status.END, roomManager.getLocalUserInfo());
            }
        }, data -> {
            this.roomInfo = null;
        });
    }

    private void fetchAllUsers() {
        if (!isRoomJoined()) {
            return;
        }
        roomManager.getUserList(roomInfo.roomId, dataList -> {
            List<RoomManager.UserInfo> audiences = new ArrayList<>();
            List<RoomManager.UserInfo> audiencesHandUp = new ArrayList<>();
            List<RoomManager.UserInfo> broadcasts = new ArrayList<>();

            Collections.sort(dataList, (o1, o2) -> {
                if (o2.userId.equals(roomInfo.userId)) {
                    return -1;
                }
                return Integer.parseInt(o2.userId) - Integer.parseInt(o1.userId);
            });
            for (RoomManager.UserInfo userInfo : dataList) {
                if (userInfo.status == RoomManager.Status.ACCEPT) {
                    broadcasts.add(userInfo);
                } else {
                    audiences.add(userInfo);
                }
                if (userInfo.status == RoomManager.Status.REQUEST) {
                    audiencesHandUp.add(userInfo);
                }
            }
            if (onEventListener != null) {
                runOnUiThread(() -> {
                    if (onEventListener != null) {
                        onEventListener.onBroadcastsChanged(broadcasts);
                        onEventListener.onAudiencesChanged(audiences);
                        onEventListener.onAudienceHandUpChanged(audiencesHandUp);
                    }
                });
            }
        });
    }

    private void unInitRoomManager() {
        RoomManager.getInstance().deleteUserInfo(roomInfo.roomId, roomManager.getLocalUserInfo());
        if (RoomManager.getCacheUserId().equals(roomInfo.userId)) {
            RoomManager.getInstance().destroyRoom(roomInfo.roomId);
        } else {
            RoomManager.getInstance().leaveRoom(roomInfo.roomId);
        }
        roomInfo = null;
        roomJoined = false;
    }

    // >>>>>>>>>>>>>>>> RoomManger END >>>>>>>>>>>>>>>>>>>>>>>>>

    // >>>>>>>>>>>>>>>> RTCEngine BEGIN >>>>>>>>>>>>>>>>>>>>>>>>>

    private void initRtcEngine(Context context) {
        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = context;
        config.mAppId = context.getString(R.string.rtc_app_id);
        config.mEventHandler = new IRtcEngineEventHandler() {
        };
        try {
            rtcEngine = RtcEngine.create(config);
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();
            rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + BuildConfig.RTCAppScenario + ","
                    + "\"serviceType\":" + BuildConfig.RTCServiceType + ","
                    + "\"appVersion\":\"" + BuildConfig.RTCAppVersion + "\""
                    + "}"
                    + "}");

            // join channel
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(context, roomInfo.roomId, uid, token -> {
                ChannelMediaOptions options = new ChannelMediaOptions();
                boolean isPublish = getLocalUserInfo().status == RoomManager.Status.ACCEPT && getLocalUserInfo().isEnableAudio;
                options.clientRoleType = isPublish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
                options.autoSubscribeAudio = true;
                options.publishMicrophoneTrack = isPublish;
                rtcEngine.joinChannel(token, roomInfo.roomId, uid, options);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void publishAudio(boolean publish) {
        if(rtcEngine == null){
            return;
        }
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = publish ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        options.autoSubscribeAudio = true;
        options.publishMicrophoneTrack = publish;
        rtcEngine.updateChannelMediaOptions(options);
    }

    private void unInitRtcEngine() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    // >>>>>>>>>>>>>>>> RTCEngine END >>>>>>>>>>>>>>>>>>>>>>>>>

    private void runOnUiThread(@NonNull Runnable runnable) {
        if (Thread.currentThread() == mainHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    private void showHandUpUserDialogInner(List<RoomManager.UserInfo> handUpUsers) {
        if (onEventListener == null) {
            return;
        }
        Context context = onEventListener.getContext();
        if (context == null) {
            return;
        }
        BottomSheetDialog handUpDialog = new BottomSheetDialog(context, R.style.BottomSheetDialog);
        handUpDialog.setCanceledOnTouchOutside(true);

        InteractiveBlogDialogHandUpBinding handUpViewBinding = InteractiveBlogDialogHandUpBinding.inflate(LayoutInflater.from(context));
        BindingSingleAdapter<RoomManager.UserInfo, InteractiveBlogDialogHandUpItemBinding> adapter = new BindingSingleAdapter<RoomManager.UserInfo, InteractiveBlogDialogHandUpItemBinding>() {
            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<InteractiveBlogDialogHandUpItemBinding> holder, int position) {
                RoomManager.UserInfo userInfo = getItem(position);
                holder.binding.ivUser.setImageResource(RandomUtil.getIconById(userInfo.userId));
                holder.binding.tvName.setText(userInfo.userName);
                holder.binding.btAgree.setOnClickListener(v -> {
                    roomManager.acceptUser(roomInfo.roomId, userInfo);
                    handUpDialog.dismiss();
                });
                holder.binding.btRefuse.setOnClickListener(v -> {
                    roomManager.refuseUser(roomInfo.roomId, userInfo);
                    handUpDialog.dismiss();
                });
            }
        };
        handUpViewBinding.rvList.setAdapter(adapter);
        adapter.insertAll(handUpUsers);

        handUpDialog.setContentView(handUpViewBinding.getRoot());
        handUpDialog.show();
    }

    private void showUserInvitedDialogInner() {
        if (!isRoomJoined() || onEventListener == null) {
            return;
        }
        Context context = onEventListener.getContext();
        if (context == null) {
            return;
        }

        InteractiveBlogDialogUserInvitedBinding userInvitedViewBindig = InteractiveBlogDialogUserInvitedBinding.inflate(LayoutInflater.from(context));
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.BottomSheetDialog)
                .setView(userInvitedViewBindig.getRoot())
                .show();

        userInvitedViewBindig.btAgree.setOnClickListener(v -> {
            roomManager.acceptUser(roomInfo.roomId, getLocalUserInfo());
            dialog.dismiss();
        });
        userInvitedViewBindig.btRefuse.setOnClickListener(v -> {
            roomManager.refuseUser(roomInfo.roomId, getLocalUserInfo());
            dialog.dismiss();
        });
    }

    private void showToast(int strRes, Object... format) {
        if (!isRoomJoined() || onEventListener == null) {
            return;
        }
        Context context = onEventListener.getContext();
        if (context == null) {
            return;
        }
        Toast.makeText(context, context.getString(strRes, format), Toast.LENGTH_SHORT).show();
    }

    interface OnEventListener {
        void onAudiencesChanged(List<RoomManager.UserInfo> userInfoList);

        void onAudienceHandUpChanged(List<RoomManager.UserInfo> userInfoList);

        void onBroadcastsChanged(List<RoomManager.UserInfo> userInfoList);

        void onLocalUserChanged(RoomManager.UserInfo userInfo);

        void onRoomDestroyed();

        Context getContext();
    }
}
