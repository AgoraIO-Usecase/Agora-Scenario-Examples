package io.agora.scene.largeclass;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.agora.board.fast.FastRoom;
import io.agora.board.fast.Fastboard;
import io.agora.board.fast.model.FastRegion;
import io.agora.board.fast.model.FastRoomOptions;
import io.agora.example.base.BaseActivity;
import io.agora.example.base.TokenGenerator;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.RtcEngineConfig;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.VideoCanvas;
import io.agora.scene.largeclass.databinding.LargeClassLiveDetailActivityBinding;
import io.agora.scene.largeclass.databinding.LargeClassLiveVideoItemBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.OnlineUserListDialog;

public class LiveDetailActivity extends BaseActivity<LargeClassLiveDetailActivityBinding> {

    private RtcEngine rtcEngine;
    private RoomManager.RoomInfo roomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private ListAdapter<RoomManager.UserInfo, BindingViewHolder<LargeClassLiveVideoItemBinding>> mVideoListAdapter;
    private OnlineUserListDialog handUserListDialog;
    private OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo> handUserListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                .onGranted(data -> initRtcEngine())
                .onDenied(data -> finish())
                .start();
        initRoomManager();
        initFastBoard();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomManager.logoutLocalUser(roomInfo.roomId);
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

    private void initFastBoard() {
        Fastboard fastboard = mBinding.fastboard.getFastboard();
        FastRoomOptions roomOptions = new FastRoomOptions(
                io.agora.scene.largeclass.Constants.BOARD_APP_ID,
                io.agora.scene.largeclass.Constants.BOARD_ROOM_UUID,
                io.agora.scene.largeclass.Constants.BOARD_ROOM_TOKEN,
                RoomManager.getCacheUserId(),
                FastRegion.CN_HZ
        );

        FastRoom fastRoom = fastboard.createFastRoom(roomOptions);
        fastRoom.join();
    }

    private void initRtcEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = this;
            config.mAppId = getString(R.string.rtc_app_id);
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> Toast.makeText(LiveDetailActivity.this, "User join channel successfully. The user id is " + uid, Toast.LENGTH_SHORT).show());
                }
            };
            rtcEngine = RtcEngine.create(config);
            rtcEngine.setVideoEncoderConfiguration(io.agora.scene.largeclass.Constants.encoderConfiguration);
            rtcEngine.enableVideo();

            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + 100 + ","
                    + "\"serviceType\":" + 12 + ","
                    + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                    + "}"
                    + "}");

            // join channel
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            rtcEngine.setClientRole(isLocalTeacher()? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(this, roomInfo.roomId, uid, ret -> {
                rtcEngine.joinChannel(ret, roomInfo.roomId, "", uid, options);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.loginLocalUser(roomInfo.roomId, RoomManager.getCacheUserId().equals(roomInfo.userId), data -> {
                updateControllerView(data);
                updateUserClientRole(data);
            });
            roomManager.subscribeUserChangeEvent(roomInfo.roomId, data -> {
                showUserStatusChangeTips(data);
                updateControllerView(data);
                updateUserClientRole(data);
                updateUserListView();
            }, data -> {
                updateUserListView();
            });
            roomManager.subscribeRoomDeleteEvent(roomInfo.roomId, data -> {
                runOnUiThread(() -> new AlertDialog.Builder(LiveDetailActivity.this)
                        .setTitle(R.string.common_tip)
                        .setMessage(R.string.common_tip_room_closed)
                        .setCancelable(false)
                        .setPositiveButton(R.string.common_confirm, (dialog, which) -> finish())
                        .show());
            });
            updateUserListView();
            runOnUiThread(() -> {
                mBinding.cbMic.setChecked(roomManager.isLocalAudioEnable(roomInfo.roomId));
                mBinding.cbVideo.setChecked(roomManager.isLocalVideoEnable(roomInfo.roomId));
            });
        });
    }

    private void showUserStatusChangeTips(RoomManager.UserInfo userInfo) {
        RoomManager.UserInfo localUser = roomManager.getLocalUser(roomInfo.roomId);
        if (isLocalTeacher()) {
            if (userInfo.status == RoomManager.UserStatus.end) {
                runOnUiThread(() -> Toast.makeText(LiveDetailActivity.this, getString(R.string.large_class_end_speak, userInfo.userName), Toast.LENGTH_SHORT).show());
            }
        } else if(userInfo.userId.equals(localUser.userId)){
            if(userInfo.status == RoomManager.UserStatus.request){
                runOnUiThread(() -> Toast.makeText(LiveDetailActivity.this, getString(R.string.large_class_requesting_speak), Toast.LENGTH_SHORT).show());
            }
            else if(userInfo.status == RoomManager.UserStatus.refuse){
                runOnUiThread(() -> Toast.makeText(LiveDetailActivity.this, getString(R.string.large_class_refused_speak), Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void updateControllerView(RoomManager.UserInfo userInfo) {
        RoomManager.UserInfo localUser = roomManager.getLocalUser(roomInfo.roomId);
        if (!userInfo.userId.equals(localUser.userId)) {
            return;
        }
        runOnUiThread(() -> {
            boolean isAccept = localUser.status == RoomManager.UserStatus.accept;
            mBinding.ivRaiseHand.setActivated(isAccept);
            mBinding.cbVideo.setVisibility(isAccept ? View.VISIBLE : View.GONE);
            mBinding.cbMic.setVisibility(isAccept ? View.VISIBLE : View.GONE);
            mBinding.cbVideo.setChecked(localUser.isEnableVideo);
            mBinding.cbMic.setChecked(localUser.isEnableAudio);
        });
    }

    private void updateUserClientRole(RoomManager.UserInfo userInfo){
        RoomManager.UserInfo localUser = roomManager.getLocalUser(roomInfo.roomId);
        if (!userInfo.userId.equals(localUser.userId) || isLocalTeacher()) {
            return;
        }
        runOnUiThread(() -> {
            boolean isAccept = localUser.status == RoomManager.UserStatus.accept;
            rtcEngine.setClientRole(isAccept ? Constants.CLIENT_ROLE_BROADCASTER: Constants.CLIENT_ROLE_AUDIENCE);
        });
    }

    private void updateUserListView() {
        roomManager.getRoomAllUsers(roomInfo.roomId, dataList -> runOnUiThread(() -> {
            mBinding.userCountView.setUserCount(dataList.size());
            updateHandsTagView(new ArrayList<>(dataList));
            updateVideoListView(new ArrayList<>(dataList));
        }));
    }

    private void updateHandsTagView(List<RoomManager.UserInfo> dataList) {
        if (!isLocalTeacher()) {
            return;
        }
        Iterator<RoomManager.UserInfo> iterator = dataList.iterator();
        while (iterator.hasNext()) {
            RoomManager.UserInfo next = iterator.next();
            if (next.status != RoomManager.UserStatus.request) {
                iterator.remove();
            }
        }
        mBinding.tvHandsListTag.setVisibility(dataList.size() > 0 ? View.VISIBLE : View.GONE);
        mBinding.tvHandsListTag.setText(String.valueOf(dataList.size()));
        handUserListAdapter.resetAll(dataList);
        if (dataList.size() == 0) {
            handUserListDialog.dismiss();
        }
    }

    private void updateVideoListView(List<RoomManager.UserInfo> dataList) {
        Iterator<RoomManager.UserInfo> iterator = dataList.iterator();
        while (iterator.hasNext()) {
            RoomManager.UserInfo next = iterator.next();
            if (next.status != RoomManager.UserStatus.accept) {
                iterator.remove();
            }
        }
        Collections.sort(dataList, (o1, o2) -> {
            if (o1.userId.equals(roomInfo.userId)) {
                return -1;
            }
            return 0;
        });
        mVideoListAdapter.submitList(dataList);
    }

    private void initView() {
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");

        mVideoListAdapter = new ListAdapter<RoomManager.UserInfo, BindingViewHolder<LargeClassLiveVideoItemBinding>>(new DiffUtil.ItemCallback<RoomManager.UserInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull RoomManager.UserInfo oldItem, @NonNull RoomManager.UserInfo newItem) {
                return oldItem.userId.equals(newItem.userId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull RoomManager.UserInfo oldItem, @NonNull RoomManager.UserInfo newItem) {
                return oldItem.userId.equals(newItem.userId) && oldItem.isEnableAudio == newItem.isEnableAudio && oldItem.isEnableVideo == newItem.isEnableVideo;
            }
        }) {

            @NonNull
            @Override
            public BindingViewHolder<LargeClassLiveVideoItemBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new BindingViewHolder<>(LargeClassLiveVideoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<LargeClassLiveVideoItemBinding> holder, int position) {
                RoomManager.UserInfo item = getItem(position);
                if (item.isEnableVideo) {
                    holder.binding.ivAvatar.setVisibility(View.GONE);
                    holder.binding.ivVideoClose.setVisibility(View.GONE);
                    holder.binding.videoContainer.setVisibility(View.VISIBLE);

                    View videoView;
                    if (!(holder.binding.videoContainer.getTag() instanceof Integer
                            && (int) holder.binding.videoContainer.getTag() == Integer.parseInt(item.userId))) {
                        videoView = RtcEngine.CreateTextureView(LiveDetailActivity.this);
                        holder.binding.videoContainer.removeAllViews();
                        holder.binding.videoContainer.addView(videoView);
                        holder.binding.videoContainer.setTag(Integer.parseInt(item.userId));
                    } else {
                        videoView = holder.binding.videoContainer.getChildAt(0);
                    }
                    if (item.userId.equals(RoomManager.getCacheUserId())) {
                        rtcEngine.setupLocalVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, 0));
                    } else {
                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(item.userId)));
                    }
                } else {
                    holder.binding.ivAvatar.setVisibility(View.VISIBLE);
                    holder.binding.ivAvatar.setImageResource(item.getAvatarResId());
                    holder.binding.ivVideoClose.setVisibility(View.VISIBLE);

                    holder.binding.videoContainer.setVisibility(View.GONE);
                }

                holder.binding.ivAudioClose.setVisibility(item.isEnableAudio ? View.GONE : View.VISIBLE);
            }
        };
        mBinding.rvVideoList.setAdapter(mVideoListAdapter);
        mBinding.rvVideoList.setItemAnimator(null);

        mBinding.ivClose.setOnClickListener(v -> onBackPressed());

        mBinding.cbMic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            roomManager.enableLocalAudio(roomInfo.roomId, isChecked);
            rtcEngine.muteLocalAudioStream(!isChecked);
        });

        mBinding.cbVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            roomManager.enableLocalVideo(roomInfo.roomId, isChecked);
            rtcEngine.muteLocalVideoStream(!isChecked);
        });

        mBinding.ivHandsList.setVisibility(isLocalTeacher() ? View.VISIBLE : View.GONE);
        mBinding.ivRaiseHand.setVisibility(isLocalTeacher() ? View.GONE : View.VISIBLE);

        mBinding.ivHandsList.setOnClickListener(v -> {
            mBinding.tvHandsListTag.setVisibility(View.GONE);
            handUserListDialog.show();
        });
        mBinding.ivRaiseHand.setActivated(false);
        mBinding.ivRaiseHand.setOnClickListener(v -> roomManager.raiseHand(roomInfo.roomId, mBinding.ivRaiseHand.isActivated()));

        handUserListAdapter = new OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo>() {
            @Override
            protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder,
                                        int position,
                                        RoomManager.UserInfo item) {
                holder.binding.ivIcon.setImageResource(item.getAvatarResId());
                holder.binding.tvName.setText(item.userName);

                holder.binding.tvStatus.setText(R.string.common_accept);
                holder.binding.tvStatus.setActivated(true);
                holder.binding.tvStatus.setVisibility(View.VISIBLE);
                holder.binding.tvStatus.setOnClickListener(v -> roomManager.acceptRaiseHand(roomInfo.roomId, item));

                holder.binding.tvStatus2.setText(R.string.common_refuse);
                holder.binding.tvStatus2.setActivated(true);
                holder.binding.tvStatus2.setVisibility(View.VISIBLE);
                holder.binding.tvStatus2.setOnClickListener(v -> roomManager.refuseRaiseHand(roomInfo.roomId, item));
            }
        };
        handUserListDialog = new OnlineUserListDialog(this)
                .setListAdapter(handUserListAdapter);
    }

    private boolean isLocalTeacher() {
        return RoomManager.getCacheUserId().equals(roomInfo.userId);
    }
}
