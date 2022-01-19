package io.agora.scene.onelive.ui.room.game;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.onelive.bean.AgoraGame;
import io.agora.scene.onelive.databinding.OneDialogGameListBinding;
import io.agora.scene.onelive.databinding.OneItemGameBinding;
import io.agora.scene.onelive.ui.room.RoomViewModel;
import io.agora.scene.onelive.util.OneUtil;

public class GameListDialog extends BaseBottomSheetDialogFragment<OneDialogGameListBinding> implements OnItemClickListener<AgoraGame>{
    public static final String TAG = "GameModeDialog";

    @Nullable
    private RoomViewModel roomViewModel;
    private BaseRecyclerViewAdapter<OneItemGameBinding, AgoraGame, ItemGameHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        roomViewModel = OneUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);
        initView();
        roomViewModel.fetchGameList();
        roomViewModel.gameList.observe(getViewLifecycleOwner(), agoraGames -> mAdapter.submitListAndPurge(agoraGames));
    }

    private void initView() {
        OneUtil.setBottomDialogBackground(mBinding.getRoot());
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, ItemGameHolder.class);
        mBinding.recyclerViewDialogGameList.setAdapter(mAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(mBinding.getRoot());
        if (controller != null) {
            controller.setAppearanceLightNavigationBars(true);
        }
    }

    @Override
    public void onItemClick(@NonNull AgoraGame data, @NonNull View view, int position, long viewType) {
        if (roomViewModel != null) {
            roomViewModel.requestStartGame(data);
            dismiss();
        }
    }

}
