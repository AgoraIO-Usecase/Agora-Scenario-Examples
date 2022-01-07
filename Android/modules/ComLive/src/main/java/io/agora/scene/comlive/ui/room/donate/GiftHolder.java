package io.agora.scene.comlive.ui.room.donate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.comlive.bean.Gift;
import io.agora.scene.comlive.databinding.ComLiveItemDialogGiftBinding;

public class GiftHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ComLiveItemDialogGiftBinding, Gift> {
    public GiftHolder(@NonNull ComLiveItemDialogGiftBinding mBinding) {
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
