package io.agora.example;

import android.app.Application;

public class BaseApplication extends Application {
    private static BaseApplication instance;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = new BaseApplication();
    }

    public static BaseApplication getInstance() {
        return instance;
    }
}
