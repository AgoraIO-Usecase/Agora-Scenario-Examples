package io.agora.scene.onelive;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.onelive.databinding.OneActivityMainBinding;
import io.agora.scene.onelive.util.OneUtil;
import io.agora.syncmanager.rtm.Sync;

public class MainActivity extends BaseActivity<OneActivityMainBinding> {

    private GlobalViewModel globalViewModel;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        globalViewModel = OneUtil.getViewModel(this, GlobalViewModel.class);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public void finish() {
        super.finish();

        new Thread(() -> {
//        RTMDestroy
            Sync.Instance().destroy();
//        RTCDestroy
            RtcEngine.destroy();
        }).start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (globalViewModel != null)
            globalViewModel.focused.setValue(hasFocus);
    }
}