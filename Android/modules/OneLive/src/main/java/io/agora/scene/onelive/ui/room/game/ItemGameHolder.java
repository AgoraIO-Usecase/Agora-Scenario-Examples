package io.agora.scene.onelive.ui.room.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.onelive.R;
import io.agora.scene.onelive.bean.AgoraGame;
import io.agora.scene.onelive.databinding.OneItemGameBinding;

public class ItemGameHolder extends BaseRecyclerViewAdapter.BaseViewHolder<OneItemGameBinding, AgoraGame> {

    public ItemGameHolder(@NonNull OneItemGameBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable AgoraGame data, int selectedIndex) {
        if (data == null) return;
        mBinding.getRoot().setText(data.getGameName());
        mBinding.getRoot().setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.one_ic_game_1,0,0);
    }
}
