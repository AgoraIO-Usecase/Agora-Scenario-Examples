package io.agora.sample.rtegame;

import android.app.Application;

import io.agora.sample.rtegame.bean.LocalUser;

public class GameApplication extends Application {
    public LocalUser user;
    private static GameApplication instance;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static GameApplication getInstance() {
        return instance;
    }
}