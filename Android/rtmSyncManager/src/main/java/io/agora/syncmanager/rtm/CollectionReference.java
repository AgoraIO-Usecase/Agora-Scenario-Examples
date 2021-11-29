package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class CollectionReference {

    private String key;
    private String parent;
    private ISyncManager manager;

    public CollectionReference(ISyncManager manager, String parent, String key) {
        this.key = key;
        this.manager = manager;
        this.parent = parent;
    }
    //not supported in rtm impl
    public CollectionReference query(Query mQuery) {
        return this;
    }
    //not supported in rtm impl
    public Query getQuery() {
        return null;
    }

    public String getKey() {
        return key;
    }

    public String getParent() {
        return parent;
    }

    public DocumentReference document(@NonNull String id) {
        return new DocumentReference(manager, parent, id);
    }

    public void add(HashMap<String, Object> datas, Sync.DataItemCallback callback) {
        manager.add(this, datas, callback);
    }

    public void get(Sync.DataListCallback callback) {
        manager.get(this, callback);
    }

    public void delete(Sync.Callback callback) {
        manager.delete(this, callback);
    }

    public void subscribe(Sync.EventListener listener) {
        manager.subscribe(this, listener);
    }
}
