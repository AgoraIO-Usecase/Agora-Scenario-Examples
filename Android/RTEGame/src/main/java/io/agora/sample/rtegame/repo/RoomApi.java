package io.agora.sample.rtegame.repo;

import androidx.annotation.NonNull;

import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.RoomInfo;

public interface RoomApi {
    void joinRoom(@NonNull RoomInfo roomInfo, LocalUser localUser);
    void joinSubRoom(@NonNull RoomInfo roomInfo, LocalUser localUser);
}
