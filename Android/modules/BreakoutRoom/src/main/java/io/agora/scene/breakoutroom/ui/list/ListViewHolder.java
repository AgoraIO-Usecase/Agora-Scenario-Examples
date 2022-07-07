package io.agora.scene.breakoutroom.ui.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomUtil;
import io.agora.scene.breakoutroom.bean.RoomInfo;
import io.agora.scene.breakoutroom.databinding.RoomItemRoomListBinding;

public class ListViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<RoomItemRoomListBinding, RoomInfo> {

    public ListViewHolder(@NonNull RoomItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo roomInfo, int selectedIndex) {
        if (roomInfo != null) {
            mBinding.bgdItemRoomList.setImageResource(RoomUtil.getDrawableByName(roomInfo.getBackgroundId()));
            mBinding.titleItemRoomList.setText(itemView.getContext().getString(R.string.room_room_name_format, roomInfo.getId(), roomInfo.getUserId()));
        }
    }
}