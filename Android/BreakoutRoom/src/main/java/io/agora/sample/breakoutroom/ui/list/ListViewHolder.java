package io.agora.sample.breakoutroom.ui.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.databinding.ItemRoomListBinding;

public class ListViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomListBinding, RoomInfo> {

    public ListViewHolder(@NonNull ItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable RoomInfo roomInfo, int selectedIndex) {
        if (roomInfo != null) {
            mBinding.bgdItemRoomList.setImageResource(RoomUtil.getDrawableByName(roomInfo.getBackgroundId()));
            mBinding.titleItemRoomList.setText(itemView.getContext().getString(R.string.room_name_format, roomInfo.getId(), roomInfo.getUserId()));
        }
    }
}