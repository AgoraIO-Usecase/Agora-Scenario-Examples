package io.agora.scene.comlive.bean;

import androidx.annotation.NonNull;

public class GameApplyInfo {
    public static final int IDLE = 1;
    public static final int PLAYING = 2;
    public static final int END = 3;

    //        1 - 未开始, 2 - 进行中, 3 - 已结束 (需要游戏有一个加载完成的回调)
    private int status;
    //       1 你画我猜
    private final int gameId;

    public GameApplyInfo(int status, int gameUid) {
        this.status = status; 
        this.gameId = gameUid;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public int getGameId() {
        return gameId;
    }

    @NonNull
    @Override
    public String toString() {
        return "GameApplyInfo{" +
                "status=" + status +
                ", gameId=" + gameId +
                '}';
    }
}
