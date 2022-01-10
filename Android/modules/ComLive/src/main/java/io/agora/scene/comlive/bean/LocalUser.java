package io.agora.scene.comlive.bean;

import androidx.annotation.NonNull;

import java.util.Random;

import io.agora.scene.comlive.util.ComLiveUtil;

public class LocalUser {
    //    随机Int转string获得, 用来作为加rtc的uid
    private @NonNull
    final String userId;

    //    格式为 "User-"+id
    private @NonNull
    final String name;

    private @NonNull
    final String avatar;

    public LocalUser() {
        this(String.valueOf(new Random().nextInt(10000)));
    }

    public LocalUser(@NonNull String userId) {
        this.userId = userId;
        this.name = "User-" + userId;
        this.avatar = ComLiveUtil.randomAvatar();
    }

    @NonNull
    public String getAvatar() {
        return avatar;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "LocalUser{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
