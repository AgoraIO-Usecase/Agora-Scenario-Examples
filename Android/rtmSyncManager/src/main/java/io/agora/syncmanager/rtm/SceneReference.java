package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class SceneReference extends DocumentReference {

    private CollectionReference mCollectionReference;

    public SceneReference(ISyncManager manager, String parent, String id) {
        super(manager, parent, id);
    }

    public CollectionReference collection(@NonNull String collectionKey) {
        mCollectionReference = new CollectionReference(this.manager, this.parent, collectionKey);
        return mCollectionReference;
    }
}
