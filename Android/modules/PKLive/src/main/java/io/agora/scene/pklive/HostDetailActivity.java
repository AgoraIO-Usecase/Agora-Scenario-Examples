package io.agora.scene.pklive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import io.agora.scene.pklive.databinding.PkLiveHostDetailActivityBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.OnlineUserListDialog;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;

public class HostDetailActivity extends AppCompatActivity {
    private final RtcManager rtcManager = new RtcManager();
    private final RoomManager roomManager = RoomManager.getInstance();

    private PkLiveHostDetailActivityBinding mBinding;
    private RoomManager.RoomInfo roomInfo;
    private RoomManager.PKApplyInfoModel pkApplyInfoModel;
    private RoomManager.PKApplyInfoModel exPkApplyInfoModel;
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;
    private final RoomManager.DataCallback<RoomManager.GiftInfo> giftInfoDataCallback = new RoomManager.DataCallback<RoomManager.GiftInfo>() {
        @Override
        public void onObtained(RoomManager.GiftInfo data) {
            runOnUiThread(() -> {
                mMessageAdapter.addMessage(new RoomManager.MessageInfo(
                        data.userId,
                        getString(R.string.live_room_message_gift_prefix),
                        data.getIconId()
                ));
                // 播放动画
                new GiftAnimPlayDialog(HostDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });
        }
    };
    private final RoomManager.DataCallback<RoomManager.PKApplyInfoModel> exPkApplyInfoModelDataCallback = data -> runOnUiThread(() -> {
        exPkApplyInfoModel = data;
        if (data.status == RoomManager.PKApplyInfoStatus.accept) {
            roomManager.startPKNow(roomInfo.roomId, data);
            resetExChannel(data, true);
        } else if (data.status == RoomManager.PKApplyInfoStatus.end) {
            roomManager.stopPKNow(roomInfo.roomId, data);
            resetExChannel(data, false);
        }
    });
    private final RoomManager.DataCallback<RoomManager.PKApplyInfoModel> pkApplyInfoModelDataCallback = data -> runOnUiThread(() -> {
        pkApplyInfoModel = data;
        if (data.status == RoomManager.PKApplyInfoStatus.invite) {
            new AlertDialog.Builder(this)
                    .setMessage("收到邀请，是否接受？")
                    .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                        roomManager.applyPKAccept(data);
                    })
                    .setNegativeButton(R.string.common_no, (dialog, which) -> roomManager.applyPKRefuse(data))
                    .show();
        }else if (data.status == RoomManager.PKApplyInfoStatus.accept) {
            roomManager.startPKNow(roomInfo.roomId, data);
            resetExChannel(data, true);
        } else if (data.status == RoomManager.PKApplyInfoStatus.end) {
            roomManager.stopPKNow(roomInfo.roomId, data);
            resetExChannel(data, false);
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = PkLiveHostDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName);
        mBinding.hostNameView.setIcon(roomInfo.getAndroidBgId());

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(false);
        mBinding.bottomView.setFun2Visible(true)
                .setFun2ImageResource(R.drawable.live_bottom_btn_pk)
                .setFun2ClickListener(v -> {
                    showHostListDialog();
                });
        mBinding.bottomView.setupInputText(true, v -> {
            // 弹出文本输入框
            showTextInputDialog();
        });
        mBinding.bottomView.setupCloseBtn(true, v -> onBackPressed());
        mBinding.bottomView.setupMoreBtn(true, v -> showSettingDialog());

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo>() {
            @Override
            protected void onItemUpdate(LiveRoomMessageListView.MessageListViewHolder holder, RoomManager.MessageInfo item, int position) {
                holder.setupMessage(item.userName, item.content, item.giftIcon);
            }
        };
        mBinding.messageList.setAdapter(mMessageAdapter);

        mBinding.btnStopPk.setOnClickListener(v -> {
            if(pkApplyInfoModel != null){
                roomManager.applyPKEnd(pkApplyInfoModel);
                pkApplyInfoModel = null;
            }
            if(exPkApplyInfoModel != null){
                roomManager.applyPKEnd(exPkApplyInfoModel);
                exPkApplyInfoModel = null;
            }
        });

        initRoomManager();
        initRtcManager();
    }

    private void showHostListDialog() {
        roomManager.getAllRooms(new RoomManager.DataListCallback<RoomManager.RoomInfo>() {
            @Override
            public void onObtained(List<RoomManager.RoomInfo> dataList) {
                if (dataList == null) {
                    return;
                }
                Iterator<RoomManager.RoomInfo> iterator = dataList.iterator();
                while (iterator.hasNext()) {
                    RoomManager.RoomInfo next = iterator.next();
                    if (next.userId.equals(RoomManager.getCacheUserId())) {
                        iterator.remove();
                    }
                }
                if (dataList.size() == 0) {
                    return;
                }
                runOnUiThread(() -> {
                    OnlineUserListDialog dialog = new OnlineUserListDialog(HostDetailActivity.this);
                    OnlineUserListDialog.AbsListItemAdapter<RoomManager.RoomInfo> adapter = new OnlineUserListDialog.AbsListItemAdapter<RoomManager.RoomInfo>() {
                        @Override
                        protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position, RoomManager.RoomInfo item) {
                            holder.binding.tvName.setText("User-" + item.userId);
                            holder.binding.ivIcon.setImageResource(RandomUtil.getIconById(item.roomId));
                            holder.binding.tvStatus.setText(R.string.online_user_list_dialog_invite);
                            holder.binding.tvStatus.setEnabled(true);
                            holder.binding.tvStatus.setActivated(true);
                            holder.binding.tvStatus.setOnClickListener(v -> {
                                roomManager.inviteToPK(roomInfo.roomId, item.roomId, item.userId, data -> {
                                    roomManager.applyPKInvite(data);
                                    roomManager.subscribePKApplyInfoEvent(data.targetRoomId, exPkApplyInfoModelDataCallback);
                                    dialog.dismiss();
                                }, data -> runOnUiThread(() -> Toast.makeText(HostDetailActivity.this, data.getMessage(), Toast.LENGTH_LONG).show()));

                            });
                        }
                    };
                    adapter.resetAll(dataList);
                    dialog.setListAdapter(adapter);
                    dialog.show();
                });
            }

        });

    }

    private void showTextInputDialog() {
        new TextInputDialog(this)
                .setOnSendClickListener((v, text) -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(RoomManager.getCacheUserId(), text)))
                .show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, () -> {
            roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, new WeakReference<>(giftInfoDataCallback));
            roomManager.subscribePKApplyInfoEvent(roomInfo.roomId, new WeakReference<>(pkApplyInfoModelDataCallback));
        });
    }

    private void initRtcManager() {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);
        rtcManager.renderLocalVideo(mBinding.localVideoContainer, null);
        rtcManager.joinChannel(roomInfo.roomId, roomInfo.userId, getString(R.string.rtc_app_token), true, new RtcManager.OnChannelListener() {
            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(HostDetailActivity.this, "code=" + code + ",message=" + message, Toast.LENGTH_LONG).show();
                    finish();
                });

            }

            @Override
            public void onJoinSuccess(int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));

            }

            @Override
            public void onUserJoined(String channelId, int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
            }

            @Override
            public void onUserOffline(String channelId, int uid) {
                runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
            }
        });
    }

    private void resetExChannel(RoomManager.PKApplyInfoModel data, boolean join) {
        String exChannelId = roomInfo.roomId.equals(data.roomId) ? data.targetRoomId : data.roomId;
        if(join){
            rtcManager.joinChannel(exChannelId, "", getString(R.string.rtc_app_token), false, new RtcManager.OnChannelListener() {
                @Override
                public void onError(int code, String message) {

                }

                @Override
                public void onJoinSuccess(int uid) {

                }

                @Override
                public void onUserJoined(String channelId, int uid) {
                    runOnUiThread(() -> {
                        mBinding.pkVideoContainer.setVisibility(View.VISIBLE);
                        mBinding.ivPkIcon.setVisibility(View.VISIBLE);
                        mBinding.btnStopPk.setVisibility(View.VISIBLE);
                        rtcManager.renderRemoteVideo(mBinding.pkVideoContainer, channelId, uid);
                    });
                }

                @Override
                public void onUserOffline(String channelId, int uid) {

                }
            });
        }else{
            mBinding.pkVideoContainer.setVisibility(View.GONE);
            mBinding.pkVideoContainer.removeAllViews();
            mBinding.ivPkIcon.setVisibility(View.GONE);
            mBinding.btnStopPk.setVisibility(View.GONE);
            rtcManager.leaveChannel(exChannelId);
            roomManager.unSubscribePKApplyInfoEvent(data.targetRoomId, exPkApplyInfoModelDataCallback);
        }

    }

    private void showSettingDialog() {
        new LiveToolsDialog(HostDetailActivity.this)
                .addToolItem(LiveToolsDialog.TOOL_ITEM_ROTATE, false, (view, item) -> rtcManager.switchCamera())
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_close_room)
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.dismiss();
                    HostDetailActivity.super.onBackPressed();
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void finish() {
        roomManager.destroyRoom(roomInfo.roomId);
        rtcManager.release();
        super.finish();
    }

}
