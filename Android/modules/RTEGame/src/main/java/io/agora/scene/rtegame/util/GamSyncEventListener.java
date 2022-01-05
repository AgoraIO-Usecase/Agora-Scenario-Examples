package io.agora.scene.rtegame.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

public class GamSyncEventListener implements Sync.EventListener {

    private final OnUpdateListener listener;
    private final String tag;

    public GamSyncEventListener(@NonNull String tag, @NonNull OnUpdateListener listener) {
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
    public void onSubscribeError(SyncManagerException ex) {
    }

    public interface OnUpdateListener {
        void update(@Nullable IObject iObject);
    }
}
