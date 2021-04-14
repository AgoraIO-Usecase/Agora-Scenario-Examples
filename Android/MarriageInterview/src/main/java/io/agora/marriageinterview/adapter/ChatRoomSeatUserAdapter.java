package io.agora.marriageinterview.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.agora.marriageinterview.R;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.databinding.ItemRoomSeatUserBinding;
import com.agora.data.model.Member;

/**
 * 房间上坐用户
 *
 * @author chenhengfei@agora.io
 */
public class ChatRoomSeatUserAdapter extends BaseRecyclerViewAdapter<Member, ChatRoomSeatUserAdapter.ViewHolder> {

    public ChatRoomSeatUserAdapter(@Nullable List<Member> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_room_seat_user;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Member item = getItemData(position);
        if (item == null) {
            return;
        }
        holder.mDataBinding.viewUser.setUserInfo(item);
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomSeatUserBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
