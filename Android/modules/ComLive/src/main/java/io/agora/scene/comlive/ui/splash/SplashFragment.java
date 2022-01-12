package io.agora.scene.comlive.ui.splash;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.base.BaseNavFragment;
import io.agora.scene.comlive.databinding.ComLiveFragmentSplashBinding;
import io.agora.scene.comlive.util.EventObserver;
import io.agora.scene.comlive.util.ComLiveUtil;


public class SplashFragment extends BaseNavFragment<ComLiveFragmentSplashBinding> {
    private GlobalViewModel globalViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        globalViewModel = ComLiveUtil.getAndroidViewModel(this, GlobalViewModel.class);

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
