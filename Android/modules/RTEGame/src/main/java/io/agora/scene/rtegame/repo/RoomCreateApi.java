package io.agora.scene.rtegame.repo;

import androidx.annotation.NonNull;

import io.agora.scene.rtegame.bean.RoomInfo;

public interface RoomCreateApi {
    void createRoom(@NonNull RoomInfo roomInfo);
}
