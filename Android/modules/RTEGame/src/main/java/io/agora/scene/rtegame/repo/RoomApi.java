package io.agora.scene.rtegame.repo;

import androidx.annotation.NonNull;

import io.agora.scene.rtegame.bean.LocalUser;
import io.agora.scene.rtegame.bean.RoomInfo;

public interface RoomApi {
    void joinRoom(@NonNull LocalUser localUser);
    void joinSubRoom(@NonNull RoomInfo roomInfo);
}
