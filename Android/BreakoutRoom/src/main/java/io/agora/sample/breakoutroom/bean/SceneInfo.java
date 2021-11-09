package io.agora.sample.breakoutroom.bean;

import androidx.annotation.NonNull;

public class SceneInfo {
    private @NonNull String sceneId;
    private @NonNull String ownerId;

    public SceneInfo(@NonNull String sceneId,@NonNull String ownerId) {
        this.sceneId = sceneId;
        this.ownerId = ownerId;
    }

    @NonNull
    public String getSceneId() {
        return sceneId;
    }
    @NonNull
    public String getOwnerId() {
        return ownerId;
    }
}
