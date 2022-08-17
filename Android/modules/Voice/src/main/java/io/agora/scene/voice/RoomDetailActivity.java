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

import io.agora.example.base.TokenGenerator;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;
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
    private RtcEngine rtcEngine;
    private final RoomManager roomManager = RoomManager.getInstance();

    private RoomManager.RoomInfo roomInfo;

    private VoiceRoomDetailActivityBinding mBinding;
    private final VoiceRoomDetailSeatItemBinding[] seatItemBindings = new VoiceRoomDetailSeatItemBinding[8];
    private LiveRoomMessageListView.LiveRoomMessageAdapter<RoomManager.MessageInfo> mMessageAdapter;

    private final RoomManager.DataListCallback<RoomManager.UserInfo> userInfoDataListCallback = dataList -> runOnUiThread(() -> {
        mBinding.userView.setUserCount(dataList.size());
        mBinding.userView.removeAllUserIcon();
        for (int i = 1; i <= 3; i++) {
            int index = dataList.size() - i;
            if(index >= 0){
                RoomManager.UserInfo userInfo = dataList.get(index);
                mBinding.userView.addUserIcon(userInfo.getAvatarImgResId(), null);
            }
        }
    });
    private final RoomManager.DataCallback<RoomManager.UserInfo> userAddOrUpdateCallback = data -> runOnUiThread(() -> {
        updateUserView(data);
        roomManager.getUserInfoList(roomInfo.roomId, userInfoDataListCallback);
    });
    private final RoomManager.DataCallback<String> userDeleteCallback = data -> runOnUiThread(() -> {
        downSeat(data);
        roomManager.getUserInfoList(roomInfo.roomId, userInfoDataListCallback);
    });
    private final RoomManager.DataCallback<RoomManager.RoomInfo> roomUpdateCallback = data -> runOnUiThread(() -> mBinding.ivBackground.setImageResource(data.getAndroidBgId()));
    private final RoomManager.DataCallback<String> roomDeleteCallback = data -> runOnUiThread(this::showRoomClosedDialog);
    private final RoomManager.DataCallback<RoomManager.MessageInfo> messageInfoDataCallback = data -> runOnUiThread(() -> {
        mMessageAdapter.addMessage(data);
    });

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
        mBinding.bottomView.setFun3Visible(isRoomOwner)
                .setFun3ImageResource(R.drawable.voice_room_detail_mic_icon)
                .setFun3Activated(true)
                .setFun3ClickListener(v -> {
                    // 麦克风
                    boolean isActivated = mBinding.bottomView.isFun3Activated();
                    mBinding.bottomView.setFun3Activated(!isActivated);
                    roomManager.enableLocalAudio(roomInfo.roomId, !isActivated);
                    if(rtcEngine != null){
                        rtcEngine.enableLocalAudio(!isActivated);
                    }
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
        initSeats(isRoomOwner);

        // 显示背景
        mBinding.ivBackground.setImageResource(roomInfo.getAndroidBgId());

        initRoomManager();
        initRtcManager(isRoomOwner);
    }

    private void initSeats(boolean isRoomOwner) {
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
    }

    private void initDialogs() {
        soundEffectDialog = SoundEffectDialog.createDialog(this,
                (item, position) -> {
                    rtcEngine.setAudioEffectPreset(io.agora.scene.voice.Constants.VOICE_EFFECT_ROOM_ACOUSTICS[position]);
                },
                (item, position) -> {
                    rtcEngine.setAudioEffectPreset(io.agora.scene.voice.Constants.VOICE_EFFECT_VOICE_CHANGER[position]);
                },
                (item, position) -> {
                    rtcEngine.setAudioEffectPreset(io.agora.scene.voice.Constants.VOICE_EFFECT_STYLE_TRANSFORMATION[position]);
                },
                (buttonView, isChecked) -> {
                    rtcEngine.setAudioEffectPreset(isChecked ? Constants.PITCH_CORRECTION: Constants.AUDIO_EFFECT_OFF);
                },
                (item, position) -> {
                    rtcEngine.setAudioEffectParameters(io.agora.scene.voice.Constants.VOICE_EFFECT_PITCH_CORRECTION, position,
                            io.agora.scene.voice.Constants.VOICE_EFFECT_PITCH_CORRECTION_VALUES[position]);
                },
                (item, position) -> {
                    rtcEngine.setAudioEffectParameters(io.agora.scene.voice.Constants.VOICE_EFFECT_PITCH_CORRECTION, position,
                            io.agora.scene.voice.Constants.VOICE_EFFECT_PITCH_CORRECTION_VALUES[position]);
                });

        voiceBeautyDialog = VoiceBeautyDialog.createDialog(this,
                (item, position) -> {
                    rtcEngine.setVoiceBeautifierPreset(io.agora.scene.voice.Constants.VOICE_BEAUTIFIER_CHAT[position]);
                },
                (item, position) -> {
                    boolean isWoman = position == 1;
                    if (isWoman) {
                        rtcEngine.setVoiceBeautifierParameters(io.agora.scene.voice.Constants.VOICE_BEAUTIFIER_SINGING, 2, 3);
                    } else {
                        rtcEngine.setVoiceBeautifierPreset(io.agora.scene.voice.Constants.VOICE_BEAUTIFIER_SINGING);
                    }
                },
                (item, position) -> {
                    rtcEngine.setVoiceBeautifierPreset(io.agora.scene.voice.Constants.VOICE_BEAUTIFIER_TRANASFORMATION[position]);
                });

        bgMusicDialog = BgMusicDialog.createDialog(this, new BgMusicDialog.Listener() {
            @Override
            public void onVolumeChanged(int max, int volume) {
                rtcEngine.adjustAudioMixingVolume(volume);
            }

            @Override
            public void onMusicSelected(BgMusicDialog.MusicInfo musicInfo, boolean isSelected) {
                if(isSelected){
                    rtcEngine.startAudioMixing(musicInfo.url, false, true, 1);
                }else{
                    rtcEngine.stopAudioMixing();
                }
            }
        });

        backgroundDialog = new BackgroundDialog(this);
        backgroundDialog.setOnBackgroundActionListener((index, res) -> {
            roomInfo.setBackgroundId(res);
            mBinding.ivBackground.setImageResource(res);
            roomManager.updateRoom(roomInfo);
        });

        settingDialog = SettingDialog.createDialog(this,
                (view, item) -> {
                    rtcEngine.enableInEarMonitoring(item.activated);
                },
                () -> backgroundDialog.show(),
                () -> bgMusicDialog.show());
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
                    RoomManager.MessageInfo item = new RoomManager.MessageInfo(roomManager.getLocalUserInfo().userName, text);
                    roomManager.sendMessage(roomInfo.roomId, item);
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
            roomManager.subscribeMessageEvent(roomInfo.roomId, messageInfoDataCallback);
            roomManager.getUserInfoList(roomInfo.roomId, userInfoDataListCallback);
        });
    }

    private void initRtcManager(boolean isRoomOwner) {

        try {
            rtcEngine = RtcEngine.create(this, getString(R.string.rtc_app_id), new IRtcEngineEventHandler() {
                @Override
                public void onError(int err) {
                    super.onError(err);
                    runOnUiThread(() -> {
                        Toast.makeText(RoomDetailActivity.this, "code=" + err + ",message=" + RtcEngine.getErrorDescription(err), Toast.LENGTH_LONG).show();
                        finish();
                    });
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_join_suffix))));
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUiThread(() -> mMessageAdapter.addMessage(new RoomManager.MessageInfo(uid + "", getString(R.string.live_room_message_user_left_suffix))));
                }

                @Override
                public void onClientRoleChanged(int oldRole, int newRole) {
                    super.onClientRoleChanged(oldRole, newRole);
                    if (newRole == Constants.CLIENT_ROLE_BROADCASTER) {
                        rtcEngine.enableLocalAudio(mBinding.bottomView.isFun3Activated());
                    }
                }
            });

            rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);

            rtcEngine.setClientRole(isRoomOwner ? Constants.CLIENT_ROLE_BROADCASTER: Constants.CLIENT_ROLE_AUDIENCE);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishLocalAudio = isRoomOwner;
            options.autoSubscribeAudio = true;
            int uid = Integer.parseInt(RoomManager.getCacheUserId());
            TokenGenerator.gen(RoomDetailActivity.this, roomInfo.roomId, uid, ret -> rtcEngine.joinChannel(ret, roomInfo.roomId, "", uid, options));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                downSeat(userInfo.objectId);
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

                    rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

                    mBinding.bottomView.setFun1Visible(true).setFun2Visible(true).setFun3Visible(true);
                } else if (userInfo.status == RoomManager.UserStatus.end) {
                    downSeat(userInfo.objectId);

                    rtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

                    mBinding.bottomView.setFun1Visible(false).setFun2Visible(false).setFun3Visible(false);
                }
            } else {
                // 其他人
                if (userInfo.status == RoomManager.UserStatus.accept) {
                    upSeat(userInfo);
                } else if (userInfo.status == RoomManager.UserStatus.end) {
                    downSeat(userInfo.objectId);
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

    private void downSeat(String objectId) {
        // the user has seat or not?
        VoiceRoomDetailSeatItemBinding userSeatBinding = getUserSeatByObjectId(objectId);

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

    private VoiceRoomDetailSeatItemBinding getUserSeatByObjectId(String objectId) {
        VoiceRoomDetailSeatItemBinding userSeatBinding = null;
        for (VoiceRoomDetailSeatItemBinding seatItemBinding : seatItemBindings) {
            Object tag = seatItemBinding.getRoot().getTag();
            if (tag instanceof RoomManager.UserInfo) {
                if (((RoomManager.UserInfo) tag).objectId.equals(objectId)){
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
        rtcEngine.leaveChannel();
        RtcEngine.destroy();
        super.finish();
    }

}
