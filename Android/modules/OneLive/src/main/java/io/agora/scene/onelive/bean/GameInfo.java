package io.agora.scene.onelive.bean;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

public class GameInfo {
    public static final int START = 2;
    public static final int END = 3;

    //        1 - 未开始, 2 - 进行中, 3 - 已结束 (需要游戏有一个加载完成的回调)
    private int status;
    // 游戏ID
    private final int gameId;

    public GameInfo(@IntRange(from = START, to = END) int status, int gameId) {
        this.status = status;
        this.gameId = gameId;
    }

    public void setStatus(@IntRange(from = START, to = END) int status) {
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
        return "GameInfo{" +
                "status=" + status +
                ", gameId=" + gameId +
                '}';
    }
}
