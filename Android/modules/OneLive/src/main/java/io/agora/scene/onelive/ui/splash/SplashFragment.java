package io.agora.scene.onelive.ui.splash;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.scene.onelive.GlobalViewModel;
import io.agora.scene.onelive.R;
import io.agora.scene.onelive.base.BaseNavFragment;
import io.agora.scene.onelive.databinding.OneFragmentSplashBinding;
import io.agora.scene.onelive.util.EventObserver;
import io.agora.scene.onelive.util.OneUtil;


public class SplashFragment extends BaseNavFragment<OneFragmentSplashBinding> {
    private GlobalViewModel globalViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        globalViewModel = OneUtil.getAndroidViewModel(this, GlobalViewModel.class);

        initListener();
    }

    private void initListener() {
        globalViewModel.isRTMInit().observe(getViewLifecycleOwner(), new EventObserver<>(succeed -> {
            if (succeed == Boolean.TRUE) toRoomListPage();
            else showError();
        }));

        mBinding.btnFgSplash.setOnClickListener(v -> globalViewModel.tryReInitSyncManager(requireContext()));

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
