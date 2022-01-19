package io.agora.scene.rtegame;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;

import io.agora.example.base.BaseActivity;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.rtegame.databinding.GameActivityMainBinding;
import io.agora.scene.rtegame.repo.GameRepo;
import io.agora.scene.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.Sync;

public class MainActivity extends BaseActivity<GameActivityMainBinding> {

    private GlobalViewModel globalViewModel;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        globalViewModel = GameUtil.getAndroidViewModel(this);
        super.onCreate(savedInstanceState);

        GameRepo.X_LC_ID = getString(R.string.x_lc_Id);
        GameRepo.X_LC_KEY = getString(R.string.x_lc_Key);
        GameRepo.X_LC_SESSION = getString(R.string.x_lc_Session);

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