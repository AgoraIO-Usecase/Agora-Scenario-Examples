package io.agora.syncmanager.rtm;

import java.util.HashMap;
import java.util.List;

public interface ISyncManager {
    Scene joinScene(Scene room, SyncManager.Callback callback);

    void getScenes(SyncManager.DataListCallback callback);

    void get(DocumentReference reference, SyncManager.DataItemCallback callback);

    void get(CollectionReference reference, SyncManager.DataListCallback callback);

    void add(CollectionReference reference, HashMap<String, Object> data, SyncManager.DataItemCallback callback);

    void delete(DocumentReference reference, SyncManager.Callback callback);

    void delete(CollectionReference reference, SyncManager.Callback callback);

    void update(DocumentReference reference, String key, Object data, SyncManager.DataItemCallback callback);

    void update(DocumentReference reference, HashMap<String, Object> data, SyncManager.DataItemCallback callback);

    void subscribe(DocumentReference reference, SyncManager.EventListener listener);

    void subscribe(CollectionReference reference, SyncManager.EventListener listener);

    void unsubscribe(SyncManager.EventListener listener);

    String getSceneClass();
}
