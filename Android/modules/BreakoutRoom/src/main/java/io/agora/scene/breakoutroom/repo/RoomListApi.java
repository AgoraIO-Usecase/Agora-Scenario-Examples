package io.agora.scene.breakoutroom.repo;

import androidx.annotation.NonNull;

import io.agora.scene.breakoutroom.bean.RoomInfo;

public interface RoomListApi {
    void createRoom(@NonNull RoomInfo roomInfo);
    void fetchRoomList();
}
