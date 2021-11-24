package io.agora.sample.rtegame.bean;

public class GameInfo {
    public static final int IDLE = 1;
    public static final int PLAYING = 2;
    public static final int END = 3;

    //        1 - 未开始, 2 - 进行中, 3 - 已结束 (需要游戏有一个加载完成的回调)
    private int status;
    //        1 - 你画我猜
    private final String gameId;
    //        屏幕共享对应的uid
    private final String gameUid;

    public GameInfo(int status, String gameId, String gameUid) {
        this.status = status;
        this.gameId = gameId;
        this.gameUid = gameUid;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public String getGameId() {
        return gameId;
    }

    public String getGameUid() {
        return gameUid;
    }
}
