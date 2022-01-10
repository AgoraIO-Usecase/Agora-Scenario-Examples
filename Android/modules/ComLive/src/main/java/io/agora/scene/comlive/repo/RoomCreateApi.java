package io.agora.scene.comlive.repo;

import androidx.annotation.NonNull;

import io.agora.scene.comlive.bean.RoomInfo;

public interface RoomCreateApi {
    void createRoom(@NonNull RoomInfo roomInfo);
}
