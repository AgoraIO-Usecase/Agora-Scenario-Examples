package io.agora.sample.rtegame.bean;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.util.Random;

@Keep
public class RoomInfo {
    //    channel名,用来join rtc channel 随机生成6位数字
    private @NonNull
    final String roomId;
    //    随机生成的汉字
    private @NonNull
    final String roomName;
    //    房主id
    private @NonNull
    final String userId;
    //    背景图片id, 从本地几张图中随机分配
    private @NonNull
    final String backgroundId;

    public RoomInfo(@NonNull String roomName, String userId) {
        this( String.valueOf(new Random().nextInt(10000)), roomName, userId);
    }

    public RoomInfo(@NonNull String roomId, @NonNull String roomName, @NonNull String userId) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.userId = userId;
        this.backgroundId = "";
    }

    public RoomInfo(@NonNull String roomId, @NonNull String roomName, @NonNull String userId, @NonNull String backgroundId) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.userId = userId;
        this.backgroundId = backgroundId;
    }

    @NonNull
    public String getRoomId() {
        return roomId;
    }

    @NonNull
    public String getRoomName() {
        return roomName;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public String getTempUserName(){
        return "User-" + userId;
    }

    @NonNull
    public String getBackgroundId() {
        return backgroundId;
    }
}
