package io.agora.sample.breakoutroom.bean;

import androidx.annotation.NonNull;

public class SubRoomInfo {
    // 子房间名
    private @NonNull final String subRoom;
    // 13位时间戳
    private @NonNull final String createTime;

    public SubRoomInfo(@NonNull String subRoom) {
        this.subRoom = subRoom;
        this.createTime = String.valueOf(System.currentTimeMillis());
    }

    public SubRoomInfo(@NonNull String userId, @NonNull String createTime) {
        this.subRoom = userId;
        this.createTime = createTime;
    }

    @NonNull
    public String getSubRoom() {
        return subRoom;
    }

    @NonNull
    public String getCreateTime() {
        return createTime;
    }
}
