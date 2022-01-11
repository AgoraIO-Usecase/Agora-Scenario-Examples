package io.agora.scene.comlive.ui.room.game;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.comlive.bean.AgoraGame;
import io.agora.scene.comlive.databinding.ComLiveDialogGameListBinding;
import io.agora.scene.comlive.databinding.ComLiveItemGameBinding;
import io.agora.scene.comlive.ui.room.RoomViewModel;
import io.agora.scene.comlive.util.ComLiveUtil;

public class GameListDialog extends BaseBottomSheetDialogFragment<ComLiveDialogGameListBinding> implements OnItemClickListener<AgoraGame>{
    public static final String TAG = "GameModeDialog";

    private RoomViewModel roomViewModel;

    private BaseRecyclerViewAdapter<ComLiveItemGameBinding, AgoraGame, ItemGameHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        roomViewModel = ComLiveUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);
        initView();
        roomViewModel.gameList.observe(getViewLifecycleOwner(), agoraGames -> mAdapter.submitListAndPurge(agoraGames));
        roomViewModel.fetchGameList();
    }

    private void initView() {
        ComLiveUtil.setBottomDialogBackground(mBinding.getRoot());
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, ItemGameHolder.class);
        mBinding.recyclerViewDialogGameList.setAdapter(mAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public void onItemClick(@NonNull AgoraGame game, @NonNull View view, int position, long viewType) {
        roomViewModel.requestStartGame(game.getGameId());
        dismiss();
    }
}
