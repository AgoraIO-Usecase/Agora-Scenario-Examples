package io.agora.sample.breakoutroom.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import io.agora.example.base.BaseActivity;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity<ActivityMainBinding> {
    private MainViewModel mainViewModel;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        mainViewModel.initSyncManager(this);
    }

    @Override
    public void onBackPressed() {
        NavDestination dest = Navigation.findNavController(mBinding.navMain).getCurrentDestination();
        if (dest != null && dest.getId() == R.id.roomListFragment) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainViewModel.destroySyncManager();
    }
}