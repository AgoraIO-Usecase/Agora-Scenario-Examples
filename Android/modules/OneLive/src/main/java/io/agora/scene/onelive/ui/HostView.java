package io.agora.scene.onelive.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import io.agora.example.base.BaseUtil;
import io.agora.scene.onelive.R;

public class HostView extends ConstraintLayout {

    private boolean isSingleHost = true;

    private final TextureView currentViewport;
    private final TextureView targetViewport;

    private final CardView viewportContainer;

    public HostView(@NonNull Context context) {
        this(context, null);
    }

    public HostView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HostView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HostView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        currentViewport = new TextureView(context);
        currentViewport.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        targetViewport = new TextureView(context);
        currentViewport.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Init CardView
        LayoutParams layoutParams = new LayoutParams(0, 0);
        layoutParams.endToEnd = ConstraintSet.PARENT_ID;
        layoutParams.topToTop = ConstraintSet.PARENT_ID;
        layoutParams.matchConstraintPercentWidth = 0.28f;
        layoutParams.dimensionRatio = "105:140";

        viewportContainer = new CardView(context);
        viewportContainer.setCardElevation(BaseUtil.dp2px(4));
        viewportContainer.setCardBackgroundColor(BaseUtil.getColorInt(context, R.attr.colorSurface));
        viewportContainer.setRadius(BaseUtil.dp2px(8));
        viewportContainer.setLayoutParams(layoutParams);
    }

    private void configViewForCurrentState() {
        if (isSingleHost) { // 只有一个人
            safeAddView(currentViewport);
            safeDetachView(viewportContainer);
        } else { // 两个人
            safeAddView(targetViewport);
            safeAddView(viewportContainer);
            safeAddView(viewportContainer, currentViewport);
        }

    }

    private void safeAddView(@NonNull View view) {
        safeAddView(this, view);
    }

    private void safeAddView(@NonNull ViewGroup parent, @NonNull View view) {
        if (view.getParent() != null) {
            if (view.getParent() != parent) {
                ((ViewGroup) view.getParent()).removeView(view);
                parent.addView(view);
            }
        } else
            parent.addView(view);
    }

    private void safeDetachView(View view) {
        if (view.getParent() == this)
            this.removeView(view);
    }

    public boolean isSingleHost() {
        return isSingleHost;
    }

    public void setSingleHost(boolean singleHost) {
        isSingleHost = singleHost;
        configViewForCurrentState();
    }

    @NonNull
    public TextureView getCurrentViewport() {
        return currentViewport;
    }

    @NonNull
    public TextureView getTargetViewport() {
        return targetViewport;
    }

    @NonNull
    public CardView getViewportContainer() {
        return viewportContainer;
    }
}
