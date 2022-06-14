package io.agora.scene.voice;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.util.Iterator;

import io.agora.scene.voice.databinding.VoiceRoomDetailActivityBinding;
import io.agora.scene.voice.databinding.VoiceRoomDetailSeatItemBinding;
import io.agora.scene.voice.widgets.BackgroundDialog;
import io.agora.scene.voice.widgets.BgMusicDialog;
import io.agora.scene.voice.widgets.SettingDialog;
import io.agora.scene.voice.widgets.SoundEffectDialog;
import io.agora.scene.voice.widgets.UserOptionsDialog;
import io.agora.scene.voice.widgets.VoiceBeautyDialog;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.LiveToolsDialog;
import io.agora.uiwidget.function.OnlineUserListDialog;
import io.agora.uiwidget.function.TabScrollDialog;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomDetailActivity extends AppCompatActivity {
    private final RtcManager rtcManager = new RtcManager();
    private final RoomManager roomManager = RoomManager.getInstance();

    private RoomManager.RoomInfo roomInfo;

    private VoiceRoomDetailActivityBinding mBinding;
    private final VoiceRoomDetailSeatItemBinding[] seatItemBindings = new VoiceRoomDetailSeatItemBinding[8];
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;

    private final RoomManager.DataCallback<RoomManager.UserInfo> userAddOrUpdateCallback = data -> runOnUiThread(() -> updateUserView(data));
    private final RoomManager.DataCallback<RoomManager.UserInfo> userDeleteCallback = data -> runOnUiThread(() -> downSeat(data));
    private final RoomManager.DataCallback<RoomManager.RoomInfo> roomUpdateCallback = data -> runOnUiThread(() -> mBinding.ivBackground.setImageResource(data.getAndroidBgId()));
    private final RoomManager.DataCallback<String> roomDeleteCallback = data -> runOnUiThread(this::showRoomClosedDialog);

    private TabScrollDialog soundEffectDialog;
    private TabScrollDialog voiceBeautyDialog;
    private BgMusicDialog bgMusicDialog;
    private LiveToolsDialog settingDialog;
    private BackgroundDialog backgroundDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = VoiceRoomDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");
        boolean isRoomOwner = RoomManager.getCacheUserId().equals(roomInfo.userId);

        initDialogs();

        mBinding.userView.randomUser(8, 3);

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName + "(" + roomInfo.roomId + ")");
        mBinding.hostNameView.setIcon(roomInfo.getAndroidBgId());

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(isRoomOwner)
                .setFun1ImageResource(R.drawable.voice_room_detail_ic_sound_effect)
                .setFun1ClickListener(v -> {
                    // 音效
                    soundEffectDialog.show();
                });
        mBinding.bottomView.setFun2Visible(isRoomOwner)
                .setFun2ImageResource(R.drawable.voice_room_detail_ic_voice_beauty)
                .setFun2ClickListener(v -> {
                    // 美声
                    voiceBeautyDialog.show();
                });
        mBinding.bottomView.setupInputText(true, v -> {
            // 弹出文本输入框
            showTextInputDialog();
        });
        mBinding.bottomView.setupCloseBtn(true, v -> onBackPressed());
        mBinding.bottomView.setupMoreBtn(isRoomOwner, v -> settingDialog.show());

        // 消息列表
        mMessageAdapter = new LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo>() {
            @Override
            protected void onItemUpdate(LiveRoomMessageListView.MessageListViewHolder holder, RoomManager.MessageInfo item, int position) {
                holder.setupMessage(item.userName, item.content, item.giftIcon);
            }
        };
        mBinding.messageList.setAdapter(mMessageAdapter);

        // 房间创建者信息
        mBinding.ivOwnerAvatar.setImageResource(RandomUtil.getIconById(roomInfo.userId));
        mBinding.tvOwnerName.setText("User-" + roomInfo.userId);

        // 座位列表
        seatItemBindings[0] = mBinding.seat01;
        seatItemBindings[1] = mBinding.seat02;
        seatItemBindings[2] = mBinding.seat03;
        seatItemBindings[3] = mBinding.seat04;
        seatItemBindings[4] = mBinding.seat05;
        seatItemBindings[5] = mBinding.seat06;
        seatItemBindings[6] = mBinding.seat07;
        seatItemBindings[7] = mBinding.seat08;
        if (isRoomOwner) {
            for (VoiceRoomDetailSeatItemBinding seatItemBinding : seatItemBindings) {
                seatItemBinding.getRoot().setOnClickListener(v -> {
                    // 显示邀请、封麦弹窗
                    Object tag = v.getTag();
                    RoomManager.UserInfo tagUserInfo = null;

                    UserOptionsDialog optionsDialog = new UserOptionsDialog(RoomDetailActivity.this);

                    if (tag instanceof RoomManager.UserInfo) {
                        optionsDialog.addOptions(UserOptionsDialog.OPTION_MUTE);
                        tagUserInfo = (RoomManager.UserInfo) tag;
                    } else {
                        optionsDialog.addOptions(UserOptionsDialog.OPTION_INVITE);
                    }

                    final RoomManager.UserInfo _tagUserInfo = tagUserInfo;
                    optionsDialog.setOnItemSelectedListener((dialog, position, option) -> {
                        if (option == UserOptionsDialog.OPTION_INVITE) {
                            // 显示用户列表
                            showOnLineUserDialog();
                        } else if (option == UserOptionsDialog.OPTION_MUTE) {
                            roomManager.endUser(roomInfo.roomId, _tagUserInfo);
                        }
                        dialog.dismiss();
                    });
                    optionsDialog.show();
                });
            }
        }

        // 显示背景
        mBinding.ivBackground.setImageResource(roomInfo.getAndroidBgId());

        initRoomManager();
        initRtcManager(isRoomOwner);
    }

    private void initDialogs() {
        soundEffectDialog = SoundEffectDialog.createDialog(this, rtcManager);
        voiceBeautyDialog = VoiceBeautyDialog.createDialog(this, rtcManager);
        bgMusicDialog = BgMusicDialog.createDialog(this, rtcManager);
        backgroundDialog = new BackgroundDialog(this);
        backgroundDialog.setOnBackgroundActionListener((index, res) -> {
            roomInfo.setBackgroundId(res);
            mBinding.ivBackground.setImageResource(res);
            roomManager.updateRoom(roomInfo);
        });
        settingDialog = SettingDialog.createDialog(this, rtcManager, () -> backgroundDialog.show(), () -> bgMusicDialog.show());
    }


    private void showOnLineUserDialog() {
        roomManager.getUserInfoList(roomInfo.roomId, dataList -> {
            Iterator<RoomManager.UserInfo> iterator = dataList.iterator();
            while (iterator.hasNext()) {
                RoomManager.UserInfo next = iterator.next();
                if (next.userId.equals(roomInfo.userId) || next.status == RoomManager.UserStatus.invite || next.status == RoomManager.UserStatus.accept) {
                    iterator.remove();
                }
            }
            if (dataList.size() == 0) {
                return;
            }

            runOnUiThread(() -> {
                OnlineUserListDialog dialog = new OnlineUserListDialog(RoomDetailActivity.this);
                OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo> adapter = new OnlineUserListDialog.AbsListItemAdapter<RoomManager.UserInfo>() {
                    @Override
                    protected void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position, RoomManager.UserInfo item) {
                        holder.binding.tvName.setText(item.userName);
                        holder.binding.tvStatus.setText(R.string.online_user_list_dialog_invite);
                        holder.binding.tvStatus.setActivated(true);
                        holder.binding.tvStatus.setEnabled(true);

                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(),
                                BitmapFactory.decodeResource(getResources(), item.getAvatarImgResId()));
                        drawable.setCircular(true);
                        holder.binding.ivIcon.setImageDrawable(drawable);

                        holder.binding.tvStatus.setOnClickListener(v -> {
                            // 发起邀请
                            roomManager.inviteUser(roomInfo.roomId, item);
                            dialog.dismiss();
                        });
                    }
                };
                adapter.resetAll(dataList);
                dialog.setListAdapter(adapter);
                dialog.show();
            });
        });
    }

    private void showTextInputDialog() {
        new TextInputDialog(this)
                .setOnSendClickListener((v, text) -> {
                    mMessageAdapter.addMessage(new RoomManager.MessageInfo(RoomManager.getCacheUserId(), text));
                })
                .show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, userList -> {
            runOnUiThread(() -> {
                for (RoomManager.UserInfo userInfo : userList) {
                    updateUserView(userInfo);
                }
            });

            roomManager.subscribeUserInfoEvent(roomInfo.roomId, userAddOrUpdateCallback, userDeleteCallback);
            roomManager.subscribeRoomEvent(roomInfo.roomId, roomUpdateCallback, roomDeleteCallback);
        });
    }

    private void initRtcManager(boolean isRoomOwner) {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);
        rtcManager.joinChannel(roomInfo.roomId, roomInfo.userId, getString(R.string.rtc_app_token), isRoomOwner, new RtcManager.OnChannelListener() {
            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(RoomDetailActivity.this, "code=" + code + ",message=" + message, Toast.LENGTH_LONG).show();
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

    private void updateUserView(RoomManager.UserInfo userInfo) {
        if (RoomManager.getCacheUserId().equals(roomInfo.userId)) {
            // 房主
            // 1 -> 发出邀请
            // 2 -> 收到邀请结果
            if (userInfo.status == RoomManager.UserStatus.accept) {
                upSeat(userInfo);
            } else if (userInfo.status == RoomManager.UserStatus.refuse) {
                // 邀请被拒绝
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.voice_room_detail_invite_refused, userInfo.userName))
                        .setPositiveButton(R.string.common_confirm, (dialog, which) -> dialog.dismiss())
                        .show();
            } else if (userInfo.status == RoomManager.UserStatus.end) {
                downSeat(userInfo);
            }
        } else {
            // 房客
            // 1 -> 收到邀请，显示接受拒绝弹窗
            // 2 -> 接受/拒绝邀请
            if (userInfo.userId.equals(RoomManager.getCacheUserId())) {
                // 自己
                if (userInfo.status == RoomManager.UserStatus.invite) {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.voice_room_detail_invite_tip)
                            .setPositiveButton(R.string.voice_room_detail_invite_accept, (dialog, which) -> {
                                roomManager.acceptInvite(roomInfo.roomId, userInfo);
                            })
                            .setNegativeButton(R.string.voice_room_detail_invite_refuse, (dialog, which) -> {
                                roomManager.refuseInvite(roomInfo.roomId, userInfo);
                            })
                            .show();
                } else if (userInfo.status == RoomManager.UserStatus.accept) {
                    upSeat(userInfo);
                    rtcManager.setPublishAudio(true);
                    mBinding.bottomView.setFun1Visible(true).setFun2Visible(true);
                } else if (userInfo.status == RoomManager.UserStatus.end) {
                    downSeat(userInfo);
                    rtcManager.setPublishAudio(false);
                    mBinding.bottomView.setFun1Visible(false).setFun2Visible(false);
                }
            } else {
                // 其他人
                if (userInfo.status == RoomManager.UserStatus.accept) {
                    upSeat(userInfo);
                } else if (userInfo.status == RoomManager.UserStatus.end) {
                    downSeat(userInfo);
                }
            }

        }
    }

    private void upSeat(RoomManager.UserInfo userInfo) {
        // the user has seat or not?
        VoiceRoomDetailSeatItemBinding userSeatBinding = getUserSeat(userInfo);

        // not found sead
        if (userSeatBinding == null) {
            for (VoiceRoomDetailSeatItemBinding seatItemBinding : seatItemBindings) {
                Object tag = seatItemBinding.getRoot().getTag();
                if (tag == null) {
                    userSeatBinding = seatItemBinding;
                    break;
                }
            }
            if (userSeatBinding == null) {
                return;
            }
            userSeatBinding.getRoot().setTag(userInfo);
        }

        userSeatBinding.seatUserAvatar.setImageResource(userInfo.getAvatarImgResId());
        userSeatBinding.seatUserMute.setVisibility(userInfo.isEnableAudio ? View.GONE : View.VISIBLE);
    }

    private void downSeat(RoomManager.UserInfo userInfo) {
        // the user has seat or not?
        VoiceRoomDetailSeatItemBinding userSeatBinding = getUserSeat(userInfo);

        // not found sead
        if (userSeatBinding == null) {
            return;
        }

        userSeatBinding.getRoot().setTag(null);
        userSeatBinding.seatUserAvatar.setImageDrawable(null);
        userSeatBinding.seatUserMute.setVisibility(View.GONE);
    }

    private VoiceRoomDetailSeatItemBinding getUserSeat(RoomManager.UserInfo userInfo) {
        VoiceRoomDetailSeatItemBinding userSeatBinding = null;
        for (VoiceRoomDetailSeatItemBinding seatItemBinding : seatItemBindings) {
            Object tag = seatItemBinding.getRoot().getTag();
            if (tag instanceof RoomManager.UserInfo) {
                if (((RoomManager.UserInfo) tag).userId.equals(userInfo.userId)) {
                    userSeatBinding = seatItemBinding;
                    break;
                }
            }
        }
        return userSeatBinding;
    }

    private void showCloseRoomDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_close_room)
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.dismiss();
                    RoomDetailActivity.super.onBackPressed();
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void showRoomClosedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_tip)
                .setMessage(R.string.common_tip_room_closed)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.dismiss();
                    RoomDetailActivity.super.onBackPressed();
                })
                .show();
    }


    @Override
    public void onBackPressed() {
        if (RoomManager.getCacheUserId().equals(roomInfo.userId)) {
            showCloseRoomDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        roomManager.leaveRoom(roomInfo.roomId, RoomManager.getCacheUserId().equals(roomInfo.userId));
        rtcManager.release();
        super.finish();
    }

}
