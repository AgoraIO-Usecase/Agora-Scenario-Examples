package io.agora.sample.breakoutroom.repo;

import androidx.annotation.NonNull;

import io.agora.sample.breakoutroom.bean.RoomInfo;

public interface RoomListApi {

    void joinRoom(@NonNull RoomInfo roomInfo);
    void createRoom(@NonNull RoomInfo roomInfo);
    void fetchRoomList();
}
