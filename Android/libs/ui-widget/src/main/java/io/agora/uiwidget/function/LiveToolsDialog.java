package io.agora.uiwidget.function;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import io.agora.uiwidget.R;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.LiveToolItemBinding;
import io.agora.uiwidget.databinding.LiveToolLayoutBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class LiveToolsDialog extends BottomSheetDialog {

    public final static ToolItem TOOL_ITEM_DATA = new ToolItem(R.string.live_tool_name_data, R.drawable.live_tool_icon_data);
    public final static ToolItem TOOL_ITEM_SPEAKER = new ToolItem(R.string.live_tool_name_speaker, R.drawable.live_tool_icon_speaker);
    public final static ToolItem TOOL_ITEM_EAR_MONITOR = new ToolItem(R.string.live_tool_name_ear_monitor, R.drawable.live_tool_icon_ear_monitor);
    public final static ToolItem TOOL_ITEM_SETTING = new ToolItem(R.string.live_tool_name_setting, R.drawable.live_tool_icon_setting);
    public final static ToolItem TOOL_ITEM_ROTATE = new ToolItem(R.string.live_tool_name_rotate, R.drawable.live_tool_icon_rotate);
    public final static ToolItem TOOL_ITEM_VIDEO = new ToolItem(R.string.live_tool_name_camera, R.drawable.live_tool_icon_video);


    private LiveToolLayoutBinding mBinding;
    private final List<ToolItem> showToolItems = new ArrayList<>();


    public LiveToolsDialog(@NonNull Context context) {
        this(context, R.style.BottomSheetDialog);
    }

    public LiveToolsDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    private void init() {
        setCanceledOnTouchOutside(true);
        mBinding = LiveToolLayoutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        mBinding.liveToolRecycler.setLayoutManager(new GridLayoutManager(getContext(), 4));
        mBinding.liveToolRecycler.setAdapter(new ToolsAdapter());
    }

    public LiveToolsDialog addToolItem(ToolItem item, boolean isActivated, OnItemClickListener listener) {
        showToolItems.add(new ToolItem(item.nameRes, item.iconRes, isActivated, listener));
        return this;
    }


    private class ToolsAdapter extends RecyclerView.Adapter<BindingViewHolder<LiveToolItemBinding>> {

        @NonNull
        @Override
        public BindingViewHolder<LiveToolItemBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BindingViewHolder<>(LiveToolItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BindingViewHolder<LiveToolItemBinding> holder, int position) {
            ToolItem toolItem = showToolItems.get(position);
            holder.binding.liveToolItemIcon.setImageResource(toolItem.iconRes);
            holder.binding.liveToolItemName.setText(toolItem.nameRes);
            holder.binding.liveToolItemIcon.setActivated(toolItem.activated);
            holder.binding.liveToolItemIcon.setOnClickListener(v -> {
                toolItem.activated = !toolItem.activated;
                holder.binding.liveToolItemIcon.setActivated(toolItem.activated);
                if (toolItem.click != null) {
                    toolItem.click.onItemClicked(v, toolItem);
                }
            });
        }

        @Override
        public int getItemCount() {
            return showToolItems.size();
        }
    }

    public static class ToolItem {
        private final int iconRes;
        private final int nameRes;
        private boolean activated;
        private final OnItemClickListener click;

        public ToolItem(int nameRes, int iconRes) {
            this.iconRes = iconRes;
            this.nameRes = nameRes;
            this.activated = false;
            this.click = null;
        }

        private ToolItem(int nameRes, int iconRes, boolean activated, OnItemClickListener click) {
            this.iconRes = iconRes;
            this.nameRes = nameRes;
            this.activated = activated;
            this.click = click;
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(View view, ToolItem item);
    }
}
