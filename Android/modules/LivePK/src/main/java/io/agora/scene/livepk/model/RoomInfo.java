package io.agora.scene.livepk.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.HashMap;

public class RoomInfo implements Serializable {
    private final long createTime;
    @NonNull
    private final String roomId;
    @NonNull
    private final String roomName;

    public RoomInfo(long createTime, @NonNull String roomId, @NonNull String roomName) {
        this.createTime = createTime;
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public long getCreateTime() {
        return createTime;
    }

    @NonNull
    public String getRoomId() {
        return roomId;
    }

    @NonNull
    public String getRoomName() {
        return roomName;
    }

    @NonNull
    public HashMap<String, String> toMap() {
        HashMap<String, String> data = new HashMap<>();
        data.put("createTime", Long.toString(createTime));
        data.put("roomId", roomId);
        data.put("roomName", roomName);
        return data;
    }

}
