package io.agora.marriageinterview.adapter;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Member;
import com.agora.data.model.Message;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryItemRoomMessagesBinding;

/**
 * 房间信息列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomMessagesAdapter extends BaseRecyclerViewAdapter<Message, RoomMessagesAdapter.ViewHolder> {

    public RoomMessagesAdapter(@Nullable List<Message> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    public void onMemberJoin(@NonNull Context context, @NonNull Member member) {
        addItem(new Message(member.getUserId().getName(), context.getString(R.string.room_chat_message_join)));
    }

    public void onMemberLeave(@NonNull Context context, @NonNull Member member) {
        addItem(new Message(member.getUserId().getName(), context.getString(R.string.room_chat_message_leave)));
    }

    public void onRoomMessageReceived(@NonNull Member member, @NonNull String message) {
        addItem(new Message(member.getUserId().getName(), message));
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_item_room_messages;
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

        holder.mDataBinding.tvName.setText(item.getName());
        holder.mDataBinding.tvMessage.setText(item.getMessage());
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<MerryItemRoomMessagesBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
