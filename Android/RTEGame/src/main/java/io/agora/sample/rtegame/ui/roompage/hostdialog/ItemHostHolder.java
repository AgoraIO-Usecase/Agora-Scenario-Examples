package io.agora.sample.rtegame.ui.roompage.hostdialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.ItemDialogHostBinding;
import io.agora.sample.rtegame.util.GameUtil;

public class ItemHostHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemDialogHostBinding, RoomInfo> {
    public ItemHostHolder(@NonNull ItemDialogHostBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo roomInfo, int selectedIndex) {
        if (roomInfo != null){
            mBinding.avatarItemHost.setImageResource(GameUtil.getBgdFromRoomBgdId(roomInfo.getBackgroundId()));
            mBinding.nameItemHost.setText(roomInfo.getTempUserName());
            mBinding.buttonItemHost.setOnClickListener(this::onItemClick);
        }
    }
}