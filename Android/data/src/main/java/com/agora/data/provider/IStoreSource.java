package com.agora.data.provider;

import androidx.annotation.NonNull;

import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.agora.data.model.User;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;

public interface IStoreSource {

    Observable<User> login(@NonNull User user);

    Observable<User> update(@NonNull User user);

    Observable<List<Room>> getRooms();

    Observable<Room> getRoomListInfo(@NonNull Room room);

    Maybe<Room> getRoomListInfo2(@NonNull Room room);

    Observable<Room> creatRoom(@NonNull Room room);

    Maybe<Room> getRoom(@NonNull Room room);

    Observable<List<Member>> getMembers(@NonNull Room room);

    Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId);
}
