package io.agora.scene.comlive.ui.room.tool;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseUtil;
import io.agora.scene.comlive.databinding.ComLiveDialogMoreBinding;
import io.agora.scene.comlive.ui.room.RoomViewModel;
import io.agora.scene.comlive.util.ComLiveUtil;

/**
 * Do show this dialog using childFragmentManager
 */
public class MoreDialog extends BaseBottomSheetDialogFragment<ComLiveDialogMoreBinding> {
    public static final String TAG = "MoreDialog";
    private RoomViewModel roomViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        roomViewModel = ComLiveUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        initView();
        initListener();
    }

    private void initView() {
        ComLiveUtil.setBottomDialogBackground(mBinding.getRoot());
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        // 翻转摄像头
        mBinding.btnFlipCameraDialogMore.setOnClickListener(v -> roomViewModel.flipCamera());

        // 开启/关闭摄像头
        mBinding.btnCameraDialogMore.addOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed())
                roomViewModel.enableCamera(isChecked);
        });
        // 开启/关闭麦克风
        mBinding.btnMicDialogMore.addOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed())
                roomViewModel.enableMic(isChecked);
        });

        // Update Status
        roomViewModel.isCameraEnabled.observe(getViewLifecycleOwner(), enabled -> mBinding.btnCameraDialogMore.setChecked(enabled));
        roomViewModel.isMicEnabled.observe(getViewLifecycleOwner(), enabled -> {
            mBinding.btnMicDialogMore.setChecked(enabled);
        });
    }
}
