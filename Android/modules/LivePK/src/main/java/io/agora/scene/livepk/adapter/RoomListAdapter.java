package io.agora.scene.livepk.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.agora.scene.livepk.R;
import io.agora.scene.livepk.model.RoomInfo;
import io.agora.scene.livepk.util.UserUtil;
import io.agora.scene.livepk.widget.SquareRelativeLayout;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomListItemViewHolder> {
    private List<RoomInfo> mRoomList = new ArrayList<>();
    private OnItemClickListener itemClickListener;

    @NonNull
    @Override
    public RoomListAdapter.RoomListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RoomListAdapter.RoomListItemViewHolder(LayoutInflater.from(parent.getContext()).
                inflate(R.layout.pk_live_room_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RoomListAdapter.RoomListItemViewHolder holder, final int position) {
        if (mRoomList.size() <= position) return;

        RoomInfo info = mRoomList.get(position);
        holder.name.setText(info.getRoomName());
        holder.layout.setBackgroundResource(UserUtil.getUserProfileIcon(info.getRoomId()));
        holder.itemView.setOnClickListener((view) -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClicked(info);
            }
        });
        holder.ivDelete.setOnClickListener(v -> {
            if(itemClickListener != null){
                itemClickListener.onItemDeleteClicked(info, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRoomList.size();
    }

    public void appendList(List<RoomInfo> infoList, boolean reset) {
        if (reset) {
            mRoomList.clear();
        }
        if (infoList != null) {
            mRoomList.addAll(infoList);
        }
        notifyDataSetChanged();
    }

    public void appendItem(RoomInfo info) {
        int position = mRoomList.size();
        mRoomList.add(info);
        notifyItemInserted(position);
    }

    public void remoteItem(String roomId) {
        int removePosition = -1;
        for (int i = 0; i < mRoomList.size(); i++) {
            if (mRoomList.get(i).getRoomId().equals(roomId)) {
                removePosition = i;
                break;
            }
        }
        mRoomList.remove(removePosition);
        notifyItemRemoved(removePosition);
    }

    public void clear(boolean notifyChange) {
        mRoomList.clear();
        if (notifyChange) notifyDataSetChanged();
    }

    public RoomInfo getLast() {
        return mRoomList.isEmpty() ? null : mRoomList.get(mRoomList.size() - 1);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    static class RoomListItemViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView name;
        SquareRelativeLayout layout;
        ImageView ivDelete;

        RoomListItemViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.live_room_list_item_room_name);
            layout = itemView.findViewById(R.id.live_room_list_item_background);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(RoomInfo item);

        void onItemDeleteClicked(RoomInfo item, int index);
    }
}
