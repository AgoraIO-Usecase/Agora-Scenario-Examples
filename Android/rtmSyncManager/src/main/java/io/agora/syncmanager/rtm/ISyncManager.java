package io.agora.syncmanager.rtm;

import java.util.HashMap;

import io.agora.common.annotation.NonNull;
import io.agora.common.annotation.Nullable;

public interface ISyncManager {
    void joinScene(@NonNull String sceneId,@Nullable Sync.JoinSceneCallback callback);

    void createScene(@NonNull Scene room, @Nullable Sync.Callback callback);

    void getScenes(Sync.DataListCallback callback);

    void get(DocumentReference reference, Sync.DataItemCallback callback);

    void get(DocumentReference reference, String key, Sync.DataItemCallback callback);

    void get(CollectionReference reference, Sync.DataListCallback callback);

    void add(CollectionReference reference, HashMap<String, Object> data, Sync.DataItemCallback callback);

    void delete(DocumentReference reference, Sync.Callback callback);

    void delete(CollectionReference reference, Sync.Callback callback);

    void update(DocumentReference reference, String key, Object data, Sync.DataItemCallback callback);

    void update(DocumentReference reference, HashMap<String, Object> data, Sync.DataItemCallback callback);

    void subscribe(DocumentReference reference, Sync.EventListener listener);

    void subscribe(DocumentReference reference, String key, Sync.EventListener listener);

    void subscribe(CollectionReference reference, Sync.EventListener listener);

    void unsubscribe(String id, Sync.EventListener listener);

    void destroy();
}
