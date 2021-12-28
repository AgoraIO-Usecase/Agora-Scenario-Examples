package io.agora.scene.rtegame.ui.room.donate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.rtegame.bean.Gift;
import io.agora.scene.rtegame.databinding.GameItemDialogGiftBinding;

public class GiftHolder extends BaseRecyclerViewAdapter.BaseViewHolder<GameItemDialogGiftBinding, Gift> {
    public GiftHolder(@NonNull GameItemDialogGiftBinding mBinding) {
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
