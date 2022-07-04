package io.agora.scene.voice.widgets;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import io.agora.scene.voice.R;
import io.agora.scene.voice.databinding.VoiceBackgroundDialogBinding;
import io.agora.scene.voice.databinding.VoiceBackgroundListItemBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.utils.StatusBarUtil;

public class BackgroundDialog extends BottomSheetDialog {

    private static final Integer[] PREVIEW_ICON_RES = {
            R.drawable.voice_room_bg_prev_1,
            R.drawable.voice_room_bg_prev_2,
            R.drawable.voice_room_bg_prev_3,
            R.drawable.voice_room_bg_prev_4,
            R.drawable.voice_room_bg_prev_5,
            R.drawable.voice_room_bg_prev_6,
            R.drawable.voice_room_bg_prev_7,
            R.drawable.voice_room_bg_prev_8,
            R.drawable.voice_room_bg_prev_9,
    };

    public static final Integer[] BG_PIC_RES = {
            R.drawable.voice_room_bg_big_1,
            R.drawable.voice_room_bg_big_2,
            R.drawable.voice_room_bg_big_3,
            R.drawable.voice_room_bg_big_4,
            R.drawable.voice_room_bg_big_5,
            R.drawable.voice_room_bg_big_6,
            R.drawable.voice_room_bg_big_7,
            R.drawable.voice_room_bg_big_8,
            R.drawable.voice_room_bg_big_9,
    };

    public interface BackgroundActionSheetListener {
        void onBackgroundPicSelected(int index, int res);
    }

    private VoiceBackgroundDialogBinding mBinding;
    private BindingSingleAdapter<Integer, VoiceBackgroundListItemBinding> mAdapter;
    private BackgroundActionSheetListener mListener;

    public BackgroundDialog(Context context) {
        super(context, io.agora.uiwidget.R.style.BottomSheetDialog);
        init();
    }


    private void init() {
        setCanceledOnTouchOutside(true);
        mBinding = VoiceBackgroundDialogBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mAdapter = new BindingSingleAdapter<Integer, VoiceBackgroundListItemBinding>(){
            private int _selectedPosition = -1;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceBackgroundListItemBinding> holder, int position) {
                final int pos = position;
                int maskVisibility = pos == _selectedPosition ? VISIBLE : GONE;
                holder.binding.actionSheetBackgroundItemSelectedMask.setVisibility(maskVisibility);

                holder.binding.actionSheetBackgroundItemImage.setClipToOutline(true);
                holder.binding.actionSheetBackgroundItemImage.setOutlineProvider(new ItemOutlineProvider());
                holder.binding.actionSheetBackgroundItemImage.setImageResource(getItem(pos));

                holder.itemView.setOnClickListener(view -> {
                    int oPosition = _selectedPosition;
                    _selectedPosition = pos;
                    notifyItemChanged(oPosition);
                    notifyItemChanged(pos);
                    if (mListener != null) {
                        mListener.onBackgroundPicSelected(
                                pos,
                                BG_PIC_RES[pos]);
                    }
                });
            }
        };
        mBinding.rvBackground.setAdapter(mAdapter);
        mBinding.rvBackground.addItemDecoration(new BackgroundViewDecoration());
        mAdapter.insertAll(PREVIEW_ICON_RES);

        mBinding.ivBack.setOnClickListener(view -> {
            dismiss();
        });
    }

    public void setOnBackgroundActionListener(BackgroundActionSheetListener listener) {
        mListener = listener;
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
