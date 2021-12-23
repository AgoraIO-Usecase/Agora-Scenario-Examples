package io.agora.scene.singlehostlive;

public class RoomInfo {
    public String roomName;
    public int userCount;
    public int bgColor;

    public RoomInfo(String roomName, int userCount, int bgColor){
        this.roomName = roomName;
        this.userCount = userCount;
        this.bgColor = bgColor;
    }

}
