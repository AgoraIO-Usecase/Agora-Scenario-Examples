package io.agora.scene.breakoutroom.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import io.agora.example.base.BaseActivity;
import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.databinding.RoomBreakoutRoomActivityMainBinding;

public class MainActivity extends BaseActivity<RoomBreakoutRoomActivityMainBinding> {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ViewModelProvider(this, new GlobalViewModelFactory(this.getApplication())).get(GlobalViewModel.class);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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
}