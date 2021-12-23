package io.agora.scene.rtegame.ui.room.game;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.rtegame.bean.AgoraGame;
import io.agora.scene.rtegame.databinding.GameDialogGameListBinding;
import io.agora.scene.rtegame.databinding.GameItemGameBinding;
import io.agora.scene.rtegame.repo.GameRepo;
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
    }

    private void initView() {
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
        mAdapter = new BaseRecyclerViewAdapter<>(fetchAllGameList(), this, ItemGameHolder.class);
        mBinding.recyclerViewDialogGameList.setAdapter(mAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public void onItemClick(@NonNull AgoraGame data, @NonNull View view, int position, long viewType) {
        showHostListDialog(data.getGameId());
    }

    private void showHostListDialog(Integer gameId) {
        dismiss();
        new HostListDialog(gameId).show(getParentFragmentManager(), HostListDialog.TAG);
    }

    private List<AgoraGame> fetchAllGameList(){
        List<AgoraGame> gameList = new ArrayList<>();
        gameList.add(GameRepo.getGameDetail(1));
        return gameList;
    }

}
