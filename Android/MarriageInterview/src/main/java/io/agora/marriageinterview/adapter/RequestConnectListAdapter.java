package io.agora.marriageinterview.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Member;
import com.agora.data.model.RequestMember;
import com.bumptech.glide.Glide;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryItemRequestConnectListBinding;

/**
 * 申请连接列表
 *
 * @author chenhengfei@agora.io
 */
public class RequestConnectListAdapter extends BaseRecyclerViewAdapter<RequestMember, RequestConnectListAdapter.ViewHolder> {

    public RequestConnectListAdapter(@Nullable List<RequestMember> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_item_request_connect_list;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestMember item = getItemData(position);
        if (item == null) {
            return;
        }

        Member member = item.getMember();
        Glide.with(holder.itemView.getContext())
                .load(member.getUserId().getAvatarRes())
                .placeholder(R.mipmap.default_head)
                .error(R.mipmap.default_head)
                .circleCrop()
                .into(holder.mDataBinding.ivHead);
        holder.mDataBinding.tvName.setText(member.getUserId().getName());
    }

    static class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<MerryItemRequestConnectListBinding> {

        ViewHolder(View view) {
            super(view);

            mDataBinding.btRefuse.setOnClickListener(this::onItemClick);
            mDataBinding.btAgree.setOnClickListener(this::onItemClick);
        }
    }
}
