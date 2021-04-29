package io.agora.marriageinterview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.ObjectsCompat;

import com.agora.data.BaseError;
import com.agora.data.RoomEventCallback;
import com.agora.data.manager.RTMManager;
import com.agora.data.manager.RoomManager;
import com.agora.data.manager.RtcManager;
import com.agora.data.manager.UserManager;
import com.agora.data.model.Action;
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
import io.agora.marriageinterview.adapter.RoomMessagesAdapter;
import io.agora.marriageinterview.adapter.RoomPreMemberListAdapter;
import io.agora.marriageinterview.data.DataRepositroy;
import io.agora.marriageinterview.databinding.MerryActivityChatRoomBinding;
import io.agora.marriageinterview.widget.ConfirmDialog;
import io.agora.marriageinterview.widget.InputMessageDialog;
import io.agora.marriageinterview.widget.InvitedMenuDialog;
import io.agora.marriageinterview.widget.MemberListDialog;
import io.agora.marriageinterview.widget.RequestConnectListDialog;
import io.agora.marriageinterview.widget.RoomSpeakerView;
import io.agora.marriageinterview.widget.UserSeatMenuDialog;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.BeautyOptions;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 直播
 * 1. 查询Room表，房间是否存在，不存在就退出。
 * 2. 查询Member表
 * 2.1. 不存在就创建用户，并且用0加入到RTC，利用RTC分配一个唯一的uid，并且修改member的streamId值.
 * 2.2. 存在就返回member对象，利用streamId加入到RTC。
 *
 * @author chenhengfei@agora.io
 */
public class ChatRoomActivity extends DataBindBaseActivity<MerryActivityChatRoomBinding> implements View.OnClickListener, RoomEventCallback {

    private static final String TAG = ChatRoomActivity.class.getSimpleName();

    private static final String TAG_ROOM = "room";

    private RoomPreMemberListAdapter mAdapterMembers;
    private RoomMessagesAdapter mAdapterMessage;

    private final IRoomConfigProvider roomConfig = new IRoomConfigProvider() {

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
        return R.layout.merry_activity_chat_room;
    }

    @Override
    protected void iniView() {
        mAdapterMembers = new RoomPreMemberListAdapter(null, null);
        mAdapterMessage = new RoomMessagesAdapter(null, null);
        mDataBinding.rvMembers.setAdapter(mAdapterMembers);
        mDataBinding.rvMessage.setAdapter(mAdapterMessage);
    }

    @Override
    protected void iniListener() {
        RoomManager.Instance(this).addRoomEventCallback(this);
        mDataBinding.ivBack.setOnClickListener(this);
        mDataBinding.ivMembers.setOnClickListener(this);
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
            mMember.setRole(Member.Role.Speaker);
        } else {
            mMember.setRole(Member.Role.Listener);
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

    private void refreshMagicIcon() {
        Member member = RoomManager.Instance(ChatRoomActivity.this).getMine();
        if (member == null) {
            return;
        }

        if (member.getRole() == Member.Role.Listener) {
            mDataBinding.ivMagic.setVisibility(View.GONE);
        } else {
            mDataBinding.ivMagic.setVisibility(View.VISIBLE);
        }
    }

    private void refreshAudioView() {
        Member member = RoomManager.Instance(ChatRoomActivity.this).getMine();
        if (member == null) {
            return;
        }

        if (isOwner()) {
            mDataBinding.ivAudio.setVisibility(View.VISIBLE);
            if (member.getIsMuted() == 1) {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_menu_close_audio);
            } else if (member.getIsSelfMuted() == 1) {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_menu_close_audio);
            } else {
                mDataBinding.ivAudio.setImageResource(R.mipmap.icon_menu_open_audio);
            }
        } else {
            if (member.getRole() == Member.Role.Listener) {
                mDataBinding.ivAudio.setVisibility(View.GONE);
            } else {
                mDataBinding.ivAudio.setVisibility(View.VISIBLE);
                if (member.getIsMuted() == 1) {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_menu_close_audio);
                } else if (member.getIsSelfMuted() == 1) {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_menu_close_audio);
                } else {
                    mDataBinding.ivAudio.setImageResource(R.mipmap.icon_menu_open_audio);
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

                        onLoadRoom(room);

                        //查看是否已经加入到了房间
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
                                        joinRoom();
                                    }

                                    @Override
                                    public void handleSuccess(@Nullable Member member) {
                                        if (RoomManager.isLeaving) {
                                            return;
                                        }

                                        if (member != null) {
                                            RoomManager.Instance(ChatRoomActivity.this).updateMine(member);
                                        }

                                        joinRoom();
                                    }
                                });
                    }
                });
    }

    private void joinRTM() {
        Room room = RoomManager.Instance(ChatRoomActivity.this).getRoom();
        Member mine = RoomManager.Instance(ChatRoomActivity.this).getMine();
        if (room == null || mine == null) {
            return;
        }

        RTMManager.Instance(ChatRoomActivity.this)
                .login(mine.getObjectId())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .concatWith(RTMManager.Instance(ChatRoomActivity.this).joinChannel(room.getObjectId()))
                .subscribe(new DataCompletableObserver(ChatRoomActivity.this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        ToastUtile.toastShort(ChatRoomActivity.this, e.getMessage());
                    }

                    @Override
                    public void handleSuccess() {

                    }
                });
    }

    private void leaveRTM() {
        RTMManager.Instance(ChatRoomActivity.this)
                .leaveChannel()
                .andThen(RTMManager.Instance(ChatRoomActivity.this).logout())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(ChatRoomActivity.this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                    }

                    @Override
                    public void handleSuccess() {

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
                        showErrorDialog(e.getMessage());
                    }

                    @Override
                    public void handleSuccess(@NonNull List<Member> members) {
                        if (RoomManager.isLeaving) {
                            return;
                        }

                        onLoadRoomMembers(members);
                        onJoinRoomEnd();
                    }
                });
    }

    private void onLoadRoom(@NonNull Room room) {

    }

    private void onLoadRoomMembers(@NonNull List<Member> members) {
        RoomManager.Instance(this).onLoadRoomMembers(members);

        for (Member member : members) {
            if (mAdapterMembers.getItemCount() < 3) {
                mAdapterMembers.addItem(member);
            }

            boolean isLocal = isMine(member);
            if (member.getRole() == Member.Role.Left) {
                mDataBinding.viewUserLeft.onMemberJoin(isLocal, member);
            }
            if (member.getRole() == Member.Role.Right) {
                mDataBinding.viewUserRight.onMemberJoin(isLocal, member);
            }
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
        inviteDialog.show(getSupportFragmentManager(), room.getAnchorId(), new InvitedMenuDialog.IConnectStatusProvider() {
            @Override
            public boolean hasLeftMember() {
                return mDataBinding.viewUserLeft.hasMember();
            }

            @Override
            public boolean hasRightMember() {
                return mDataBinding.viewUserRight.hasMember();
            }
        });
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

                        joinRTM();
                        getMembers();
                    }
                });
    }

    private void onJoinRoomEnd() {
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

            mDataBinding.viewUserMiddle.onMemberJoin(true, member);

            RoomManager.Instance(this).startLivePlay();
            refreshAudioView();
        } else {
            mDataBinding.ivRequest.setVisibility(View.GONE);
            mDataBinding.ivMagic.setVisibility(View.GONE);
            mDataBinding.ivAudio.setVisibility(View.GONE);

            mDataBinding.viewUserMiddle.onMemberJoin(false, owner);

            onRoleChanged(true, member);
            onAudioStatusChanged(true, member);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivMessage) {
            showInputMessageDialog();
        } else if (id == R.id.ivMembers) {
            showMembersDialog();
        } else if (id == R.id.ivRequest) {
            showRequestListDialog();
        } else if (id == R.id.ivMagic) {
            toggleMagic();
        } else if (id == R.id.ivAudio) {
            toggleAudio();
        } else if (id == R.id.viewUserLeft) {
            onLeftViewClick();
        } else if (id == R.id.viewUserRight) {
            onRightViewClick();
        } else if (id == R.id.ivBack) {
            if (isOwner()) {
                showCloseRoomDialog();
            } else {
                showLeaveRoomDialog();
            }
        }
    }

    private boolean enableBeauty = false;
    private BeautyOptions mOptions = new BeautyOptions();

    private void toggleMagic() {
        enableBeauty = !enableBeauty;
        RtcManager.Instance(this).getRtcEngine().setBeautyEffectOptions(enableBeauty, mOptions);
    }

    private void showInputMessageDialog() {
        Member mine = RoomManager.Instance(ChatRoomActivity.this).getMine();
        if (mine == null) {
            return;
        }

        InputMessageDialog dialog = new InputMessageDialog();
        dialog.show(getSupportFragmentManager(), new InputMessageDialog.ISendMessageCallback() {
            @Override
            public void onSendMessage(String message) {
                onRoomMessageReceived(mine, message);
            }
        });
    }

    private void showRequestListDialog() {
        RequestConnectListDialog dialog = new RequestConnectListDialog();
        dialog.show(getSupportFragmentManager(), new RequestConnectListDialog.IConnectStatusProvider() {
            @Override
            public boolean hasLeftMember() {
                return mDataBinding.viewUserLeft.hasMember();
            }

            @Override
            public boolean hasRightMember() {
                return mDataBinding.viewUserRight.hasMember();
            }
        });
    }

    private void showMembersDialog() {
        MemberListDialog dialog = new MemberListDialog();
        dialog.show(getSupportFragmentManager(), new MemberListDialog.IConnectStatusProvider() {
            @Override
            public boolean isMemberConnected(Member member) {
                if (ObjectsCompat.equals(mDataBinding.viewUserLeft.getMember(), member)) {
                    return true;
                }

                if (ObjectsCompat.equals(mDataBinding.viewUserRight.getMember(), member)) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean hasLeftMember() {
                return mDataBinding.viewUserLeft.hasMember();
            }

            @Override
            public boolean hasRightMember() {
                return mDataBinding.viewUserRight.hasMember();
            }
        });
    }

    private void onLeftViewClick() {
        if (mDataBinding.viewUserLeft.hasMember()) {
            if (isOwner()) {
                showUserMenuDialog(mDataBinding.viewUserLeft.getMember());
            }
        } else {
            if (isOwner()) {
                return;
            }

            Member member = RoomManager.Instance(this).getMine();
            if (member == null || member.getRole() != Member.Role.Listener) {
                return;
            }
            showRequestDialog(mDataBinding.viewUserLeft, Action.ACTION.RequestLeft);
        }
    }

    private void onRightViewClick() {
        if (mDataBinding.viewUserRight.hasMember()) {
            if (isOwner()) {
                showUserMenuDialog(mDataBinding.viewUserRight.getMember());
            }
        } else {
            if (isOwner()) {
                return;
            }

            Member member = RoomManager.Instance(this).getMine();
            if (member == null || member.getRole() != Member.Role.Listener) {
                return;
            }
            showRequestDialog(mDataBinding.viewUserRight, Action.ACTION.RequestRight);
        }
    }

    private ConfirmDialog requestConnectDialog;

    private void showRequestDialog(RoomSpeakerView mRoomSpeakerView, @NonNull Action.ACTION action) {
        if (requestConnectDialog != null && requestConnectDialog.isShowing()) {
            return;
        }

        requestConnectDialog = new ConfirmDialog();
        requestConnectDialog.show(getSupportFragmentManager(), getString(R.string.request_dialog_title), getString(R.string.request_dialog_message), new ConfirmDialog.OnConfirmCallback() {

            @Override
            public void onClickCancel() {

            }

            @Override
            public void onClickConfirm() {
                requestContect(mRoomSpeakerView, action);
            }
        });
    }

    private void leaveRoom() {
        leaveRTM();
        RoomManager.Instance(this).leaveRoom();
        finish();
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
                        refreshAudioView();
                    }
                });
    }

    private void requestContect(RoomSpeakerView mRoomSpeakerView, @NonNull Action.ACTION action) {
        Member member = RoomManager.Instance(this).getMine();
        if (member == null || member.getRole() != Member.Role.Listener) {
            return;
        }

        mRoomSpeakerView.setEnabled(false);
        RoomManager.Instance(this)
                .requestConnect(action)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        ToastUtile.toastShort(ChatRoomActivity.this, e.getMessage());
                        mRoomSpeakerView.setEnabled(true);
                    }

                    @Override
                    public void handleSuccess() {
                        mRoomSpeakerView.setEnabled(true);
                        ToastUtile.toastShort(ChatRoomActivity.this, R.string.request_handup_success);
                    }
                });
    }

    private UserSeatMenuDialog mUserSeatMenuDialog;

    private void showUserMenuDialog(Member data) {
        if (mUserSeatMenuDialog != null && mUserSeatMenuDialog.isShowing()) {
            return;
        }

        mUserSeatMenuDialog = new UserSeatMenuDialog();
        mUserSeatMenuDialog.show(getSupportFragmentManager(), data);
    }


    @Override
    public void onRoomClosed(@NonNull Room room, boolean fromUser) {
        if (!fromUser) {
            ToastUtile.toastShort(this, R.string.room_closed);
            leaveRoom();
        }
    }

    @Override
    public void onMemberJoin(@NonNull Member member) {
        if (mAdapterMembers.getItemCount() < 3) {
            mAdapterMembers.addItem(member);
        }

        mAdapterMessage.onMemberJoin(ChatRoomActivity.this, member);
    }

    @Override
    public void onMemberLeave(@NonNull Member member) {
        mAdapterMembers.deleteItem(member);

        mAdapterMessage.onMemberLeave(ChatRoomActivity.this, member);

        this.mDataBinding.viewUserLeft.onMemberLeave(member);
        this.mDataBinding.viewUserRight.onMemberLeave(member);
    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull Member member) {
        if (isMine(member)) {
            refreshAudioView();
            refreshMagicIcon();
        }

        boolean isLocal = isMine(member);
        if (member.getRole() == Member.Role.Listener) {
            this.mDataBinding.viewUserLeft.onMemberLeave(member);
            this.mDataBinding.viewUserRight.onMemberLeave(member);
        } else if (member.getRole() == Member.Role.Left) {
            this.mDataBinding.viewUserLeft.onMemberJoin(isLocal, member);
        } else if (member.getRole() == Member.Role.Right) {
            this.mDataBinding.viewUserRight.onMemberJoin(isLocal, member);
        }

        if (!isMine && isMine(member)) {
            if (member.getRole() == Member.Role.Listener) {
                ToastUtile.toastShort(this, R.string.member_speaker_to_listener);
            }
        }
    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull Member member) {
        if (isMine(member)) {
            refreshAudioView();
        }

        Member mOwner = RoomManager.Instance(this).getOwner();
        if (ObjectsCompat.equals(member, mOwner)) {
            mDataBinding.viewUserMiddle.onMemberAudioChanged(member);
        } else {
            mDataBinding.viewUserLeft.onMemberAudioChanged(member);
            mDataBinding.viewUserRight.onMemberAudioChanged(member);
        }

        if (!isMine && isMine(member)) {
            if (member.getIsMuted() == 1) {
                ToastUtile.toastShort(this, R.string.member_muted);
            }
        }
    }

    @Override
    public void onSDKVideoStatusChanged(@NonNull Member member) {
        mDataBinding.viewUserMiddle.onMemberVideoChanged(member);
        mDataBinding.viewUserLeft.onMemberVideoChanged(member);
        mDataBinding.viewUserRight.onMemberVideoChanged(member);
    }

    @Override
    public void onReceivedRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        mDataBinding.ivRequest.setCount(DataRepositroy.Instance(this).getHandUpListCount());
    }

    @Override
    public void onRequestAgreed(@NonNull Member member) {
        if (isOwner()) {
            mDataBinding.ivRequest.setCount(DataRepositroy.Instance(this).getHandUpListCount());
        }
    }

    @Override
    public void onRequestRefuse(@NonNull Member member) {
        if (isMine(member)) {
            ToastUtile.toastShort(this, R.string.handup_refuse);
        }

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
        if (mDataBinding.viewUserLeft.hasMember() && mDataBinding.viewUserRight.hasMember()) {
            return;
        }

        boolean isLocal = isMine(member);
        if (mDataBinding.viewUserLeft.hasMember() == false) {
            member.setRole(Member.Role.Left);
            mDataBinding.viewUserLeft.onMemberJoin(isLocal, member);
        } else if (mDataBinding.viewUserRight.hasMember() == false) {
            member.setRole(Member.Role.Right);
            mDataBinding.viewUserLeft.onMemberJoin(isLocal, member);
        }
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

    @Override
    public void onRoomMessageReceived(@NonNull Member member, @NonNull String message) {
        mAdapterMessage.onRoomMessageReceived(member, message);
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
        mDataBinding.ivBack.performClick();
    }
}
