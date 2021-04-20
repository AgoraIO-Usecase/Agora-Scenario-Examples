package io.agora.marriageinterview.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Message;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.ItemRoomMessagesBinding;

/**
 * 房间信息列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomMessagesAdapter extends BaseRecyclerViewAdapter<Message, RoomMessagesAdapter.ViewHolder> {

    public RoomMessagesAdapter(@Nullable List<Message> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_room_messages;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message item = getItemData(position);
        if (item == null) {
            return;
        }
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemRoomMessagesBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
