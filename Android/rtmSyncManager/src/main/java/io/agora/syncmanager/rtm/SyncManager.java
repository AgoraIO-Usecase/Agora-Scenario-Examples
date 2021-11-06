package io.agora.syncmanager.rtm;

import android.content.Context;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.syncmanager.rtm.impl.DataSyncImpl;

/**
 * 房间状态同步
 */
public final class SyncManager implements ISyncManager {

    private volatile static SyncManager instance;

    private static GsonConverter mConverter = new GsonConverter();

    private SyncManager() {
    }

    public static SyncManager Instance() {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null)
                    instance = new SyncManager();
            }
        }
        return instance;
    }

    private ISyncManager mISyncManager;

    public void init(Context context, Map<String, String> params) {
        mISyncManager = new DataSyncImpl(context, params);
        mConverter = new GsonConverter();
    }

    public static GsonConverter getConverter() {
        return mConverter;
    }

    public static void setConverter(GsonConverter mConverter) {
        SyncManager.mConverter = mConverter;
    }

    public SceneReference getScene(String id) {
        return new SceneReference(id, mISyncManager.getSceneClass());
    }

    public CollectionReference collection(String key) {
        return new CollectionReference(null, key);
    }

    @Override
    public Scene joinScene(Scene room, SyncManager.Callback callback) {
        return mISyncManager.joinScene(room, callback);
    }

    @Override
    public void getScenes(SyncManager.DataListCallback callback) {
        mISyncManager.getScenes(callback);
    }

    @Override
    public void get(DocumentReference reference, DataItemCallback callback) {
        mISyncManager.get(reference, callback);
    }

    @Override
    public void get(CollectionReference reference, DataListCallback callback) {
        mISyncManager.get(reference, callback);
    }

    @Override
    public void add(CollectionReference reference, HashMap<String, Object> datas, DataItemCallback callback) {
        mISyncManager.add(reference, datas, callback);
    }

    @Override
    public void delete(DocumentReference reference, Callback callback) {
        mISyncManager.delete(reference, callback);
    }

    @Override
    public void delete(CollectionReference reference, Callback callback) {
        mISyncManager.delete(reference, callback);
    }

    @Override
    public void update(DocumentReference reference, String key, Object data, DataItemCallback callback) {
        mISyncManager.update(reference, key, data, callback);
    }

    @Override
    public void update(DocumentReference reference, HashMap<String, Object> datas, DataItemCallback callback) {
        mISyncManager.update(reference, datas, callback);
    }

    @Override
    public void subscribe(DocumentReference reference, EventListener listener) {
        mISyncManager.subscribe(reference, listener);
    }

    @Override
    public void subscribe(CollectionReference reference, EventListener listener) {
        mISyncManager.subscribe(reference, listener);
    }

    @Override
    public void unsubscribe(EventListener listener) {
        mISyncManager.unsubscribe(listener);
    }

    @Override
    public String getSceneClass() {
        return null;
    }

    public interface EventListener {
        void onCreated(IObject item);

        void onUpdated(IObject item);

        void onDeleted(IObject item);

        void onSubscribeError(SyncManagerException ex);
    }

    public interface Callback {
        void onSuccess();

        void onFail(SyncManagerException exception);
    }

    public interface DataItemCallback {
        void onSuccess(IObject result);

        void onFail(SyncManagerException exception);
    }

    public interface DataListCallback {
        void onSuccess(List<IObject> result);

        void onFail(SyncManagerException exception);
    }
}
