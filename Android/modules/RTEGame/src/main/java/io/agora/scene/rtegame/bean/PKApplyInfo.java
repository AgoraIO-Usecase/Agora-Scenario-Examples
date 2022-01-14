package io.agora.scene.rtegame.bean;

import androidx.annotation.NonNull;

public class PKApplyInfo implements Cloneable{
    public static final int APPLYING = 1;
    public static final int AGREED = 2;
    public static final int REFUSED = 3;
    public static final int END = 4;
//    自己的ID
    private final String userId;
//    对方的UserID
    private final String targetUserId;
//    发起pk的主播名, 用于弹窗显示, 一个随机生成的中文名字
    private final String userName;
//    1 - 申请中, 2 - 已接受, 3 - 已拒绝, 4-已结束
    private int status;
//    1 - 你画我猜
    private final String gameId;
//    自己直播间的RoomID
    private final String roomId;
//    对方直播间的RoomID
    private final String targetRoomId;

    public PKApplyInfo(@NonNull String userId, @NonNull String targetUserId, @NonNull String userName, int status,@NonNull String gameId, @NonNull String roomId, @NonNull String targetRoomId) {
        this.userId = userId;
        this.targetUserId = targetUserId;
        this.userName = userName;
        this.status = status;
        this.gameId = gameId;
        this.roomId = roomId;
        this.targetRoomId = targetRoomId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getTargetUserId() {
        return targetUserId;
    }

    @NonNull
    public String getUserName() {
        return userName;
    }

    public int getStatus() {
        return status;
    }

    @NonNull
    public String getGameId() {
        return gameId;
    }

    @NonNull
    public String getRoomId() {
        return roomId;
    }

    @NonNull
    public String getTargetRoomId() {
        return targetRoomId;
    }

    @NonNull
    @Override
    public String toString() {
        return "PKApplyInfo{" +
                "userId='" + userId + '\'' +
                ", targetUserId='" + targetUserId + '\'' +
                ", userName='" + userName + '\'' +
                ", status=" + status +
                ", gameId=" + gameId +
                ", roomId='" + roomId + '\'' +
                ", targetRoomId='" + targetRoomId + '\'' +
                '}';
    }

    @Override
    public PKApplyInfo clone() {
        PKApplyInfo pkApplyInfo = null;
        try {
            pkApplyInfo = (PKApplyInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return pkApplyInfo;
    }
}
