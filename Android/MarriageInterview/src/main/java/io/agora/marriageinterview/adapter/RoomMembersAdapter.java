package io.agora.marriageinterview.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Member;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.ItemRoomMembersBinding;

/**
 * 房间成员列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomMembersAdapter extends BaseRecyclerViewAdapter<Member, RoomMembersAdapter.ViewHolder> {

    public RoomMembersAdapter(@Nullable List<Member> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_room_members;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Member item = getItemData(position);
        if (item == null) {
            return;
        }
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomMembersBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
