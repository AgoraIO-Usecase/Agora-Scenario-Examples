package io.agora.syncmanager.rtm;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class Scene implements Parcelable {
    public static final String TABLE_NAME = "AGORA_ROOM";

    public static final String COLUMN_ID = "objectId";
    public static final String COLUMN_OWNERID = "userId";
    public static final String COLUMN_NAME = "channelName";
    public static final String COLUMN_COVER = "cover";
    public static final String COLUMN_MV = "mv";
    public static final String COLUMN_CREATEDAT = "createdAt";

    private String id;
    private String channelName;
    private String userId;
    private String cover;
    private String mv;
    private Date createdAt;

    public Scene() {
    }

    protected Scene(Parcel in) {
        id = in.readString();
        channelName = in.readString();
        userId = in.readString();
        cover = in.readString();
        mv = in.readString();
        createdAt = (Date) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(channelName);
        dest.writeString(userId);
        dest.writeString(cover);
        dest.writeString(mv);
        dest.writeSerializable(createdAt);
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_OWNERID, userId);
        datas.put(COLUMN_NAME, channelName);
        datas.put(COLUMN_COVER, cover);
        datas.put(COLUMN_MV, mv);
        return datas;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Scene> CREATOR = new Creator<Scene>() {
        @Override
        public Scene createFromParcel(Parcel in) {
            return new Scene(in);
        }

        @Override
        public Scene[] newArray(int size) {
            return new Scene[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getMv() {
        return mv;
    }

    public void setMv(String mv) {
        this.mv = mv;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void radomCover() {
        int value = new Random().nextInt(8) + 1;
        cover = String.valueOf(value);
    }

    public void radomMV() {
        int value = new Random().nextInt(5) + 1;
        mv = String.valueOf(value);
    }

    @Override
    public String toString() {
        return "Scene{" +
                "id='" + id + '\'' +
                ", channelName='" + channelName + '\'' +
                ", userId='" + userId + '\'' +
                ", cover='" + cover + '\'' +
                ", mv='" + mv + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scene scene = (Scene) o;

        return id.equals(scene.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
