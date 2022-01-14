package io.agora.scene.rtegame.ui.splash;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseUtil;
import io.agora.scene.rtegame.GlobalViewModel;
import io.agora.scene.rtegame.R;
import io.agora.scene.rtegame.base.BaseFragment;
import io.agora.scene.rtegame.databinding.GameFragmentSplashBinding;
import io.agora.scene.rtegame.util.EventObserver;
import io.agora.scene.rtegame.util.GameUtil;


public class SplashFragment extends BaseFragment<GameFragmentSplashBinding> {
    private GlobalViewModel globalViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        globalViewModel = GameUtil.getAndroidViewModel(this);
        initListener();
    }

    private void initListener() {
        globalViewModel.isRTMInit().observe(getViewLifecycleOwner(), new EventObserver<>(succeed -> {
            if (succeed == Boolean.TRUE) toRoomListPage();
            else showError();
        }));

    }

    private void toRoomListPage() {
        findNavController().popBackStack(R.id.splashFragment, true);
        findNavController().navigate(R.id.roomListFragment);
    }

    private void showError() {
        mBinding.loadingStatus.setVisibility(View.GONE);
        mBinding.errorStatus.setVisibility(View.VISIBLE);
    }
}
