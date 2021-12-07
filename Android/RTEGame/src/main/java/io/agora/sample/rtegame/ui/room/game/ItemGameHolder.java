package io.agora.sample.rtegame.ui.room.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.AgoraGame;
import io.agora.sample.rtegame.databinding.ItemGameBinding;

public class ItemGameHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemGameBinding, AgoraGame> {

    public ItemGameHolder(@NonNull ItemGameBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable AgoraGame data, int selectedIndex) {
        if (data == null) return;
        mBinding.getRoot().setText(data.getGameName());
        mBinding.getRoot().setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.ic_game_1,0,0);
    }
}
