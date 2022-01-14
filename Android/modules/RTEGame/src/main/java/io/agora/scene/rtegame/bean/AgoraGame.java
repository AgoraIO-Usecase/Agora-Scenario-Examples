package io.agora.scene.rtegame.bean;

import androidx.annotation.NonNull;

public class AgoraGame {
    @NonNull
    private final String gameId;
    @NonNull
    private final String gameName;

    public AgoraGame(@NonNull String gameId, @NonNull String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;
    }

    @NonNull
    public String getGameId() {
        return gameId;
    }

    @NonNull
    public String getGameName() {
        return gameName;
    }
}

