package io.agora.sample.breakoutroom.bean;

import androidx.annotation.NonNull;

public class RoomInfo {
    private @NonNull final String id;
    private @NonNull final String userId;
    private @NonNull final String backgroundId;

    public RoomInfo(@NonNull String id, @NonNull String userId, @NonNull String backgroundId) {
        this.id = id;
        this.userId = userId;
        this.backgroundId = backgroundId;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getBackgroundId() {
        return backgroundId;
    }
}
