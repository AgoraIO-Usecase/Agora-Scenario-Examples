package io.agora.scene.rtegame.bean;

import androidx.annotation.NonNull;

public class PKInfo {
    public static final int APPLYING = 1;
    public static final int AGREED = 2;
    public static final int REFUSED = 3;
    public static final int END = 4;
//    1 - 申请中, 2 - 已接受, 3 - 已拒绝, 4-已结束
    private int status;
//    发起pk的主播房间id, 对应rtc的channel
    private final String roomId;
//    对方主播的ID, 拉流使用
    private final String userId;

    public PKInfo(int status, @NonNull String roomId, @NonNull String userId) {
        this.status = status;
        this.roomId = roomId;
        this.userId = userId;
    }

    public int getStatus() {
        return status;
    }

    @NonNull
    public String getRoomId() {
        return roomId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @NonNull
    @Override
    public String toString() {
        return "PKInfo{" +
                "status=" + status +
                ", roomId='" + roomId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
