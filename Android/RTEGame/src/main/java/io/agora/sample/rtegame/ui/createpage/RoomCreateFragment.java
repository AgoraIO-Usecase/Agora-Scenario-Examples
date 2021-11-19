package io.agora.sample.rtegame.ui.createpage;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.FragmentCreateRoomBinding;
import io.agora.sample.rtegame.util.GameUtil;

public class RoomCreateFragment extends BaseFragment<FragmentCreateRoomBinding> {

    private GlobalViewModel mGlobalModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlobalModel = new ViewModelProvider(requireActivity()).get(GlobalViewModel.class);
        initListener();

        setupRandomName();
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 顶部
            mBinding.toolbarFgCreate.setPadding(0,inset.top,0,0);
            // 底部
            ConstraintLayout.LayoutParams lpBtn = (ConstraintLayout.LayoutParams) mBinding.btnLiveFgCreate.getLayoutParams();
            lpBtn.bottomMargin = inset.bottom + ((int) BaseUtil.dp2px(36));
            mBinding.btnLiveFgCreate.setLayoutParams(lpBtn);

            return WindowInsetsCompat.CONSUMED;
        });
        // 监听"返回键"
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToStartPage();
            }
        });
        mBinding.toolbarFgCreate.setNavigationOnClickListener((v) -> navigateToStartPage());
        mBinding.btnRandomFgCreate.setOnClickListener((v) -> setupRandomName());
        mBinding.btnLiveFgCreate.setOnClickListener((v) -> startLive());
    }

    /**
     * 开始直播
     */
    private void startLive() {
        showLoading();
        RoomInfo roomInfo = new RoomInfo(mBinding.nameFgCreate.getText().toString(), "");
        mGlobalModel.createRoom(roomInfo).observe(getViewLifecycleOwner(), this::onRoomInfoChanged);
    }

    private void onRoomInfoChanged(RoomInfo roomInfo) {
        dismissLoading();
        if (roomInfo == null) {
            BaseUtil.toast("create failed");
        } else {
            Navigation.findNavController(mBinding.getRoot()).popBackStack();
        }
    }

    private void setupRandomName() {
        String currentName = GameUtil.getRandomRoomName();
        mBinding.nameFgCreate.setText(currentName);
    }

    private void navigateToStartPage() {
        NavHostFragment.findNavController(this).popBackStack(R.id.roomListFragment, false);
//        NavHostFragment.findNavController(this).navigate(R.id.action_roomCreateFragment_to_roomListFragment);
    }
}
