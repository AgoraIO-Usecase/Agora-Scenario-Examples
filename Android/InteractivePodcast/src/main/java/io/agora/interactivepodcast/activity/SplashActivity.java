package io.agora.interactivepodcast.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.interactivepodcast.R;
import io.agora.interactivepodcast.databinding.LivecastActivitySplashBinding;

/**
 * 闪屏界面
 *
 * @author chenhengfei@agora.io
 */
public class SplashActivity extends DataBindBaseActivity<LivecastActivitySplashBinding> {

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.livecast_activity_splash;
    }

    @Override
    protected void iniView() {

    }

    @Override
    protected void iniListener() {

    }

    @Override
    protected void iniData() {
        startActivity(new Intent(this, RoomListActivity.class));
        finish();
    }
}
