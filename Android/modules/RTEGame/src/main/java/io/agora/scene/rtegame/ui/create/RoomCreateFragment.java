package io.agora.scene.rtegame.ui.create;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseUtil;
import io.agora.scene.rtegame.GlobalViewModel;
import io.agora.scene.rtegame.R;
import io.agora.scene.rtegame.base.BaseFragment;
import io.agora.scene.rtegame.databinding.GameFragmentCreateRoomBinding;
import io.agora.scene.rtegame.util.EventObserver;
import io.agora.scene.rtegame.util.GameUtil;

public class RoomCreateFragment extends BaseFragment<GameFragmentCreateRoomBinding> {

    private CreateViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = GameUtil.getViewModel(CreateViewModel.class, this);
        initListener();

        setupRandomName();
        mViewModel.startPreview(mBinding.cameraPreviewFgCreate);
        ObjectAnimator.ofFloat(mBinding.cameraPreviewFgCreate, View.SCALE_X, 0.5f, 1f).setDuration(600L).start();
        ObjectAnimator.ofFloat(mBinding.cameraPreviewFgCreate, View.SCALE_Y, 0.5f, 1f).setDuration(600L).start();
    }

    @Override
    public void onDestroyView() {
        if (GlobalViewModel.currentRoom == null)
            mViewModel.stopPreview();
        super.onDestroyView();
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            // 顶部
            mBinding.toolbarFgCreate.setPadding(0, inset.top, 0, 0);
//            // 底部
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

        mViewModel.isRoomCreateSuccess().observe(getViewLifecycleOwner(), new EventObserver<>(this::onRoomInfoChanged));
    }

    /**
     * 开始直播
     */
    private void startLive() {
        showLoading();
        mViewModel.createRoom(mBinding.nameFgCreate.getText().toString());
    }

    private void onRoomInfoChanged(Boolean res) {
        dismissLoading();
        if (res != Boolean.TRUE) {
            BaseUtil.toast(requireContext(), "create failed");
        } else {
            // 创建成功，需要进入房间，在另外一个SurfaceView中预览
            mViewModel.stopPreview();
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
