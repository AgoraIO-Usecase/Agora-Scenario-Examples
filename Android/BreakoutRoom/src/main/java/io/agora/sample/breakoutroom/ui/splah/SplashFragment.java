package io.agora.sample.breakoutroom.ui.splah;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import io.agora.example.base.BaseFragment;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.databinding.FragmentSplashBinding;
import io.agora.sample.breakoutroom.ui.MainViewModel;

public class SplashFragment extends BaseFragment<FragmentSplashBinding> {
    private MainViewModel mainViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        initListener();
    }

    private void initListener() {
        mainViewModel.isRTMInit().observe(getViewLifecycleOwner(), aBoolean -> {
            if (!aBoolean) showError();
            else toRoomListPage();
        });
    }

    private void toRoomListPage(){
        Navigation.findNavController(mBinding.getRoot()).navigate(R.id.action_splashFragment_to_roomListFragment);
    }

    private void showError(){
        mBinding.loadingStatus.setVisibility(View.GONE);
        mBinding.errorStatus.setVisibility(View.VISIBLE);
    }
}
