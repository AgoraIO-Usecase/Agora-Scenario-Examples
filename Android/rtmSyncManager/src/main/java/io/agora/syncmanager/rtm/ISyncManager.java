package io.agora.syncmanager.rtm;

import java.util.HashMap;

public interface ISyncManager {
    void joinScene(Scene room, Sync.JoinSceneCallback callback);

    void getScenes(Sync.DataListCallback callback);

    void get(DocumentReference reference, Sync.DataItemCallback callback);

    void get(CollectionReference reference, Sync.DataListCallback callback);

    void add(CollectionReference reference, HashMap<String, Object> data, Sync.DataItemCallback callback);

    void delete(DocumentReference reference, Sync.Callback callback);

    void delete(CollectionReference reference, Sync.Callback callback);

    void update(DocumentReference reference, String key, Object data, Sync.DataItemCallback callback);

    void update(DocumentReference reference, HashMap<String, Object> data, Sync.DataItemCallback callback);

    void subscribe(DocumentReference reference, Sync.EventListener listener);

    void subscribe(CollectionReference reference, Sync.EventListener listener);

    void unsubscribe(Sync.EventListener listener);

    String getSceneClass();
}
