package io.agora.sample.rtegame.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import io.agora.sample.rtegame.bean.RoomInfo;

public interface RoomCreateApi {
    LiveData<RoomInfo> createRoom(@NonNull RoomInfo roomInfo);
}
