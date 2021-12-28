package io.agora.scene.rtegame.ui.room.game;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.databinding.GameDialogGameModeBinding;
import io.agora.scene.rtegame.util.GameUtil;

public class GameModeDialog extends BaseBottomSheetDialogFragment<GameDialogGameModeBinding> implements OnItemClickListener<RoomInfo> {
    public static final String TAG = "GameModeDialog";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);
        initView();
    }

    private void initView() {
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
        mBinding.btnPkFgGameMode.setOnClickListener(this::showGameListDialog);


        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void showGameListDialog(View view) {
        dismiss();
        new GameListDialog().show(getParentFragmentManager(), GameListDialog.TAG);
    }

}
