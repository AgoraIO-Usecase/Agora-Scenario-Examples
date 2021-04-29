package io.agora.marriageinterview.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.model.Member;
import com.bumptech.glide.Glide;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryItemRoomMemberListBinding;
import io.agora.marriageinterview.widget.MemberListDialog;

/**
 * 房间成员列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomMemberListAdapter extends BaseRecyclerViewAdapter<Member, RoomMemberListAdapter.ViewHolder> {

    private boolean showInvite = false;
    private Member mine;
    private MemberListDialog.IConnectStatusProvider mIConnectStatusProvider;

    public RoomMemberListAdapter(@Nullable List<Member> datas, @Nullable Object listener, boolean showInvite, Member mine, MemberListDialog.IConnectStatusProvider mIConnectStatusProvider) {
        super(datas, listener);
        this.showInvite = showInvite;
        this.mine = mine;
        this.mIConnectStatusProvider = mIConnectStatusProvider;
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_item_room_member_list;
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

        Glide.with(holder.itemView.getContext())
                .load(item.getUserId().getAvatarRes())
                .placeholder(R.mipmap.default_head)
                .error(R.mipmap.default_head)
                .circleCrop()
                .into(holder.mDataBinding.ivHead);
        holder.mDataBinding.tvName.setText(item.getUserId().getName());

        if (showInvite) {
            if (ObjectsCompat.equals(item, mine)) {
                holder.mDataBinding.tvInvite.setVisibility(View.GONE);
            } else if (mIConnectStatusProvider.isMemberConnected(item)) {
                holder.mDataBinding.tvInvite.setVisibility(View.GONE);
            } else {
                holder.mDataBinding.tvInvite.setVisibility(View.VISIBLE);
            }
        } else {
            holder.mDataBinding.tvInvite.setVisibility(View.GONE);
        }
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<MerryItemRoomMemberListBinding> {

        public ViewHolder(View view) {
            super(view);

            mDataBinding.tvInvite.setOnClickListener(this::onItemClick);
        }
    }
}
