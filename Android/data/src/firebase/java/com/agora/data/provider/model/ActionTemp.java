package com.agora.data.provider.model;

import com.agora.data.model.Action;
import com.google.firebase.firestore.DocumentReference;

public class ActionTemp {
    private String objectId;
    private DocumentReference memberId;
    private DocumentReference roomId;
    private int action;
    private int status;

    public ActionTemp() {
    }

    public Action create(String objectId) {
        Action action = new Action();
        action.setObjectId(objectId);
        return action;
    }
}
