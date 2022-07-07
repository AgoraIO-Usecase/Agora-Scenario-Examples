package io.agora.scene.breakoutroom.repo;

import androidx.annotation.NonNull;

public interface RoomApi {

    void joinRTCRoom(@NonNull String roomId);
    void createSubRoom(@NonNull String name);
    void fetchAllSubRooms();
    void subscribeSubRooms();
}
