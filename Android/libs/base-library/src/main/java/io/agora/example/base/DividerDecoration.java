package io.agora.example.base;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 分割线
 *
 * @author chenhengfei@agora.io
 */
public class DividerDecoration extends RecyclerView.ItemDecoration {

    private final int gapHorizontal;
    private final int gapVertical;
    private final int spanCount;

    public DividerDecoration(int spanCount) {
        gapHorizontal = (int) BaseUtil.dp2px(16);
        gapVertical = gapHorizontal;
        this.spanCount = spanCount;
    }

    public DividerDecoration(int spanCount, int gapHorizontal, int gapHeight) {
        this.gapHorizontal = (int) BaseUtil.dp2px(gapHorizontal);
        this.gapVertical = (int) BaseUtil.dp2px(gapHeight);
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int index = parent.getChildAdapterPosition(view);

        if(spanCount == 1){
            outRect.left = gapHorizontal;
            outRect.right = gapHorizontal;
        }else {
            outRect.left = gapHorizontal * (spanCount - index % spanCount)/spanCount;
            outRect.right = gapHorizontal * (1 + index % spanCount)/spanCount;
        }
        outRect.top = gapVertical / 2;
        outRect.bottom = gapVertical / 2;
    }
}
