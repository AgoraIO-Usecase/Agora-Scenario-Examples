package io.agora.scene.breakoutroom;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.agora.example.base.TokenGenerator;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.LeaveChannelOptions;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.breakoutroom.databinding.BreakoutRoomLiveDetailActivityBinding;
import io.agora.scene.breakoutroom.databinding.BreakoutRoomLiveVideoItemBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class LiveDetailActivity extends AppCompatActivity {
    private final RoomManager roomManager = RoomManager.getInstance();
    private RtcEngine rtcEngine;
    private final int localUid = Integer.parseInt(RoomManager.getCacheUserId());

    private BreakoutRoomLiveDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private final List<BreakoutRoomLiveVideoItemBinding> remoteVideoItemBindings = new ArrayList<>();

    private RoomManager.DataCallback<RoomManager.SubRoomInfo> subRoomAddOrUpdate = data -> {

        int tabCount = mBinding.tabLayoutFgRoom.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            TabLayout.Tab tab = mBinding.tabLayoutFgRoom.getTabAt(i);
            if(tab.getText().toString().equals(data.subRoom)){
                return;
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
                tab.setText(data.subRoom);
                mBinding.tabLayoutFgRoom.addTab(tab);
            }
        });
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = BreakoutRoomLiveDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        mBinding.ivClose.setOnClickListener(v -> finish());

        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        tab.setText(roomInfo.id);
        mBinding.tabLayoutFgRoom.addTab(tab);
        mBinding.tabLayoutFgRoom.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String roomId = tab.getText().toString();
                LeaveChannelOptions options = new LeaveChannelOptions();
                options.stopMicrophoneRecording = false;
                rtcEngine.leaveChannel(options);

                Iterator<BreakoutRoomLiveVideoItemBinding> iterator = remoteVideoItemBindings.iterator();
                while (iterator.hasNext()){
                    BreakoutRoomLiveVideoItemBinding binding = iterator.next();
                    mBinding.dynamicViewFgRoom.dynamicRemoveView(binding.getRoot());
                    iterator.remove();
                }

                joinChannel(roomId);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mBinding.fabFgRoom.setOnClickListener(v -> showSubRoomCreateDialog());
        mBinding.checkboxMicFgRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rtcEngine.muteLocalAudioStream(!isChecked);
        });

        initRoomManager();
        initRtcEngine();

        renderLocalPreview();
        joinChannel(roomInfo.id);
    }

    private void showSubRoomCreateDialog() {
        EditText etChannelName = new EditText(this);
        etChannelName.setHint(R.string.preview_control_name_enter_hint);
        new AlertDialog.Builder(this)
                .setTitle(R.string.breakout_room_create_subroom)
                .setView(etChannelName)
                .setPositiveButton(R.string.common_create, (dialog, which) -> {
                    String channelName = etChannelName.getText().toString();
                    if (TextUtils.isEmpty(channelName)) {
                        Toast.makeText(LiveDetailActivity.this, R.string.breakout_room_name_empty_tip, Toast.LENGTH_SHORT).show();
                    } else {
                        roomManager.createSubRoom(roomInfo.id, channelName);
                    }
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.id, () -> {
            roomManager.subscribeSubRoomEvent(roomInfo.id, subRoomAddOrUpdate);
            roomManager.getAllSubRooms(roomInfo.id, dataList -> {
                runOnUiThread(() -> {
                    for (RoomManager.SubRoomInfo subRoomInfo : dataList) {
                        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
                        tab.setText(subRoomInfo.subRoom);
                        mBinding.tabLayoutFgRoom.addTab(tab);
                    }
                });
            });
        });
    }

    private void initRtcEngine() {
        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {

                @Override
                public void onError(int err) {
                    super.onError(err);
                    runOnUiThread(() -> {
                        Toast.makeText(LiveDetailActivity.this, "code=" + err + ",message=" + RtcEngine.getErrorDescription(err), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> {
                        BreakoutRoomLiveVideoItemBinding videoItemBinding = BreakoutRoomLiveVideoItemBinding.inflate(LayoutInflater.from(LiveDetailActivity.this), mBinding.dynamicViewFgRoom, false);
                        videoItemBinding.getRoot().setTag(uid);
                        mBinding.dynamicViewFgRoom.dynamicAddView(videoItemBinding.getRoot());
                        remoteVideoItemBindings.add(videoItemBinding);

                        rtcEngine.setupRemoteVideo(new VideoCanvas(videoItemBinding.videoTextureview, Constants.RENDER_MODE_HIDDEN, uid));
                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> {
                        Iterator<BreakoutRoomLiveVideoItemBinding> iterator = remoteVideoItemBindings.iterator();
                        while (iterator.hasNext()){
                            BreakoutRoomLiveVideoItemBinding binding = iterator.next();
                            if((Integer) binding.getRoot().getTag() == uid){
                                iterator.remove();
                                mBinding.dynamicViewFgRoom.dynamicRemoveView(binding.getRoot());
                                break;
                            }
                        }
                        rtcEngine.setupRemoteVideo(new VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, uid));
                    });
                }
            });
            rtcEngine.enableVideo();

            rtcEngine.setParameters("{"
                    + "\"rtc.report_app_scenario\":"
                    + "{"
                    + "\"appScenario\":" + 100 + ","
                    + "\"serviceType\":" + 12 + ","
                    + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                    + "}"
                    + "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinChannel(String roomId) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        String channelName = roomInfo.userId + roomId;
        TokenGenerator.gen(this, roomInfo.id, localUid, ret -> rtcEngine.joinChannel(ret, channelName, localUid, options));
    }

    private void renderLocalPreview() {
        BreakoutRoomLiveVideoItemBinding videoItemBinding = BreakoutRoomLiveVideoItemBinding.inflate(LayoutInflater.from(this), mBinding.dynamicViewFgRoom, false);
        mBinding.dynamicViewFgRoom.dynamicAddView(videoItemBinding.getRoot());
        rtcEngine.setupLocalVideo(new VideoCanvas(videoItemBinding.videoTextureview, io.agora.rtc2.Constants.RENDER_MODE_HIDDEN));
        rtcEngine.startPreview();
    }

    @Override
    public void finish() {
        roomManager.leaveRoom(roomInfo.id);
        rtcEngine.stopPreview();
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }

}
