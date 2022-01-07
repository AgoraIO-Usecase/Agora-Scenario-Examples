package io.agora.scene.onelive.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

public class OneSyncEventListener implements Sync.EventListener {

    private final OnUpdateListener listener;
    private final String tag;

    public OneSyncEventListener(@NonNull String tag, @NonNull OnUpdateListener listener) {
        this.listener = listener;
        this.tag = tag;
    }

    @Override
    public void onCreated(@NonNull IObject item) {
        listener.update(item);
    }

    @Override
    public void onUpdated(@NonNull IObject item) {
        listener.update(item);
    }

    @Override
    public void onDeleted(@NonNull IObject item) {
        listener.update(item);
    }

    @Override
    public void onSubscribeError(@NonNull SyncManagerException ex) {
    }

    public interface OnUpdateListener {
        void update(@Nullable IObject iObject);
    }
}
