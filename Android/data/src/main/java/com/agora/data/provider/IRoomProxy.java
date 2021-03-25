package com.agora.data.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Member;
import com.agora.data.model.Room;

import io.reactivex.Maybe;

public interface IRoomProxy {
    boolean isMembersContainsKey(@NonNull String memberId);

    @Nullable
    Member getMemberById(@NonNull String memberId);

    @Nullable
    Member getMemberByStramId(long streamId);

    Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId);

    boolean isOwner(@NonNull Member member);

    boolean isOwner();

    boolean isOwner(String userId);

    boolean isMine(@NonNull Member member);

    @Nullable
    Member getMine();

    @Nullable
    Room getRoom();

    void onMemberJoin(@NonNull Member member);

    void onMemberLeave(boolean isManual, @NonNull Member member);

    /**
     * 本地操作触发回调，比如自己下台操作，会触发此回调
     */
    void onLocalRoleChanged(@NonNull Member member);

    /**
     * 本地操作触发回调，比如自己静音，会触发次回调
     */
    void onLocalAudioStatusChanged(@NonNull Member member);

    /**
     * 远端操纵触发次回调，比如管理员把某人下台，会触发此回调
     */
    void onRemoteRoleChanged(@NonNull Member member);

    /**
     * 远端操纵触发次回调，比如管理员把某人禁言，会触发此回调
     */
    void onRemoteAudioStatusChanged(@NonNull Member member);

    void onReceivedHandUp(@NonNull Member member);

    void onHandUpAgree(@NonNull Member member);

    void onHandUpRefuse(@NonNull Member member);

    void onReceivedInvite(@NonNull Member member);

    void onInviteAgree(@NonNull Member member);

    void onInviteRefuse(@NonNull Member member);

    void onEnterMinStatus();

    void onRoomError(int error);
}
