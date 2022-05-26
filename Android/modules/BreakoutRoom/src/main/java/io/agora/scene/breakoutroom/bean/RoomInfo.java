package io.agora.scene.breakoutroom.bean;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public class RoomInfo implements Serializable {
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

    @NonNull
    @Override
    public String toString() {
        return "RoomInfo{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", backgroundId='" + backgroundId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomInfo roomInfo = (RoomInfo) o;
        return id.equals(roomInfo.id) && userId.equals(roomInfo.userId) && backgroundId.equals(roomInfo.backgroundId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, backgroundId);
    }
}
