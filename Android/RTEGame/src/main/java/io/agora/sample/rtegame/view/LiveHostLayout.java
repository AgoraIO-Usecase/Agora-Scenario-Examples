package io.agora.sample.rtegame.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import io.agora.example.base.BaseUtil;

public class LiveHostLayout extends ConstraintLayout {

    public int bottomMarginInGameType = 0;

    public LiveHostCardView hostView;
    public LiveHostCardView subHostView;

    private Type type;

    public LiveHostLayout(@NonNull Context context) {
        this(context, null);
    }

    public LiveHostLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveHostLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            hostView = new LiveHostCardView(context);
            hostView.setCardBackgroundColor(Color.RED);
            subHostView = new LiveHostCardView(context);
            subHostView.setCardBackgroundColor(Color.BLUE);
            addView(hostView, new LayoutParams(0, 0));
            addView(subHostView, new LayoutParams(0, 0));
            setType(Type.DOUBLE);
        }
    }

    public LiveHostCardView createHostView(){
        if (hostView != null)
            this.removeView(hostView);

        hostView = new LiveHostCardView(getContext());
        hostView.setId(View.generateViewId());
        this.addView(hostView , new ConstraintLayout.LayoutParams(0, 0));
        return hostView;
    }

    public LiveHostCardView createSubHostView(){
        if (subHostView != null)
            this.removeView(subHostView);

        subHostView = new LiveHostCardView(getContext());
        subHostView.setId(View.generateViewId());
        this.addView(subHostView, new ConstraintLayout.LayoutParams(0 ,0));
        return subHostView;
    }

    private void onDoubleInGamePerformed() {
        if (subHostView != null && subHostView.getParent() == this) {
            BaseUtil.logD("subHostView");
            subHostView.setVisibility(VISIBLE);
            ConstraintLayout.LayoutParams lp = (LayoutParams) subHostView.getLayoutParams();
            clearRequiredViewParams(lp);
            int width = getContext().getResources().getDisplayMetrics().widthPixels;
            int height = getContext().getResources().getDisplayMetrics().heightPixels;

            lp.dimensionRatio = width + ":" + height;
            lp.matchConstraintPercentWidth = 0.25f;
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            subHostView.setLayoutParams(lp);
        }
        if (hostView != null && hostView.getParent() == this) {
            BaseUtil.logD("hostView");
            ConstraintLayout.LayoutParams lp = (LayoutParams) hostView.getLayoutParams();
            clearRequiredViewParams(lp);

            int width = getContext().getResources().getDisplayMetrics().widthPixels;
            int height = getContext().getResources().getDisplayMetrics().heightPixels;

            lp.dimensionRatio = width + ":" + height;
            lp.matchConstraintPercentWidth = 0.25f;

            if (subHostView == null) {
                lp.rightToRight = ConstraintSet.PARENT_ID;
                lp.bottomToBottom = ConstraintSet.PARENT_ID;
            } else {
                lp.rightToLeft = subHostView.getId();
                lp.bottomToBottom = subHostView.getId();
            }
            hostView.setLayoutParams(lp);
        }
    }

    private void onDoublePerformed() {
        if (subHostView != null && subHostView.getParent() == this) {
            subHostView.setVisibility(VISIBLE);
            ConstraintLayout.LayoutParams lp = (LayoutParams) subHostView.getLayoutParams();
            clearRequiredViewParams(lp);

            lp.dimensionRatio = "1:1";
            lp.verticalBias = 0.3f;
            lp.matchConstraintPercentWidth = 0.5f;
            lp.topToTop = ConstraintSet.PARENT_ID;
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            subHostView.setLayoutParams(lp);
        }
        if (hostView != null && hostView.getParent() == this) {
            ConstraintLayout.LayoutParams lp = (LayoutParams) hostView.getLayoutParams();
            clearRequiredViewParams(lp);

            lp.dimensionRatio = "1:1";
            lp.verticalBias = 0.3f;
            lp.matchConstraintPercentWidth = 0.5f;
            lp.leftToLeft = ConstraintSet.PARENT_ID;
            lp.topToTop = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            BaseUtil.logD(lp.toString());
            hostView.setLayoutParams(lp);
        }
    }

    private void onHostOnlyPerformed() {
        if (subHostView != null) subHostView.setVisibility(GONE);
        if (hostView != null && hostView.getParent() == this) {
            ConstraintLayout.LayoutParams lp = (LayoutParams) hostView.getLayoutParams();
            lp.leftToLeft = ConstraintSet.PARENT_ID;
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.topToTop = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            hostView.setLayoutParams(lp);
        }
    }


    public void clearRequiredViewParams(ConstraintLayout.LayoutParams lp) {
        lp.horizontalBias = 0.5f;
        lp.verticalBias = 0.5f;
        lp.dimensionRatio = null;
        lp.matchConstraintPercentWidth = 1f;
        lp.leftMargin = 0;
        lp.topMargin = 0;
        lp.rightMargin = 0;
        lp.bottomMargin = 0;

        lp.leftToLeft = ConstraintSet.UNSET;
        lp.leftToRight = ConstraintSet.UNSET;
        lp.topToTop = ConstraintSet.UNSET;
        lp.topToBottom = ConstraintSet.UNSET;
        lp.rightToRight = ConstraintSet.UNSET;
        lp.rightToLeft = ConstraintSet.UNSET;
        lp.bottomToBottom = ConstraintSet.UNSET;
        lp.bottomToTop = ConstraintSet.UNSET;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        switch (this.type) {
            case HOST_ONLY: {
                onHostOnlyPerformed();
                break;
            }
            case DOUBLE: {
                onDoublePerformed();
                break;
            }
            case DOUBLE_IN_GAME: {
                onDoubleInGamePerformed();
                break;
            }
        }
    }

    public enum Type {
        HOST_ONLY, DOUBLE, DOUBLE_IN_GAME,
    }

}
