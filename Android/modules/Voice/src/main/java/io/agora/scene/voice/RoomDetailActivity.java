package io.agora.scene.voice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.scene.voice.databinding.VoiceRoomDetailActivityBinding;
import io.agora.scene.voice.databinding.VoiceRoomDetailSeatItemBinding;
import io.agora.scene.voice.utils.RoomBgUtil;
import io.agora.scene.voice.widgets.BackgroundDialog;
import io.agora.scene.voice.widgets.BgMusicDialog;
import io.agora.scene.voice.widgets.SettingDialog;
import io.agora.scene.voice.widgets.SoundEffectDialog;
import io.agora.scene.voice.widgets.VoiceBeautyDialog;
import io.agora.uiwidget.function.GiftAnimPlayDialog;
import io.agora.uiwidget.function.GiftGridDialog;
import io.agora.uiwidget.function.LiveRoomMessageListView;
import io.agora.uiwidget.function.TextInputDialog;
import io.agora.uiwidget.utils.StatusBarUtil;

public class RoomDetailActivity extends AppCompatActivity {
    private final RtcManager rtcManager = new RtcManager();
    private final RoomManager roomManager = RoomManager.getInstance();

    private RoomManager.RoomInfo roomInfo;

    private VoiceRoomDetailActivityBinding mBinding;
    private VoiceRoomDetailSeatItemBinding[] seatItemBindings = new VoiceRoomDetailSeatItemBinding[8];

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
                new GiftAnimPlayDialog(RoomDetailActivity.this)
                        .setAnimRes(data.getGifId())
                        .show();
            });
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = VoiceRoomDetailActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        roomInfo = (RoomManager.RoomInfo) getIntent().getSerializableExtra("roomInfo");

        mBinding.userView.randomUser(8, 3);

        // 房间信息
        mBinding.hostNameView.setName(roomInfo.roomName);
        mBinding.hostNameView.setIcon(roomInfo.getAndroidBgId());

        // 底部按钮栏
        mBinding.bottomView.setFun1Visible(true)
                .setFun1ImageResource(R.drawable.voice_room_detail_ic_sound_effect)
                .setFun1ClickListener(v -> {
                    // 音效
                    SoundEffectDialog.showDialog(RoomDetailActivity.this);
                });
        mBinding.bottomView.setFun2Visible(true)
                .setFun2ImageResource(R.drawable.voice_room_detail_ic_voice_beauty)
                .setFun2ClickListener(v -> {
                    // 美声
                    VoiceBeautyDialog.showDialog(RoomDetailActivity.this);
                });
        mBinding.bottomView.setFun3Visible(true)
                .setFun3ImageResource(R.drawable.live_bottom_btn_gift)
                .setFun3ClickListener(v -> {
                    // 礼物
                    new GiftGridDialog(RoomDetailActivity.this)
                            .resetGiftList(GiftGridDialog.DEFAULT_GIFT_LIST)
                            .show();
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

        // 座位列表
        seatItemBindings[0] = mBinding.seat01;
        seatItemBindings[1] = mBinding.seat02;
        seatItemBindings[2] = mBinding.seat03;
        seatItemBindings[3] = mBinding.seat04;
        seatItemBindings[4] = mBinding.seat05;
        seatItemBindings[5] = mBinding.seat06;
        seatItemBindings[6] = mBinding.seat07;
        seatItemBindings[7] = mBinding.seat08;

        // 显示背景
        mBinding.ivBackground.setImageResource(RoomBgUtil.getRoomBgPicRes(0));

        initRoomManager();
        initRtcManager();
    }

    private void showTextInputDialog() {
        new TextInputDialog(this)
                .setOnSendClickListener((v, text) -> {
                    mMessageAdapter.addMessage(new RoomManager.MessageInfo(RoomManager.getCacheUserId(), text));
                })
                .show();
    }

    private void initRoomManager() {
        roomManager.joinRoom(roomInfo.roomId, roomId -> roomManager.subscribeGiftReceiveEvent(roomInfo.roomId, giftInfoDataCallback));
    }

    private void initRtcManager() {
        rtcManager.init(this, getString(R.string.rtc_app_id), null);
        rtcManager.joinChannel(roomInfo.roomId, roomInfo.userId, getString(R.string.rtc_app_token), true, new RtcManager.OnChannelListener() {
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

    private void showSettingDialog() {
        SettingDialog.showDialog(this, (view, item) -> {
            if(SettingDialog.ITEM_BACKGROUND_MUSIC.equals(item)){
                BgMusicDialog.showDialog(this, new BgMusicDialog.Listener() {
                    @Override
                    public void onVolumeChanged(int max, int volume) {

                    }

                    @Override
                    public void onMusicSelected(BgMusicDialog.MusicInfo musicInfo, boolean isSelected) {

                    }
                });
            }else if(SettingDialog.ITEM_BACKGROUND.equals(item)){
                BackgroundDialog dialog = new BackgroundDialog(this);
                dialog.setOnBackgroundActionListener((index, res) -> {

                });
                dialog.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
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

    @Override
    public void finish() {
        roomManager.leaveRoom(roomInfo.roomId, true);
        rtcManager.release();
        super.finish();
    }

}
