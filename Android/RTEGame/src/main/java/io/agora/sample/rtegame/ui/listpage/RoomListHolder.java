package io.agora.sample.rtegame.ui.listpage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.ItemRoomListBinding;
import io.agora.sample.rtegame.util.GameUtil;

public class RoomListHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomListBinding, RoomInfo> {
    public RoomListHolder(@NonNull ItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo room, int selectedIndex) {
        if (room != null){
            mBinding.titleItemRoomList.setText(room.getRoomName());
            mBinding.bgdItemRoomList.setImageResource(GameUtil.getBgdFromRoomBgdId(room.getBackgroundId()));
        }
    }
}
