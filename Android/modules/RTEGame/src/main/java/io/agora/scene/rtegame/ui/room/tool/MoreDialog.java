package io.agora.scene.rtegame.ui.room.tool;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.scene.rtegame.databinding.GameDialogMoreBinding;
import io.agora.scene.rtegame.ui.room.RoomViewModel;
import io.agora.scene.rtegame.util.GameUtil;

/**
 * Do show this dialog using childFragmentManager
 */
public class MoreDialog extends BaseBottomSheetDialogFragment<GameDialogMoreBinding> {
    public static final String TAG = "MoreDialog";
    private RoomViewModel roomViewModel;



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        roomViewModel = GameUtil.getViewModel(RoomViewModel.class, requireParentFragment());
        initView();
        initListener();
    }

    private void initView() {
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
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
        mBinding.btnCameraDialogMore.setChecked(!roomViewModel.isLocalVideoMuted);
        mBinding.btnCameraDialogMore.addOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed())
                roomViewModel.muteLocalVideoStream(!isChecked);
        });
        // 开启/关闭麦克风
        mBinding.btnMicDialogMore.setChecked(!roomViewModel.isLocalMicMuted);
        mBinding.btnMicDialogMore.addOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed())
                roomViewModel.muteLocalAudioStream(!isChecked);
        });
    }
}
