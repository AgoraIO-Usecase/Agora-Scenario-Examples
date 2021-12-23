package io.agora.scene.livepk.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int mItemSpacing;
    private final int spanCount;

    public SpaceItemDecoration(int itemSpacing, int spanCount){
        this.mItemSpacing = itemSpacing;
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int position = parent.getChildAdapterPosition(view);
        int total = parent.getAdapter() == null ? 0 : parent.getAdapter().getItemCount();
        int half = mItemSpacing / 2;

        outRect.top = half;
        outRect.bottom = half;

        if (position < spanCount) {
            outRect.top = mItemSpacing;
        } else {
            int remain = total % spanCount;
            if (remain == 0) remain = spanCount;
            if (position + remain >= total) {
                outRect.bottom = mItemSpacing;
            }
        }

        if (position % spanCount == 0) {
            outRect.left = mItemSpacing;
            outRect.right = mItemSpacing / 2;
        } else {
            outRect.left = mItemSpacing / 2;
            outRect.right = mItemSpacing;
        }
    }
}
