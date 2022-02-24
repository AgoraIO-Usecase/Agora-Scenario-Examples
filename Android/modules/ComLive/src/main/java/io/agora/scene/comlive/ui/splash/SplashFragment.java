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
        globalViewModel = ComLiveUtil.getAndroidViewModel(this);
        initListener();
    }

    private void initListener() {
        globalViewModel.isSDKsReady().observe(getViewLifecycleOwner(), this::checkInitResult);
    }
    private void checkInitResult(int initResult) {
        int initResultMask = 0b001010;
        if ((initResult & initResultMask) == initResultMask) {
            StringBuilder initFailedSDKString = new StringBuilder();
            if ((initResult & 0b000001) == 0)
                initFailedSDKString.append("RTM,");
            if ((initResult & 0b000100) == 0)
                initFailedSDKString.append("RTC,");

            if (initFailedSDKString.length() != 0) {
                initFailedSDKString.deleteCharAt(initFailedSDKString.length() - 1);
                showError(initFailedSDKString.toString());
            } else {
                toRoomListPage();
            }
        }
    }
    private void toRoomListPage() {
        findNavController().popBackStack(R.id.splashFragment, true);
        findNavController().navigate(R.id.roomListFragment);
    }

    private void showError(String errorMsg) {
        mBinding.btnFgSplash.setText(getString(R.string.com_live_error_desc, errorMsg));
        mBinding.loadingStatus.setVisibility(View.GONE);
        mBinding.errorStatus.setVisibility(View.VISIBLE);
    }
}
