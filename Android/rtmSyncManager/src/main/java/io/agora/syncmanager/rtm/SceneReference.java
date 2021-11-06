package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class SceneReference extends DocumentReference {

    private CollectionReference mCollectionReference;

    public SceneReference(String id, String className) {
        super(new CollectionReference(null, className), id);
    }

    public CollectionReference collection(@NonNull String collectionKey) {
        mCollectionReference = new CollectionReference(this, collectionKey);
        return mCollectionReference;
    }
}
