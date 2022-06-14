package io.agora.scene.voice.widgets;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import io.agora.scene.voice.R;
import io.agora.scene.voice.utils.RoomBgUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class BackgroundDialog extends BottomSheetDialog {
    public interface BackgroundActionSheetListener {
        void onBackgroundPicSelected(int index, int res);
    }

    private static final int GRID_SPAN = 3;

    private int mSelected;
    private BackgroundAdapter mAdapter;
    private BackgroundActionSheetListener mListener;
    private AppCompatImageView mBackBtn;

    public BackgroundDialog(Context context) {
        super(context, io.agora.uiwidget.R.style.BottomSheetDialog);
        init();
    }


    private void init() {
        setCanceledOnTouchOutside(true);

        View rootView = LayoutInflater.from(getContext()).inflate(R.layout.voice_background_dialog, null);
        RecyclerView recyclerView = rootView.findViewById(R.id.action_sheet_background_recycler);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), GRID_SPAN);
        recyclerView.setLayoutManager(gridLayoutManager);
        mAdapter = new BackgroundAdapter();
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new BackgroundViewDecoration());

        mBackBtn = rootView.findViewById(R.id.iv_back);
        mBackBtn.setOnClickListener(view -> {
            dismiss();
        });
        super.setContentView(rootView);

        StatusBarUtil.hideStatusBar(getWindow(), false);
    }

    public void setOnBackgroundActionListener(BackgroundActionSheetListener listener) {
        mListener = listener;
    }

    public void setSelected(int selected) {
        if (0 <= selected && selected < RoomBgUtil.totalCount()) {
            mSelected = selected;
            mAdapter.notifyDataSetChanged();
        }
    }

    public int getSelected() {
        return mSelected;
    }

    public void setShowBackButton(boolean show) {
        mBackBtn.setVisibility(show ? VISIBLE : GONE);
    }

    private class BackgroundAdapter extends RecyclerView.Adapter<BackgroundViewHolder> {
        @NonNull
        @Override
        public BackgroundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BackgroundViewHolder(LayoutInflater.from(getContext()).inflate(
                    R.layout.voice_background_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BackgroundViewHolder holder, int position) {
            int pos = holder.getAdapterPosition();
            int maskVisibility = pos == mSelected ? VISIBLE : GONE;
            holder.mask.setVisibility(maskVisibility);

            holder.image.setClipToOutline(true);
            holder.image.setOutlineProvider(new ItemOutlineProvider());
            holder.image.setImageResource(RoomBgUtil.getRoomBgPreviewRes(pos));

            holder.itemView.setOnClickListener(view -> {
                mSelected = pos;
                notifyDataSetChanged();
                if (mListener != null) mListener.onBackgroundPicSelected(pos,
                        RoomBgUtil.getRoomBgPicRes(pos));
            });
        }

        @Override
        public int getItemCount() {
            return RoomBgUtil.totalCount();
        }
    }

    private static class BackgroundViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout mask;
        AppCompatImageView image;

        public BackgroundViewHolder(@NonNull View itemView) {
            super(itemView);

            mask = itemView.findViewById(R.id.action_sheet_background_item_selected_mask);
            image = itemView.findViewById(R.id.action_sheet_background_item_image);
        }
    }

    private class ItemOutlineProvider extends ViewOutlineProvider {
        int radius = getContext().getResources().getDimensionPixelOffset(R.dimen.voice_background_corner_2);

        @Override
        public void getOutline(View view, Outline outline) {
            Rect rect = new Rect();
            view.getDrawingRect(rect);
            outline.setRoundRect(rect, radius);
        }
    }

    private class BackgroundViewDecoration extends RecyclerView.ItemDecoration {
        int paddingHorizontal = getContext().getResources().getDimensionPixelOffset(R.dimen.voice_background_corner_2);
        int paddingVertical = getContext().getResources().getDimensionPixelOffset(R.dimen.voice_background_corner_4);

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        }
    }
}
