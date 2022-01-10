package io.agora.scene.comlive.ui.room.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.bean.AgoraGame;
import io.agora.scene.comlive.databinding.ComLiveItemGameBinding;

public class ItemGameHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ComLiveItemGameBinding, AgoraGame> {

    public ItemGameHolder(@NonNull ComLiveItemGameBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable AgoraGame data, int selectedIndex) {
        if (data == null) return;
        mBinding.getRoot().setText(data.getGameName());
        mBinding.getRoot().setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.com_live_ic_game_1,0,0);
    }
}
