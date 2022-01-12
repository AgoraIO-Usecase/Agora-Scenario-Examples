package io.agora.scene.comlive;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.comlive.databinding.ComLiveActivityMainBinding;
import io.agora.scene.comlive.repo.GameRepo;
import io.agora.syncmanager.rtm.Sync;

public class MainActivity extends BaseActivity<ComLiveActivityMainBinding> {

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GameRepo.X_LC_ID = getString(R.string.x_lc_Id);
        GameRepo.X_LC_KEY = getString(R.string.x_lc_Key);
        GameRepo.X_LC_SESSION = getString(R.string.x_lc_Session);

        new ViewModelProvider(this, new GlobalViewModelFactory(this.getApplication())).get(GlobalViewModel.class);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
}