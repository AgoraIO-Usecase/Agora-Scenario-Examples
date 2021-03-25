package com.agora.data;

import androidx.multidex.MultiDexApplication;

import com.agora.data.manager.RtcManager;

public class AgoraApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        RtcManager.Instance(this).init();
    }
}
