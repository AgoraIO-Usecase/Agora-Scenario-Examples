package io.agora.scene.rtegame.ui.room.game;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.rtegame.bean.AgoraGame;
import io.agora.scene.rtegame.databinding.GameDialogGameListBinding;
import io.agora.scene.rtegame.databinding.GameItemGameBinding;
import io.agora.scene.rtegame.repo.GameRepo;
import io.agora.scene.rtegame.ui.room.RoomViewModel;
import io.agora.scene.rtegame.ui.room.invite.HostListDialog;
import io.agora.scene.rtegame.util.GameUtil;

public class GameListDialog extends BaseBottomSheetDialogFragment<GameDialogGameListBinding> implements OnItemClickListener<AgoraGame>{
    public static final String TAG = "GameModeDialog";

    private BaseRecyclerViewAdapter<GameItemGameBinding, AgoraGame, ItemGameHolder> mAdapter;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);
        initView();
        RoomViewModel roomViewModel = GameUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        roomViewModel.fetchGameList();
        roomViewModel.gameList.observe(getViewLifecycleOwner(), agoraGames -> mAdapter.submitListAndPurge(agoraGames));
    }

    private void initView() {
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, ItemGameHolder.class);
        mBinding.recyclerViewDialogGameList.setAdapter(mAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(requireDialog().getWindow().getDecorView());
        if (controller!=null) {
            boolean isNightMode = GameUtil.isNightMode(getResources().getConfiguration());
            controller.setAppearanceLightStatusBars(!isNightMode);
            controller.setAppearanceLightNavigationBars(!isNightMode);
        }
    }

    @Override
    public void onItemClick(@NonNull AgoraGame data, @NonNull View view, int position, long viewType) {
        showHostListDialog(data.getGameId());
    }

    private void showHostListDialog(String gameId) {
        dismiss();
        new HostListDialog(gameId).show(getParentFragmentManager(), HostListDialog.TAG);
    }

}
