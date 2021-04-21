package io.agora.marriageinterview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.agora.data.BaseError;
import com.agora.data.DataRepositroy;
import com.agora.data.RoomEventCallback;
import com.agora.data.manager.RoomManager;
import com.agora.data.manager.RtcManager;
import com.agora.data.manager.UserManager;
import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.observer.DataCompletableObserver;
import com.agora.data.observer.DataMaybeObserver;
import com.agora.data.observer.DataObserver;
import com.agora.data.provider.IRoomConfigProvider;

import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.adapter.RoomMembersAdapter;
import io.agora.marriageinterview.adapter.RoomMessagesAdapter;
import io.agora.marriageinterview.databinding.ActivityChatRoomBinding;
import io.agora.marriageinterview.widget.ConfirmDialog;
import io.agora.marriageinterview.widget.HandUpDialog;
import io.agora.marriageinterview.widget.InviteMenuDialog;
import io.agora.marriageinterview.widget.InvitedMenuDialog;
import io.agora.marriageinterview.widget.UserSeatMenuDialog;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 直播
 * 1. 查询Room表，房间是否存在，不存在就退出。
 * 2. 查询Member表
 * 2.1. 不存在就创建用户，并且用0加入到RTC，利用RTC分配一个唯一的uid，并且修改member的streamId值，这里注意，rtc分配的uid是int类型，需要进行（& 0xffffffffL）转换成long类型。
 * 2.2. 存在就返回member对象，利用streamId加入到RTC。
 *
 * @author chenhengfei@agora.io
 */
public class ChatRoomActivity extends DataBindBaseActivity<ActivityChatRoomBinding> implements View.OnClickListener, RoomEventCallback {

    private static final String TAG = ChatRoomActivity.class.getSimpleName();

    private static final String TAG_ROOM = "room";

    private RoomMembersAdapter mAdapterMembers;
    private RoomMessagesAdapter mAdapterMessage;

    private IRoomConfigProvider roomConfig = new IRoomConfigProvider() {

        @Override
        public void setup(RtcEngine mRtcEngine) {
            mRtcEngine.enableAudio();
            mRtcEngine.enableVideo();

            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY, Constants.AUDIO_SCENARIO_CHATROOM_ENTERTAINMENT);
            mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration());
        }

        @Override
        public boolean isNeedVideo() {
            return true;
        }

        @Override
        public boolean isNeedAudio() {
            return true;
        }
    };

    public static Intent newIntent(Context context, Room mRoom) {
        Intent intent = new Intent(context, ChatRoomActivity.class);
        intent.putExtra(TAG_ROOM, mRoom);
        return intent;
    }

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_chat_room;
    }

    @Override
    protected void iniView() {
        mAdapterMembers = new RoomMembersAdapter(null, null);
        mAdapterMessage = new RoomMessagesAdapter(null, null);
        mDataBinding.rvMembers.setAdapter(mAdapterMembers);
        mDataBinding.rvMessage.setAdapter(mAdapterMessage);
    }

    @Override
    protected void iniListener() {
        RoomManager.Instance(this).addRoomEventCallback(this);
        mDataBinding.ivBack.setOnClickListener(this);
        mDataBinding.ivMessage.setOnClickListener(this);
        mDataBinding.ivRequest.setOnClickListener(this);
        mDataBinding.ivMagic.setOnClickListener(this);
        mDataBinding.ivAudio.setOnClickListener(this);
        mDataBinding.viewUserMiddle.setOnClickListener(this);
        mDataBinding.viewUserLeft.setOnClickListener(this);
        mDataBinding.viewUserRight.setOnClickListener(this);
    }

    @Override
    protected void iniData() {
        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            ToastUtile.toastShort(this, "please login in");
            finish();
            return;
        }

        Room mRoom = (Room) getIntent().getExtras().getSerializable(TAG_ROOM);

        RoomManager.Instance(this).setupRoomConfig(roomConfig);

        Member mMember = new Member(mUser);
        mMember.setRoomId(mRoom);
        RoomManager.Instance(this).onJoinRoom(mRoom, mMember);

        if (isOwner()) {
            mMember.setIsSpeaker(1);
        } else {
            mMember.setIsSpeaker(0);
        }

        UserManager.Instance(this).getUserLiveData().observe(this, tempUser -> {
            if (tempUser == null) {
                return;
            }

            Member temp = RoomManager.Instance(ChatRoomActivity.this).getMine();
            if (temp == null) {
                return;
            }

            temp.setUser(tempUser);
        });

        preJoinRoom(mRoom);
    }

    private void refreshVoiceView() {
        Member member = RoomManager.Instance(ChatRoomActivity.this).getMine();
        if (member == null) {
            return;
        }

        if (isOwner()) {
            mDataBinding.ivAudio.setVisibility(View.VISIBLE);
            if (member.getIsMuted() == 1) {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_microphoneoff);
            } else if (member.getIsSelfMuted() == 1) {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_microphoneoff);
            } else {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_microphoneon);
            }
        } else {
            if (member.getIsSpeaker() == 0) {
                mDataBinding.ivAudio.setVisibility(View.GONE);
            } else {
                mDataBinding.ivAudio.setVisibility(View.VISIBLE);
                if (member.getIsMuted() == 1) {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_microphoneoff);
                } else if (member.getIsSelfMuted() == 1) {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_microphoneoff);
                } else {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_microphoneon);
                }
            }
        }
    }

    private void preJoinRoom(Room room) {
        onLoadRoom(room);

        RoomManager.Instance(this)
                .getRoom(room)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataMaybeObserver<Room>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        if (RoomManager.isLeaving) {
                            return;
                        }

                        ToastUtile.toastShort(ChatRoomActivity.this, R.string.error_room_not_exsit);
                        RoomManager.Instance(ChatRoomActivity.this).leaveRoom();
                        finish();
                    }

                    @Override
                    public void handleSuccess(@Nullable Room room) {
                        if (RoomManager.isLeaving) {
                            return;
                        }

                        if (room == null) {
                            ToastUtile.toastShort(ChatRoomActivity.this, R.string.error_room_not_exsit);
                            RoomManager.Instance(ChatRoomActivity.this).leaveRoom();
                            finish();
                            return;
                        }

                        Member mine = RoomManager.Instance(ChatRoomActivity.this).getMine();
                        if (mine == null) {
                            RoomManager.Instance(ChatRoomActivity.this).leaveRoom();
                            finish();
                            return;
                        }

                        RoomManager.Instance(ChatRoomActivity.this)
                                .getMember(mine.getUserId().getObjectId())
                                .observeOn(AndroidSchedulers.mainThread())
                                .compose(mLifecycleProvider.bindToLifecycle())
                                .subscribe(new DataMaybeObserver<Member>(ChatRoomActivity.this) {
                                    @Override
                                    public void handleError(@NonNull BaseError e) {
                                        if (RoomManager.isLeaving) {
                                            return;
                                        }

                                        ToastUtile.toastShort(ChatRoomActivity.this, R.string.error_room_not_exsit);
                                        finish();
                                    }

                                    @Override
                                    public void handleSuccess(@Nullable Member member) {
                                        if (RoomManager.isLeaving) {
                                            return;
                                        }

                                        Room room = RoomManager.Instance(ChatRoomActivity.this).getRoom();
                                        onLoadRoom(room);

                                        joinRoom();
                                    }
                                });
                    }
                });
    }

    private void getMembers() {
        Room room = RoomManager.Instance(this).getRoom();
        if (room == null) {
            return;
        }

        DataRepositroy.Instance(this)
                .getMembers(room)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataObserver<List<Member>>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {

                    }

                    @Override
                    public void handleSuccess(@NonNull List<Member> members) {
                        if (RoomManager.isLeaving) {
                            return;
                        }

                        onLoadRoomMembers(members);
                    }
                });
    }

    private void onLoadRoom(Room room) {

    }

    private void onLoadRoomMembers(@NonNull List<Member> members) {
        RoomManager.Instance(this).onLoadRoomMembers(members);

        for (Member member : members) {
            mAdapterMembers.addItem(member);
        }
    }

    private InvitedMenuDialog inviteDialog;

    private void showInviteDialog() {
        if (inviteDialog != null && inviteDialog.isShowing()) {
            return;
        }

        Room room = RoomManager.Instance(this).getRoom();
        if (room == null) {
            return;
        }
        inviteDialog = new InvitedMenuDialog();
        inviteDialog.show(getSupportFragmentManager(), room.getAnchorId());
    }

    private void closeInviteDialog() {
        if (inviteDialog != null && inviteDialog.isShowing()) {
            inviteDialog.dismiss();
        }
    }

    private void joinRoom() {
        RoomManager.Instance(this)
                .joinRoom()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        if (RoomManager.isLeaving) {
                            return;
                        }

                        showErrorDialog(e.getMessage());
                    }

                    @Override
                    public void handleSuccess() {
                        if (RoomManager.isLeaving) {
                            return;
                        }

                        getMembers();
                        onJoinRoomEnd();
                    }
                });
    }

    private void onJoinRoomEnd() {
        refreshVoiceView();
        refreshHandUpView();

        Member member = RoomManager.Instance(this).getMine();
        if (member == null) {
            return;
        }

        Member owner = RoomManager.Instance(this).getOwner();
        if (owner == null) {
            return;
        }
        if (isOwner()) {
            mDataBinding.ivRequest.setVisibility(View.VISIBLE);
            mDataBinding.ivMagic.setVisibility(View.VISIBLE);
            mDataBinding.ivAudio.setVisibility(View.VISIBLE);


            if (member.getIsSpeaker() == 1) {
                SurfaceView mLocalView = mDataBinding.viewUserMiddle.getVideoSurfaceView();
                mLocalView.setZOrderMediaOverlay(true);
                VideoCanvas mVideoCanvas = new VideoCanvas(mLocalView);
                mVideoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
                mVideoCanvas.uid = RtcManager.Instance(this).uid;
                RtcManager.Instance(this).getRtcEngine().setupLocalVideo(mVideoCanvas);
                mDataBinding.viewUserMiddle.onMemberJoin(member);

                RoomManager.Instance(this).startLivePlay();
                RtcManager.Instance(this).getRtcEngine().startPreview();
            } else {
                RoomManager.Instance(this).stopLivePlay();
            }
        } else {
            mDataBinding.ivRequest.setVisibility(View.GONE);
            mDataBinding.ivMagic.setVisibility(View.GONE);
            mDataBinding.ivAudio.setVisibility(View.GONE);

            mDataBinding.viewUserMiddle.onMemberJoin(owner);
            SurfaceView mRemoteView = mDataBinding.viewUserMiddle.getVideoSurfaceView();
            VideoCanvas mVideoCanvasOwner = new VideoCanvas(mRemoteView);
            mVideoCanvasOwner.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
            mVideoCanvasOwner.uid = owner.getStreamId().intValue();
            RtcManager.Instance(this).getRtcEngine().setupRemoteVideo(mVideoCanvasOwner);

            if (member.getIsSpeaker() == 1) {
                SurfaceView mLocalView = mDataBinding.viewUserLeft.getVideoSurfaceView();
                mLocalView.setZOrderMediaOverlay(true);
                VideoCanvas mVideoCanvas = new VideoCanvas(mLocalView);
                mVideoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
                mVideoCanvas.uid = RtcManager.Instance(this).uid;
                RtcManager.Instance(this).getRtcEngine().setupLocalVideo(mVideoCanvas);
                mDataBinding.viewUserLeft.onMemberJoin(member);

                RoomManager.Instance(this).startLivePlay();
                RtcManager.Instance(this).getRtcEngine().startPreview();
            } else {
                RoomManager.Instance(this).stopLivePlay();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivMembers) {

        } else if (id == R.id.ivRequest) {

        } else if (id == R.id.ivMagic) {

        } else if (id == R.id.ivAudio) {
            toggleAudio();
        } else if (id == R.id.viewUserLeft) {
        } else if (id == R.id.viewUserRight) {
        } else if (id == R.id.ivBack) {
            showLeaveRoomDialog();
        }
    }

    private void leaveRoom() {
        RoomManager.Instance(this).leaveRoom();
        finish();
    }

    private void gotoHandsUpList() {
        Room mRoom = RoomManager.Instance(this).getRoom();
        if (mRoom == null) {
            return;
        }

        new HandUpDialog().show(getSupportFragmentManager());
    }

    private void toggleAudio() {
        if (!RoomManager.Instance(this).isOwner()) {
            Member member = RoomManager.Instance(this).getMine();
            if (member == null) {
                return;
            }

            if (member.getIsMuted() == 1) {
                ToastUtile.toastShort(this, R.string.member_muted);
                return;
            }
        }

        mDataBinding.ivAudio.setEnabled(false);
        RoomManager.Instance(this)
                .toggleSelfAudio()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        ToastUtile.toastShort(ChatRoomActivity.this, e.getMessage());
                        mDataBinding.ivAudio.setEnabled(true);
                    }

                    @Override
                    public void handleSuccess() {
                        mDataBinding.ivAudio.setEnabled(true);
                        refreshVoiceView();
                    }
                });
    }

    private void refreshHandUpView() {
//        if (RoomManager.Instance(this).isOwner()) {
//            mDataBinding.ivHandUp.setVisibility(View.GONE);
//        } else {
//            mDataBinding.ivHandUp.setImageResource(R.mipmap.icon_un_handup);
//
//            Member member = RoomManager.Instance(this).getMine();
//            if (member == null) {
//                return;
//            }
//            mDataBinding.ivHandUp.setVisibility(member.getIsSpeaker() == 0 ? View.VISIBLE : View.GONE);
//        }
    }

    private void toggleHandUp() {
//        mDataBinding.ivHandUp.setEnabled(false);
//        RoomManager.Instance(this)
//                .requestHandsUp()
//                .observeOn(AndroidSchedulers.mainThread())
//                .compose(mLifecycleProvider.bindToLifecycle())
//                .subscribe(new DataCompletableObserver(this) {
//                    @Override
//                    public void handleError(@NonNull BaseError e) {
//                        ToastUtile.toastShort(ChatRoomActivity.this, e.getMessage());
//                        mDataBinding.ivHandUp.setEnabled(true);
//                    }
//
//                    @Override
//                    public void handleSuccess() {
//                        refreshHandUpView();
//                        mDataBinding.ivHandUp.setEnabled(true);
//                        ToastUtile.toastShort(ChatRoomActivity.this, R.string.request_handup_success);
//                    }
//                });
    }

    private UserSeatMenuDialog mUserSeatMenuDialog;

    private void showUserMenuDialog(Member data) {
        if (mUserSeatMenuDialog != null && mUserSeatMenuDialog.isShowing()) {
            return;
        }

        mUserSeatMenuDialog = new UserSeatMenuDialog();
        mUserSeatMenuDialog.show(getSupportFragmentManager(), data);
    }


    private InviteMenuDialog mInviteMenuDialog;

    private void showUserInviteDialog(Member data) {
        if (mInviteMenuDialog != null && mInviteMenuDialog.isShowing()) {
            return;
        }

        mInviteMenuDialog = new InviteMenuDialog();
        mInviteMenuDialog.show(getSupportFragmentManager());
    }

    @Override
    public void onOwnerLeaveRoom(@NonNull Room room) {
        ToastUtile.toastShort(this, R.string.room_closed);
        finish();
    }

    @Override
    public void onLeaveRoom(@NonNull Room room) {

    }

    @Override
    public void onMemberJoin(@NonNull Member member) {

    }

    @Override
    public void onMemberLeave(@NonNull Member member) {
    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull Member member) {
        refreshVoiceView();
        refreshHandUpView();

        if (!isMine && isMine(member)) {
            if (member.getIsSpeaker() == 0) {
                ToastUtile.toastShort(this, R.string.member_speaker_to_listener);
            }
        }
    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull Member member) {
        refreshVoiceView();

        if (!isMine && isMine(member)) {
            if (member.getIsMuted() == 1) {
                ToastUtile.toastShort(this, R.string.member_muted);
            }
        }

    }

    @Override
    public void onSDKVideoStatusChanged(@NonNull Member member) {
        mDataBinding.viewUserMiddle.onMemberVideoChanged(member);
    }

    @Override
    public void onReceivedHandUp(@NonNull Member member) {
        mDataBinding.ivRequest.setCount(DataRepositroy.Instance(this).getHandUpListCount());
    }

    @Override
    public void onHandUpAgree(@NonNull Member member) {
        refreshHandUpView();

        if (isOwner()) {
            mDataBinding.ivRequest.setCount(DataRepositroy.Instance(this).getHandUpListCount());
        }
    }

    @Override
    public void onHandUpRefuse(@NonNull Member member) {
        if (isMine(member)) {
            ToastUtile.toastShort(this, R.string.handup_refuse);
        }

        refreshHandUpView();

        if (isOwner()) {
            mDataBinding.ivRequest.setCount(DataRepositroy.Instance(this).getHandUpListCount());
        }
    }

    @Override
    public void onReceivedInvite(@NonNull Member member) {
        showInviteDialog();
    }

    @Override
    public void onInviteAgree(@NonNull Member member) {

    }

    @Override
    public void onInviteRefuse(@NonNull Member member) {
        if (isOwner()) {
            ToastUtile.toastShort(this, getString(R.string.invite_refuse, member.getUserId().getName()));
        }
    }

    @Override
    public void onEnterMinStatus() {

    }


    @Override
    public void onRoomError(int error) {
        showErrorDialog(getString(R.string.error_room_default, String.valueOf(error)));
    }

    private AlertDialog errorDialog = null;

    private void showErrorDialog(String msg) {
        if (errorDialog != null && errorDialog.isShowing()) {
            return;
        }

        errorDialog = new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leaveRoom();
                    }
                })
                .show();
    }

    private boolean isMine(Member member) {
        return RoomManager.Instance(this).isMine(member);
    }

    private boolean isOwner() {
        return RoomManager.Instance(this).isOwner();
    }

    private boolean isOwner(Member member) {
        return RoomManager.Instance(this).isOwner(member);
    }

    private void showCloseRoomDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.show(getSupportFragmentManager(), getString(R.string.room_dialog_close_room_title), getString(R.string.room_dialog_close_room_message),
                new ConfirmDialog.OnConfirmCallback() {
                    @Override
                    public void onClickCancel() {

                    }

                    @Override
                    public void onClickConfirm() {
                        leaveRoom();
                    }
                });
    }

    private void showLeaveRoomDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.show(getSupportFragmentManager(), getString(R.string.room_dialog_leave_room_title), getString(R.string.room_dialog_leave_room_message),
                new ConfirmDialog.OnConfirmCallback() {
                    @Override
                    public void onClickCancel() {

                    }

                    @Override
                    public void onClickConfirm() {
                        leaveRoom();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        RoomManager.Instance(this).removeRoomEventCallback(this);
        closeInviteDialog();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

    }
}
