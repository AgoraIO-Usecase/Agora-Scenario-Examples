package io.agora.marriageinterview.adapter;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Room;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryItemRoomsBinding;

/**
 * 房间列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomListAdapter extends BaseRecyclerViewAdapter<Room, RoomListAdapter.ViewHolder> {

    public RoomListAdapter(@Nullable List<Room> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_item_rooms;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Room item = getItemData(position);
        if (item == null) {
            return;
        }

        holder.mDataBinding.tvName.setText(item.getChannelName());
        Glide.with(holder.itemView).load(item.getAnchorId().getAvatarRes()).circleCrop().into(holder.mDataBinding.ivHead);
        Glide.with(holder.itemView)
                .load(item.getAnchorId().getAvatarRes())
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        holder.mDataBinding.clbackground.setBackground(resource);
                    }
                });
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<MerryItemRoomsBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
