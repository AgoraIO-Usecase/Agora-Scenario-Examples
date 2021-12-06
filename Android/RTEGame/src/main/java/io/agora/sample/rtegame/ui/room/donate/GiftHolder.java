package io.agora.sample.rtegame.ui.room.donate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.bean.Gift;
import io.agora.sample.rtegame.databinding.ItemDialogGiftBinding;

public class GiftHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemDialogGiftBinding, Gift> {
    public GiftHolder(@NonNull ItemDialogGiftBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable Gift gift, int selectedIndex) {
        if (gift != null){
            mBinding.getRoot().setIconResource(gift.iconRes);
            mBinding.getRoot().setText(gift.getName());
        }
    }
}
