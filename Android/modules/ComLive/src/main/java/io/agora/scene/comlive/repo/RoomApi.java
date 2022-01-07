package io.agora.scene.comlive.repo;

import androidx.annotation.NonNull;

import io.agora.scene.comlive.bean.LocalUser;
import io.agora.scene.comlive.bean.RoomInfo;

public interface RoomApi {
    void joinRoom(@NonNull LocalUser localUser);
}
