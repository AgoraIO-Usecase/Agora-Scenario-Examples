package io.agora.marriageinterview.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.IDataRepositroy;
import com.agora.data.manager.RoomManager;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.RequestMember;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.provider.IDataProvider;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;

public final class DataRepositroy implements IDataRepositroy {
    private static final String TAG = DataRepositroy.class.getSimpleName();

    private volatile static DataRepositroy instance;

    private Context mContext;

    private IDataProvider mDataProvider;

    private DataRepositroy(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized DataRepositroy Instance(Context context) {
        if (instance == null) {
            synchronized (DataRepositroy.class) {
                if (instance == null)
                    instance = new DataRepositroy(context);
            }
        }
        return instance;
    }

    private IDataProvider getIDataProvider() {
        if (mDataProvider == null) {
            mDataProvider = new DataProvider(mContext, RoomManager.Instance(mContext));
        }
        return mDataProvider;
    }

    @Override
    public Observable<User> login(@NonNull User user) {
        return getIDataProvider().getStoreSource().login(user);
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        return getIDataProvider().getStoreSource().update(user);
    }

    @Override
    public Maybe<List<Room>> getRooms() {
        return getIDataProvider().getStoreSource().getRooms();
    }

    @Override
    public Observable<Room> getRoomCountInfo(@NonNull Room room) {
        return getIDataProvider().getStoreSource().getRoomCountInfo(room);
    }

    @Override
    public Maybe<Room> getRoomSpeakersInfo(@NonNull Room room) {
        return getIDataProvider().getStoreSource().getRoomSpeakersInfo(room);
    }

    @Override
    public Observable<Room> creatRoom(@NonNull Room room) {
        return getIDataProvider().getStoreSource().creatRoom(room);
    }

    @Override
    public Maybe<Room> getRoom(@NonNull Room room) {
        return getIDataProvider().getStoreSource().getRoom(room);
    }

    @Override
    public Observable<List<Member>> getMembers(@NonNull Room room) {
        return getIDataProvider().getStoreSource().getMembers(room);
    }

    @Override
    public Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId) {
        return getIDataProvider().getStoreSource().getMember(roomId, userId);
    }

    @Override
    public Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member) {
        return getIDataProvider().getMessageSource().joinRoom(room, member);
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        return getIDataProvider().getMessageSource().leaveRoom(room, member);
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        return getIDataProvider().getMessageSource().muteVoice(member, muted);
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        return getIDataProvider().getMessageSource().muteSelfVoice(member, muted);
    }

    @Override
    public Completable requestConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        return getIDataProvider().getMessageSource().requestConnect(member, action);
    }

    @Override
    public Completable agreeRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        return getIDataProvider().getMessageSource().agreeRequest(member, action);
    }

    @Override
    public Completable refuseRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        return getIDataProvider().getMessageSource().refuseRequest(member, action);
    }

    @Override
    public Completable inviteConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        return getIDataProvider().getMessageSource().inviteConnect(member, action);
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        return getIDataProvider().getMessageSource().agreeInvite(member);
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        return getIDataProvider().getMessageSource().refuseInvite(member);
    }

    @Override
    public Completable seatOff(@NonNull Member member, @NonNull Member.Role role) {
        return getIDataProvider().getMessageSource().seatOff(member, role);
    }

    @Override
    public Observable<List<RequestMember>> getRequestList() {
        return getIDataProvider().getMessageSource().getRequestList();
    }

    @Override
    public int getHandUpListCount() {
        return getIDataProvider().getMessageSource().getHandUpListCount();
    }
}
