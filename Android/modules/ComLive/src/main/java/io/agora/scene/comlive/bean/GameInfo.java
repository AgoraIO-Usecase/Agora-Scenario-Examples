package io.agora.scene.comlive.bean;

import androidx.annotation.NonNull;

public class GameInfo {
    public static final int START = 2;
    public static final int END = 3;

    //        1 - 未开始, 2 - 进行中, 3 - 已结束 (需要游戏有一个加载完成的回调)
    private int status;
    //        屏幕共享对应的uid
    private final String gameUid;
    // 游戏ID
    @NonNull
    private final String gameId;

    public GameInfo(int status, @NonNull String gameUid, @NonNull String gameId) {
        this.status = status;
        this.gameUid = gameUid;
        this.gameId = gameId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @NonNull
    public String getGameUid() {
        return gameUid;
    }

    @NonNull
    public String getGameId() {
        return gameId;
    }

    @NonNull
    @Override
    public String toString() {
        return "GameInfo{" +
                "status=" + status +
                ", gameUid=" + gameUid +
                ", gameId='" + gameId + '\'' +
                '}';
    }
}
