package io.agora.sample.rtegame.repo;

import androidx.annotation.NonNull;

import io.agora.sample.rtegame.bean.RoomInfo;

public interface RoomCreateApi {
    void createRoom(@NonNull RoomInfo roomInfo);
}
