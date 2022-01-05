package io.agora.scene.onelive.ui.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.scene.onelive.bean.RoomInfo;
import io.agora.scene.onelive.databinding.OneItemRoomListBinding;
import io.agora.scene.onelive.util.OneUtil;

public class RoomListHolder extends BaseRecyclerViewAdapter.BaseViewHolder<OneItemRoomListBinding, RoomInfo> {
    public RoomListHolder(@NonNull OneItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo room, int selectedIndex) {
        if (room != null){
            mBinding.titleItemRoomList.setText(room.getRoomName());
            mBinding.bgdItemRoomList.setImageTintList(BaseUtil.getScrimColorSelector(itemView.getContext()));
            mBinding.bgdItemRoomList.setImageResource(OneUtil.getBgdByRoomBgdId(room.getBackgroundId()));
        }
    }
}
