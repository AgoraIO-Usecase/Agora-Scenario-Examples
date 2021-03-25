package com.agora.data;

import androidx.multidex.MultiDexApplication;

import com.agora.data.manager.RtcManager;

import cn.leancloud.AVLogger;
import cn.leancloud.AVOSCloud;
import io.agora.baselibrary.BuildConfig;

public class AgoraApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        RtcManager.Instance(this).init();

        if (BuildConfig.DEBUG) {
            AVOSCloud.setLogLevel(AVLogger.Level.DEBUG);
        } else {
            AVOSCloud.setLogLevel(AVLogger.Level.ERROR);
        }
        AVOSCloud.initialize(this, getString(R.string.leancloud_app_id),
                getString(R.string.leancloud_app_key),
                getString(R.string.leancloud_server_url));
    }
}
