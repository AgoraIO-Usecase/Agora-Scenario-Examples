package io.agora.scene.onelive;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.onelive.databinding.OneActivityMainBinding;
import io.agora.scene.onelive.repo.GameRepo;
import io.agora.scene.onelive.util.OneUtil;
import io.agora.syncmanager.rtm.Sync;

public class MainActivity extends BaseActivity<OneActivityMainBinding> {

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        GameRepo.X_LC_ID = getString(R.string.x_lc_Id);
        GameRepo.X_LC_KEY = getString(R.string.x_lc_Key);
        GameRepo.X_LC_SESSION = getString(R.string.x_lc_Session);

        OneUtil.getAndroidViewModel(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }
}