package io.agora.sample.breakoutroom.repo;

import androidx.annotation.NonNull;

import io.agora.sample.breakoutroom.bean.RoomInfo;

public interface RoomListApi {

    default void joinRoom(@NonNull RoomInfo roomInfo){}
    default void createRoom(@NonNull RoomInfo roomInfo){}
    default void fetchRoomList(){}
}
