package io.agora.scene;

import android.app.Application;
import android.text.TextUtils;

import com.tencent.bugly.crashreport.CrashReport;

public class AppApplication extends Application {
    private static final String BUGLY_APP_ID = "6bc53b5895";

    @Override
    public void onCreate() {
        super.onCreate();
        if (!TextUtils.isEmpty(BUGLY_APP_ID)) {
            CrashReport.initCrashReport(getApplicationContext(), BUGLY_APP_ID, BuildConfig.DEBUG);
        }
    }

}
