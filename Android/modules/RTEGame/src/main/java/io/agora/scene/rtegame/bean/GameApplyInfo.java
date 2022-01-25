package io.agora.scene.rtegame.bean;

import androidx.annotation.NonNull;

public class GameApplyInfo {
    public static final int IDLE = 1;
    public static final int PLAYING = 2;
    public static final int END = 3;

    //        1 - 未开始, 2 - 进行中, 3 - 已结束 (需要游戏有一个加载完成的回调)
    private int status;
    private final String gameId;

    public GameApplyInfo(int status,@NonNull String gameId) {
        this.status = status;
        this.gameId = gameId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @NonNull
    public String getGameId() {
        return gameId;
    }

    @NonNull
    @Override
    public String toString() {
        return "GameApplyInfo{" +
                "status=" + status +
                ", gameId='" + gameId + '\'' +
                '}';
    }
}
