package io.agora.scene.breakoutroom.ui.splah;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.base.BaseNavFragment;
import io.agora.scene.breakoutroom.databinding.RoomFragmentSplashBinding;
import io.agora.scene.breakoutroom.ui.MainViewModel;

public class SplashFragment extends BaseNavFragment<RoomFragmentSplashBinding> {
    private MainViewModel mainViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        initListener();
    }

    private void initListener() {
        mainViewModel.isRTMInit().observe(getViewLifecycleOwner(), initSucceed -> {
            if (initSucceed == Boolean.TRUE) toRoomListPage();
            else showError();
        });
    }

    private void toRoomListPage(){
        findNavController().navigate(R.id.action_splashFragment_to_roomListFragment);
    }

    private void showError(){
        mBinding.loadingStatus.setVisibility(View.GONE);
        mBinding.errorStatus.setVisibility(View.VISIBLE);
    }
}
