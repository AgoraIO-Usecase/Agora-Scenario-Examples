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
        globalViewModel = OneUtil.getAndroidViewModel(this);
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
        mBinding.btnFgSplash.setText(getString(R.string.one_error_desc, errorMsg));
        mBinding.loadingStatus.setVisibility(View.GONE);
        mBinding.errorStatus.setVisibility(View.VISIBLE);
    }
}
