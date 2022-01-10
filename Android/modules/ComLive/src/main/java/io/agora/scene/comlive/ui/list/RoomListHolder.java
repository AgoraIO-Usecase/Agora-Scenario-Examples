package io.agora.scene.comlive.ui.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.databinding.ComLiveItemRoomListBinding;
import io.agora.scene.comlive.util.ComLiveUtil;

public class RoomListHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ComLiveItemRoomListBinding, RoomInfo> {
    public RoomListHolder(@NonNull ComLiveItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo room, int selectedIndex) {
        if (room != null){
            mBinding.titleItemRoomList.setText(room.getRoomName());
            mBinding.bgdItemRoomList.setImageResource(ComLiveUtil.getBgdByRoomBgdId(room.getBackgroundId()));
        }
    }
}
