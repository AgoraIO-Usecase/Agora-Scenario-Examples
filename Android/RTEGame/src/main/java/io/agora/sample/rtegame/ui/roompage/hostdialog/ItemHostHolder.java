package io.agora.sample.rtegame.ui.roompage.hostdialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.ItemDialogHostBinding;

public class ItemHostHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemDialogHostBinding, RoomInfo> {
    public ItemHostHolder(@NonNull ItemDialogHostBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo roomInfo, int selectedIndex) {
        if (roomInfo != null){
            mBinding.avatarItemHost.setImageResource(R.drawable.portrait01);
            mBinding.nameItemHost.setText(roomInfo.getTempUserName());
            mBinding.buttonItemHost.setOnClickListener(this::onItemClick);
        }
    }
}
