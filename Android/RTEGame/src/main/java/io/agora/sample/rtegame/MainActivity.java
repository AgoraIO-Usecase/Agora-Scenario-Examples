package io.agora.sample.rtegame;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;

import io.agora.example.base.BaseActivity;
import io.agora.example.base.BaseUtil;
import io.agora.rtc2.RtcEngine;
import io.agora.sample.rtegame.databinding.ActivityMainBinding;
import io.agora.syncmanager.rtm.Sync;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setBackgroundDrawable(null);
    }

    @Override
    public void finish() {
        super.finish();
        BaseUtil.logD("finish");
//        RTMDestroy
        Sync.Instance().destroy();
//        RTCDestroy
        RtcEngine.destroy();
    }

}