package io.agora.sample.rtegame.ui.roompage.moredialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.databinding.DialogMoreBinding;
import io.agora.sample.rtegame.ui.roompage.RoomViewModel;
import io.agora.sample.rtegame.util.GameUtil;

/**
 * Do show this dialog using childFragmentManager
 */
public class MoreDialog extends BaseBottomSheetDialogFragment<DialogMoreBinding> {
    public static final String TAG = "MoreDialog";
    private RoomViewModel roomViewModel;



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        roomViewModel = GameUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        initView();
        initListener();
    }

    private void initView() {
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            BaseUtil.logD(inset.toString());
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
