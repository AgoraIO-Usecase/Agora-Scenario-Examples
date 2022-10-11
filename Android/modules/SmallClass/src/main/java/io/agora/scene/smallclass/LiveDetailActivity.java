package io.agora.scene.smallclass;

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
import io.agora.rtc.video.VideoCanvas;
import io.agora.scene.smallclass.databinding.SmallClassLiveDetailActivityBinding;
import io.agora.scene.smallclass.databinding.SmallClassLiveVideoItemBinding;
import io.agora.uiwidget.basic.BindingViewHolder;

public class LiveDetailActivity extends BaseActivity<SmallClassLiveDetailActivityBinding> {

    private RtcEngine rtcEngine;
    private RoomManager.RoomInfo roomInfo;
    private final RoomManager roomManager = RoomManager.getInstance();
    private ListAdapter<RoomManager.UserInfo, BindingViewHolder<SmallClassLiveVideoItemBinding>> mVideoListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        initRoomManager();
        initRtcEngine();
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
                io.agora.scene.smallclass.Constants.BOARD_APP_ID,
                io.agora.scene.smallclass.Constants.BOARD_ROOM_UUID,
                io.agora.scene.smallclass.Constants.BOARD_ROOM_TOKEN,
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
            rtcEngine.setVideoEncoderConfiguration(io.agora.scene.smallclass.Constants.encoderConfiguration);
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
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(this, roomInfo.roomId, uid, ret -> {
                rtcEngine.joinChannel(ret, roomInfo.roomId, "", uid);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.loginLocalUser(roomInfo.roomId);
            roomManager.subscribeUserChangeEvent(roomInfo.roomId, data -> updateUserListView(), data -> updateUserListView());
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

    private void updateUserListView() {
        roomManager.getRoomAllUsers(roomInfo.roomId, dataList -> runOnUiThread(() -> mVideoListAdapter.submitList(dataList)));
    }

    private void initView() {
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");

        mVideoListAdapter = new ListAdapter<RoomManager.UserInfo, BindingViewHolder<SmallClassLiveVideoItemBinding>>(new DiffUtil.ItemCallback<RoomManager.UserInfo>() {
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
            public BindingViewHolder<SmallClassLiveVideoItemBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new BindingViewHolder<>(SmallClassLiveVideoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<SmallClassLiveVideoItemBinding> holder, int position) {
                RoomManager.UserInfo item = getItem(position);
                if (item.isEnableVideo) {
                    holder.binding.ivAvatar.setVisibility(View.GONE);
                    holder.binding.ivVideoClose.setVisibility(View.GONE);
                    holder.binding.videoContainer.setVisibility(View.VISIBLE);

                    View videoView;
                    if(!(holder.binding.videoContainer.getTag() instanceof Integer
                            && (int)holder.binding.videoContainer.getTag() == Integer.parseInt(item.userId))){
                        videoView = RtcEngine.CreateTextureView(LiveDetailActivity.this);
                        holder.binding.videoContainer.removeAllViews();
                        holder.binding.videoContainer.addView(videoView);
                        holder.binding.videoContainer.setTag(Integer.parseInt(item.userId));
                    }else{
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

        mBinding.bottomView
                .setupCloseBtn(true, v -> onBackPressed())
                .setupMoreBtn(false, null)
                .setFun1Visible(false)
                .setFun2Visible(false)
                .setupInputText(false, null);


        mBinding.cbMic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            roomManager.enableLocalAudio(roomInfo.roomId, isChecked);
            rtcEngine.muteLocalAudioStream(!isChecked);
        });

        mBinding.cbVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            roomManager.enableLocalVideo(roomInfo.roomId, isChecked);
            rtcEngine.muteLocalVideoStream(!isChecked);
        });
    }

}
