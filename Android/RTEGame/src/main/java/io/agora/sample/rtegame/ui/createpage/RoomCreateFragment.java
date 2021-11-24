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

import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.base.BaseFragment;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.FragmentCreateRoomBinding;
import io.agora.sample.rtegame.util.EventObserver;
import io.agora.sample.rtegame.util.GameUtil;

public class RoomCreateFragment extends BaseFragment<FragmentCreateRoomBinding> {

    private GlobalViewModel mGlobalModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlobalModel = GameUtil.getViewModel(requireActivity(), GlobalViewModel.class);
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
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToStartPage();
            }
        });
        mBinding.toolbarFgCreate.setNavigationOnClickListener((v) -> navigateToStartPage());
        mBinding.btnRandomFgCreate.setOnClickListener((v) -> setupRandomName());
        mBinding.btnLiveFgCreate.setOnClickListener((v) -> startLive());
        mGlobalModel.roomInfo.observe(getViewLifecycleOwner(), new EventObserver<>(this::onRoomInfoChanged));
    }

    /**
     * 开始直播
     */
    private void startLive() {
        showLoading();
        RoomInfo roomInfo = new RoomInfo(mBinding.nameFgCreate.getText().toString(), GameApplication.getInstance().user.getUserId());
        mGlobalModel.createRoom(roomInfo);
    }

    private void onRoomInfoChanged(RoomInfo roomInfo) {
        dismissLoading();
        if (roomInfo == null) {
            BaseUtil.toast(GameApplication.getInstance(),"create failed");
        } else {
            findNavController().popBackStack();
        }
    }

    private void setupRandomName() {
        String currentName = GameUtil.getRandomRoomName();
        mBinding.nameFgCreate.setText(currentName);
    }

    private void navigateToStartPage() {
        findNavController().popBackStack(R.id.roomListFragment, false);
    }
}
