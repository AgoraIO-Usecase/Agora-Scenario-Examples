package io.agora.sample.rtegame.ui.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.databinding.ItemRoomMessageBinding;

public class MessageHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomMessageBinding, CharSequence> {
    public MessageHolder(@NonNull ItemRoomMessageBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable CharSequence data, int selectedIndex) {
        mBinding.getRoot().setText(data);
    }
}
