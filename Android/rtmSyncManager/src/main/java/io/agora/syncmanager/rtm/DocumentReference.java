package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class DocumentReference {

    private String id;
    protected String parent;
    protected ISyncManager manager;

    public DocumentReference(ISyncManager manager, String parent, String id) {
        this.id = id;
        this.parent = parent;
        this.manager = manager;
    }

    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    //not supported in rtm impl
    public Query getQuery() {
        return null;
    }

    //not supported in rtm impl
    public DocumentReference query(Query mQuery) {
        return this;
    }

    public void get(Sync.DataItemCallback callback) {
        manager.get(this, callback);
    }

    public void get(String key, Sync.DataItemCallback callback) {
        manager.get(this, key, callback);
    }

    public void update(@NonNull HashMap<String, Object> data, Sync.DataItemCallback callback) {
        manager.update(this, data, callback);
    }

    public void update(String key, Object data, Sync.DataItemCallback callback) {
        manager.update(this, key, data, callback);
    }

    public void delete(Sync.Callback callback) {
        manager.delete(this, callback);
    }

    public void subscribe(Sync.EventListener listener) {
        manager.subscribe(this, listener);
    }

    public void subscribe(String key, Sync.EventListener listener) {
        manager.subscribe(this, key, listener);
    }

    public void unsubscribe(Sync.EventListener listener) {
        manager.unsubscribe(id, listener);
    }
}
