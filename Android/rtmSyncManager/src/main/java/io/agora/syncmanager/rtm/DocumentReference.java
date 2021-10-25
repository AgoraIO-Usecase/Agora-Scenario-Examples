package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class DocumentReference {

    private String id;
    private CollectionReference parent;
    private Query mQuery;

    public DocumentReference(CollectionReference parent, String id) {
        this.parent = parent;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Query getQuery() {
        return mQuery;
    }

    public DocumentReference query(Query mQuery) {
        this.mQuery = mQuery;
        return this;
    }

    public CollectionReference getParent() {
        return parent;
    }

    public void get(SyncManager.DataItemCallback callback) {
        SyncManager.Instance().get(this, callback);
    }

    public void update(@NonNull HashMap<String, Object> data, SyncManager.DataItemCallback callback) {
        SyncManager.Instance().update(this, data, callback);
    }

    public void update(String key, Object data, SyncManager.DataItemCallback callback) {
        SyncManager.Instance().update(this, key, data, callback);
    }

    public void delete(SyncManager.Callback callback) {
        SyncManager.Instance().delete(this, callback);
    }

    public void subscribe(SyncManager.EventListener listener) {
        SyncManager.Instance().subscribe(this, listener);
    }
}
