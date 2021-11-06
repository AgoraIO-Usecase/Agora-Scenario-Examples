package io.agora.livepk.model;

import java.io.Serializable;
import java.util.HashMap;

public class RoomInfo implements Serializable {
    public static final long EXPIRED_DURATION_MS = 5 * 60 * 1000;

    public static final int PUSH_MODE_DIRECT_CDN   = 1;
    public static final int PUSH_MODE_RTC          = 2;

    public long createTime;
    public long expiredTime;
    public String roomId;
    public String roomName;
    public int mode;
    public String userIdPK;
    public int userCount = 1;

    public RoomInfo() {
    }

    public RoomInfo(RoomInfo roomInfo) {
        this.createTime = roomInfo.createTime;
        this.expiredTime = roomInfo.expiredTime;
        this.roomId = roomInfo.roomId;
        this.roomName = roomInfo.roomName;
        this.mode = roomInfo.mode;
        this.userIdPK = roomInfo.userIdPK;
        this.userCount = roomInfo.userCount;
    }

    public RoomInfo(long createTime, String roomId, String roomName, int mode) {
        this.createTime = createTime;
        this.expiredTime = createTime + EXPIRED_DURATION_MS;
        this.roomId = roomId;
        this.roomName = roomName;
        this.mode = mode;
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("createTime", createTime);
        data.put("expiredTime", expiredTime);
        data.put("roomId", roomId);
        data.put("roomName", roomName);
        data.put("mode", mode);
        data.put("userIdPK", userIdPK);
        data.put("userCount", userCount);
        return data;
    }

}
