package io.agora.livepk.model;

import java.io.Serializable;
import java.util.HashMap;

public class RoomInfo implements Serializable {
    public long createTime;
    public String roomId;
    public String roomName;

    public RoomInfo() {
    }

    public RoomInfo(long createTime, String roomId, String roomName) {
        this.createTime = createTime;
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("createTime", createTime);
        data.put("roomId", roomId);
        data.put("roomName", roomName);
        return data;
    }

}
