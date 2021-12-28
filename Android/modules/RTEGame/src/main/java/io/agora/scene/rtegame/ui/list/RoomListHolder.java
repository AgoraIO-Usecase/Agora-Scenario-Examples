package io.agora.scene.rtegame.ui.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.databinding.GameItemRoomListBinding;
import io.agora.scene.rtegame.util.GameUtil;

public class RoomListHolder extends BaseRecyclerViewAdapter.BaseViewHolder<GameItemRoomListBinding, RoomInfo> {
    public RoomListHolder(@NonNull GameItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo room, int selectedIndex) {
        if (room != null){
            mBinding.titleItemRoomList.setText(room.getRoomName());
            mBinding.bgdItemRoomList.setImageResource(GameUtil.getBgdByRoomBgdId(room.getBackgroundId()));
        }
    }
}
