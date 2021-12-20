package io.agora.scene.rtegame.ui.room.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.rtegame.R;
import io.agora.scene.rtegame.bean.AgoraGame;
import io.agora.scene.rtegame.databinding.GameItemGameBinding;

public class ItemGameHolder extends BaseRecyclerViewAdapter.BaseViewHolder<GameItemGameBinding, AgoraGame> {

    public ItemGameHolder(@NonNull GameItemGameBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable AgoraGame data, int selectedIndex) {
        if (data == null) return;
        mBinding.getRoot().setText(data.getGameName());
        mBinding.getRoot().setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.game_ic_game_1,0,0);
    }
}
