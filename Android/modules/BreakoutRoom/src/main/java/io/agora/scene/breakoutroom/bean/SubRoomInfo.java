package io.agora.scene.breakoutroom.bean;

import androidx.annotation.NonNull;

import java.util.Objects;

import io.agora.example.base.BaseUtil;

public class SubRoomInfo implements Comparable<SubRoomInfo> {
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

    @NonNull
    @Override
    public String toString() {
        return "SubRoomInfo{" +
                "subRoom='" + subRoom + '\'' +
                ", createTime='" + createTime + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubRoomInfo that = (SubRoomInfo) o;
        return subRoom.equals(that.getSubRoom()) && createTime.equals(that.getCreateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(subRoom, createTime);
    }

    @Override
    public int compareTo(SubRoomInfo o) {
        try {
            return (int) (Long.parseLong(this.getCreateTime()) - Long.parseLong(o.getCreateTime()));
        } catch (NumberFormatException e) {
            BaseUtil.logE(e);
        }
        return -1;
    }
}
