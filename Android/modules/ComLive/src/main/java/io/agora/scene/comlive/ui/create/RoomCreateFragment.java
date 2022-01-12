package io.agora.scene.comlive.ui.create;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseUtil;
import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.base.BaseNavFragment;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.databinding.ComLiveFragmentCreateRoomBinding;
import io.agora.scene.comlive.util.EventObserver;
import io.agora.scene.comlive.util.ComLiveUtil;

public class RoomCreateFragment extends BaseNavFragment<ComLiveFragmentCreateRoomBinding> {

    private GlobalViewModel mGlobalModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlobalModel = ComLiveUtil.getAndroidViewModel(this, GlobalViewModel.class);
        initListener();

        setupRandomName();
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPaddingRelative(inset.left,inset.top,inset.right,inset.bottom);
//            // 顶部
//            mBinding.toolbarFgCreate.setPadding(0,inset.top,0,0);
//            // 底部
//            ConstraintLayout.LayoutParams lpBtn = (ConstraintLayout.LayoutParams) mBinding.btnLiveFgCreate.getLayoutParams();
//            lpBtn.bottomMargin = inset.bottom + ((int) BaseUtil.dp2px(36));
//            mBinding.btnLiveFgCreate.setLayoutParams(lpBtn);

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
        RoomInfo roomInfo = new RoomInfo(mBinding.nameFgCreate.getText().toString(), GlobalViewModel.localUser.getUserId());
        mGlobalModel.createRoom(roomInfo);
    }

    private void onRoomInfoChanged(RoomInfo roomInfo) {
        dismissLoading();
        if (roomInfo == null) {
            BaseUtil.toast(requireContext(),"create failed");
        } else {
            findNavController().popBackStack();
        }
    }

    private void setupRandomName() {
        String currentName = ComLiveUtil.getRandomRoomName();
        mBinding.nameFgCreate.setText(currentName);
    }

    private void navigateToStartPage() {
        findNavController().popBackStack(R.id.roomListFragment, false);
    }
}
