package io.agora.syncmanager.rtm;

import android.content.Context;


import java.util.List;
import java.util.Map;

import io.agora.syncmanager.rtm.impl.DataSyncImpl;

/**
 * 房间状态同步
 */
public final class Sync {

    private volatile static Sync instance;

    private Sync() {
    }

    public static Sync Instance() {
        if (instance == null) {
            synchronized (Sync.class) {
                if (instance == null)
                    instance = new Sync();
            }
        }
        return instance;
    }

    private ISyncManager mISyncManager;

    public void init(Context context, Map<String, String> params, Sync.Callback callback) {
        mISyncManager = new DataSyncImpl(context, params, callback);
    }

    public void joinScene(Scene room, Sync.JoinSceneCallback callback) {
        mISyncManager.joinScene(room, callback);
    }

    public void getScenes(Sync.DataListCallback callback) {
        mISyncManager.getScenes(callback);
    }

    public interface EventListener {
        void onCreated(IObject item);

        void onUpdated(IObject item);

        void onDeleted(IObject item);

        void onSubscribeError(SyncManagerException ex);
    }

    public interface JoinSceneCallback {
        void onSuccess(SceneReference sceneReference);

        void onFail(SyncManagerException exception);
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
//    public static  interface SyncCallback<T>{
//        void onSuccess(T resData);
//
//        void onFail(SyncManagerException exception);
//
//    }
}
