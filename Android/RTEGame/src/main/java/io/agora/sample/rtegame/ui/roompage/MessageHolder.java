package io.agora.sample.rtegame.ui.roompage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.databinding.ItemRoomMessageBinding;

public class MessageHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomMessageBinding, String> {
    public MessageHolder(@NonNull ItemRoomMessageBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable String data, int selectedIndex) {
        mBinding.getRoot().setText(data);
    }
}
