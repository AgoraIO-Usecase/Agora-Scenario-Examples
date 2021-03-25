package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Observable;

class MessageSource extends BaseMessageSource {

    private Gson mGson = new Gson();
    private Context mContext;

    /**
     * 申请举手用户列表
     */
    private final Map<String, Member> handUpMembers = new ConcurrentHashMap<>();

    public MessageSource(@NonNull Context context, @NonNull IRoomProxy iRoomProxy) {
        super(iRoomProxy);
        this.mContext = context;
    }


    @Override
    public Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member) {
        return null;
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        return null;
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        return null;
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        return null;
    }

    @Override
    public Completable requestHandsUp(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable agreeHandsUp(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable refuseHandsUp(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable inviteSeat(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable seatOff(@NonNull Member member) {
        return null;
    }

    @Override
    public Observable<List<Member>> getHandUpList() {
        return null;
    }

    @Override
    public int getHandUpListCount() {
        return 0;
    }
}
