package io.agora.sample.rtegame.ui.splashpage;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.base.BaseFragment;
import io.agora.sample.rtegame.databinding.FragmentSplashBinding;


public class SplashFragment extends BaseFragment<FragmentSplashBinding> {
    private GlobalViewModel globalViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        globalViewModel = new ViewModelProvider(requireActivity()).get(GlobalViewModel.class);

        initListener();
    }

    private void initListener() {
        globalViewModel.initSyncManager(requireContext()).observe(getViewLifecycleOwner(), succeed -> {
            if (!succeed) showError();
            else toRoomListPage();
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
