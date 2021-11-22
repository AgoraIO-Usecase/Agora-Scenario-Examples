package io.agora.sample.rtegame.ui.splashpage;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.base.BaseFragment;
import io.agora.sample.rtegame.databinding.FragmentSplashBinding;
import io.agora.sample.rtegame.util.EventObserver;


public class SplashFragment extends BaseFragment<FragmentSplashBinding> {
    private GlobalViewModel globalViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        globalViewModel = new ViewModelProvider(requireActivity()).get(GlobalViewModel.class);

        initListener();
        globalViewModel.initSyncManager(requireContext());
    }

    private void initListener() {
        globalViewModel.isRTMInit().observe(getViewLifecycleOwner(), succeed -> {
            if (succeed == Boolean.TRUE) toRoomListPage();
            else showError();
        });
    }

    private void toRoomListPage(){
        findNavController().popBackStack(R.id.splashFragment, true);
        findNavController().navigate(R.id.roomListFragment);
    }

    private void showError(){
        mBinding.loadingStatus.setVisibility(View.GONE);
        mBinding.errorStatus.setVisibility(View.VISIBLE);
    }
}
