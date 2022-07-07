package io.agora.uiwidget.function;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import io.agora.uiwidget.R;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.OnlineUserListDialogItemBinding;
import io.agora.uiwidget.databinding.OnlineUserListDialogLayoutBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class OnlineUserListDialog extends BottomSheetDialog {
    private OnlineUserListDialogLayoutBinding mBinding;

    public OnlineUserListDialog(@NonNull Context context) {
        this(context, R.style.BottomSheetDialog);
    }

    public OnlineUserListDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    private void init() {
        setCanceledOnTouchOutside(true);
        mBinding = OnlineUserListDialogLayoutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.VERTICAL, false));
    }

    public <T> OnlineUserListDialog setListAdapter(AbsListItemAdapter<T> adapter){
        mBinding.recyclerView.setAdapter(adapter);
        return this;
    }

    @Override
    public void show() {
        super.show();
    }

    public static abstract class AbsListItemAdapter<T> extends RecyclerView.Adapter<BindingViewHolder<OnlineUserListDialogItemBinding>>{
        private final List<T> mList = new ArrayList<>();

        @NonNull
        @Override
        public final BindingViewHolder<OnlineUserListDialogItemBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BindingViewHolder<>(OnlineUserListDialogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public final void onBindViewHolder(@NonNull BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position) {
            T item = mList.get(position);
            onItemUpdate(holder, position, item);
        }

        @Override
        public final int getItemCount() {
            return mList.size();
        }

        public AbsListItemAdapter<T> resetAll(List<T> list){
            mList.clear();
            mList.addAll(list);
            notifyDataSetChanged();
            return this;
        }

        protected abstract void onItemUpdate(BindingViewHolder<OnlineUserListDialogItemBinding> holder, int position, T item);
    }
}
