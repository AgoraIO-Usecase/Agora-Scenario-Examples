package com.agora.data.provider.model;

import com.agora.data.model.Room;
import com.google.firebase.firestore.DocumentReference;

public class RoomTemp {
    private String channelName;
    private DocumentReference anchorId;

    public RoomTemp() {
    }

    public Room createRoom(String objectId) {
        Room room = new Room();
        room.setObjectId(objectId);
        room.setChannelName(channelName);
        return room;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public DocumentReference getAnchorId() {
        return anchorId;
    }

    public void setAnchorId(DocumentReference anchorId) {
        this.anchorId = anchorId;
    }
}
