package io.agora.uiwidget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuqiang
 * 可自定义一行多少个 View
 * 默认Flex模式
 * 所有子View宽高1：1
 * 需要定制可重写 {@link this#getStepByChildCount(int)}
 * <p>
 * For STYLE_FLEX children will be in a ConstraintLayout.
 * For STYLE_SCROLL children will be in this view and a ScrollableLinearLayout with a IndicatorView.
 */
public class DynamicView extends ConstraintLayout {
    public static final int STYLE_FLEX = 0;
    public static final int STYLE_SCROLL = 1;


    @Nullable
    public ScrollView flexContainerScrollView;
    @Nullable
    public ConstraintLayout flexContainer;
    @Nullable
    public ScrollableLinearLayout scrollContainer;
    @Nullable
    public IndicatorView indicatorView;

    private int layoutStyle;
    public boolean needCustomClick = false;
    public boolean fitEnd = true;
    public int gapInFlex = 0;

    ///////////////////////////////////// ANIMATION /////////////////////////////////////////////////////////////////
    public final Transition flexTransition = new AutoTransition();

    public DynamicView(@NonNull Context context) {
        this(context, null);
    }

    public DynamicView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DynamicView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DynamicView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DynamicView, defStyleAttr, defStyleRes);
        layoutStyle = ta.getInt(R.styleable.DynamicView_dynamic_layoutStyle, DynamicView.STYLE_FLEX);
        fitEnd = ta.getBoolean(R.styleable.DynamicView_dynamic_fitEnd, true);
        gapInFlex = ta.getDimensionPixelSize(R.styleable.DynamicView_dynamic_gapInFlex, 0);

        int previewViewCount = ta.getInt(R.styleable.DynamicView_dynamic_previewViewCount, 3);
        ta.recycle();

        // init childView
        setupViewWithStyle();

        // for the preview
        if (this.isInEditMode()) {
            for (int i = 0; i < previewViewCount; i++) {
                dynamicAddView(ScrollableLinearLayout.getChildAudioCardView(getContext(), null, "V" + (i + 1)));
            }
        }
    }

    @Override
    protected void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configChangeForContainer(newConfig);
    }


    private void configChangeForContainer(Configuration newConfig) {

        if (scrollContainer != null) {
            scrollContainer.setTranslationX(0f);
            scrollContainer.setTranslationY(0f);

            ConstraintLayout.LayoutParams lp4Indicator;
            ConstraintLayout.LayoutParams lp4Container;

            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                scrollContainer.setOrientation(LinearLayoutCompat.HORIZONTAL);

                lp4Container = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp4Indicator = new LayoutParams(LayoutParams.MATCH_PARENT, (int) dp2px(24));

                if (fitEnd) {
                    lp4Container.bottomToBottom = ConstraintSet.PARENT_ID;
                    lp4Indicator.bottomToTop = scrollContainer.getId();
                    if (indicatorView != null) {
                        indicatorView.gravity = Gravity.BOTTOM;
                    }
                } else {
                    lp4Container.topToTop = ConstraintSet.PARENT_ID;
                    lp4Indicator.topToBottom = scrollContainer.getId();
                    if (indicatorView != null) {
                        indicatorView.gravity = Gravity.TOP;
                    }
                }


            } else {
                scrollContainer.setOrientation(LinearLayoutCompat.VERTICAL);
                lp4Container = new LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
                lp4Indicator = new LayoutParams((int) dp2px(24), LayoutParams.MATCH_PARENT);

                if (fitEnd) {
                    lp4Container.endToEnd = ConstraintSet.PARENT_ID;
                    lp4Indicator.endToStart = scrollContainer.getId();
                    if (indicatorView != null) {
                        indicatorView.gravity = Gravity.END;
                    }
                } else {
                    lp4Container.startToStart = ConstraintSet.PARENT_ID;
                    lp4Indicator.startToEnd = scrollContainer.getId();
                    if (indicatorView != null) {
                        indicatorView.gravity = Gravity.START;
                    }
                }
            }

            if (indicatorView != null) {
                indicatorView.setLayoutParams(lp4Indicator);
            }
            scrollContainer.setLayoutParams(lp4Container);

            scrollContainer.attachToIndicator(indicatorView);
            scrollContainer.forceLayout();
            scrollContainer.invalidate();
        }
    }

    public void switchView(@NonNull View thumbView) {
        if (layoutStyle == STYLE_SCROLL) {
            View mainView = this.getChildAt(0);
            if (mainView != scrollContainer && mainView != indicatorView)
                switchView(mainView, thumbView);
            else switchView(null, thumbView);
        }
    }

    /**
     * 仅在 STYLE_SCROLL 可用
     */
    public void switchView(@Nullable View mainView, @NonNull View thumbView) {

        if (thumbView.getParent() != scrollContainer)
            throw new IllegalStateException("thumbView should be a child of scrollContainer.");

        if (!needCustomClick) {
            thumbView.setOnClickListener(null);
            if (mainView != null)
                mainView.setOnClickListener(this::switchView);
        }
        if (mainView != null)
            configViewIfIsSurfaceView(mainView, true);

        configViewIfIsSurfaceView(thumbView, false);

        doSwitchView(mainView, thumbView);
    }

    private void doSwitchView(@Nullable View mainView, @NonNull View thumbView) {
        if (scrollContainer == null) return;

        int indexOfMain = this.indexOfChild(mainView);
        if (indexOfMain >= 0)
            this.removeView(mainView);
        else indexOfMain = 0;

        int indexOfThumb = scrollContainer.indexOfChild(thumbView);
        if (indexOfThumb >= 0)
            scrollContainer.removeView(thumbView);

        this.addView(thumbView, indexOfMain, getLpForMainView());

        if (mainView != null)
            scrollContainer.addView(mainView, indexOfThumb);
    }

    /**
     * Handle stuff each time layoutStyle changed.
     * 1. Gather previousChildren then detach all of it.
     * 2. Recreate container based on current configuration and current layoutStyle.
     * 3. Restore all view.
     */
    private void setupViewWithStyle() {
        List<View> oldChildren = resetView();

        if (layoutStyle == DynamicView.STYLE_FLEX) {
            initFlexContainer();
            for (View child : oldChildren) {
                configViewIfIsSurfaceView(child, false);
                if (!needCustomClick)
                    child.setOnClickListener(null);
                dynamicAddView(child);
            }
            regroupFlexChildren();
        } else {
            initScrollContainer();
            for (int i = 0; i < oldChildren.size(); i++) {
                View child = oldChildren.get(i);
                if (i == 0) {
                    configViewIfIsSurfaceView(child, false);
                    if (!needCustomClick) child.setOnClickListener(null);
                } else {
                    configViewIfIsSurfaceView(child, true);
                    if (!needCustomClick) child.setOnClickListener(this::switchView);
                }
                dynamicAddView(child);
            }
        }
        oldChildren.clear();
    }

    /**
     * if there is a SurfaceView inside a child
     * setZOrderMediaOverlay to it
     *
     * @param child the view you want to config
     */
    private void configViewIfIsSurfaceView(View child, boolean zOrderMediaOverlay) {
        // for SurfaceView
        SurfaceView surfaceView = null;
        if (child instanceof ViewGroup) {
            ViewGroup viewGroup = ((ViewGroup) child);
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (viewGroup.getChildAt(i) instanceof SurfaceView) {
                    surfaceView = (SurfaceView) viewGroup.getChildAt(i);
                    break;
                }
            }
        } else if (child instanceof SurfaceView) {
            surfaceView = (SurfaceView) child;
        }
        if (surfaceView != null)
            surfaceView.setZOrderMediaOverlay(zOrderMediaOverlay);
    }

    /**
     * gather all view then detach all of it
     *
     * @return previousChildren
     */
    private List<View> resetView() {
        List<View> previousChildren = new ArrayList<>();

        if (getChildCount() != 0) {

            // there will be a view in this on STYLE_SCROLL.
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view != indicatorView && view != flexContainerScrollView && view != scrollContainer)
                    previousChildren.add(view);
            }

            ViewGroup container = null;
            if (flexContainer != null)
                container = flexContainer;
            else if (scrollContainer != null)
                container = scrollContainer;

            if (container != null) {
                for (int i = 0; i < container.getChildCount(); i++)
                    previousChildren.add(container.getChildAt(i));
                container.removeAllViews();
            }
            removeAllViews();
            flexContainerScrollView = null;
            flexContainer = null;
            scrollContainer = null;
            indicatorView = null;
        }
        return previousChildren;
    }

    private void initFlexContainer() {
        flexContainerScrollView = new ScrollView(getContext());
        flexContainerScrollView.setId(ViewCompat.generateViewId());
        flexContainerScrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        flexContainerScrollView.setFillViewport(true);

        flexContainer = new ConstraintLayout(getContext());
        flexContainer.setId(ViewCompat.generateViewId());
        flexContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        flexContainer.setLayoutTransition(null);

        flexContainerScrollView.addView(flexContainer);
        this.addView(flexContainerScrollView);
    }

    private void initScrollContainer() {
        scrollContainer = new ScrollableLinearLayout(getContext());
        scrollContainer.setId(ViewCompat.generateViewId());
        scrollContainer.fitEnd = this.fitEnd;
        ViewCompat.setElevation(scrollContainer, dp2px(2));

        indicatorView = new IndicatorView(getContext());
        indicatorView.setId(ViewCompat.generateViewId());
        ViewCompat.setElevation(indicatorView, dp2px(2));

        configChangeForContainer(getResources().getConfiguration());

        this.addView(scrollContainer);
        this.addView(indicatorView);
    }

    /**
     * For a better control we override child's LayoutParams
     */
    public void dynamicAddView(@NonNull View child) {
        if (child.getId() == View.NO_ID)
            child.setId(ViewCompat.generateViewId());

        if (layoutStyle == DynamicView.STYLE_FLEX) {
            child.setLayoutParams(getLpForMainView());
            addChildInFlexLayout(child);
        } else if (this.getChildAt(0) == scrollContainer || this.getChildAt(0) == indicatorView) {
            child.setLayoutParams(getLpForMainView());
            addView(child, 0);
        } else {
            if (!needCustomClick)
                child.setOnClickListener(this::switchView);
            if (scrollContainer != null)
                scrollContainer.addView(child);
        }
    }

    public void dynamicRemoveView(@NonNull View view) {
        if (layoutStyle == DynamicView.STYLE_FLEX) {
            removeChildInFlexLayout(view);
        } else if (scrollContainer != null){
            scrollContainer.removeView(view);
        }
    }

    public void addChildInFlexLayout(@NonNull View view) {
        if (flexContainer == null) return;

        TransitionManager.beginDelayedTransition(flexContainer, flexTransition);
        flexContainer.addView(view);
        regroupFlexChildren();
    }

    public void dynamicRemoveViewWithTag(@NonNull Object tag) {
        View view = findViewWithTag(tag);
        if (view != null) {
            if (view.getParent() == this) this.removeView(view);
            else if (view.getParent() == scrollContainer && scrollContainer != null)
                scrollContainer.removeView(view);
            else if (view.getParent() == flexContainer && flexContainer != null) removeChildInFlexLayout(view);
        }
    }

    public void removeChildInFlexLayout(int index) {
        if (flexContainer != null)
            removeChildInFlexLayout(flexContainer.getChildAt(index));
    }

    public void removeChildInFlexLayout(@NonNull View view) {
        if (flexContainer != null) {
            TransitionManager.beginDelayedTransition(flexContainer, flexTransition);
            flexContainer.removeView(view);
            regroupFlexChildren();
        }
    }

    /**
     * 重新组织子 View 的关联
     */
    protected void regroupFlexChildren() {
        if (flexContainer != null) {
            int childCount = flexContainer.getChildCount();
            if (childCount != 0) {
                View currentView;
                int step = getStepByChildCount(childCount);
                for (int i = 0; i < childCount; i++) {
                    currentView = flexContainer.getChildAt(i);
                    configViewForFlexStyle(i, step, currentView);
                }
            }
        }
    }

    @NonNull
    public ConstraintLayout.LayoutParams getLpForMainView() {
        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, 0);
        if (this.layoutStyle == DynamicView.STYLE_FLEX) {
            lp.dimensionRatio = "1:1";
        } else {
            lp.leftToLeft = ConstraintSet.PARENT_ID;
            lp.startToStart = ConstraintSet.PARENT_ID;

            lp.topToTop = ConstraintSet.PARENT_ID;

            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.endToEnd = ConstraintSet.PARENT_ID;

            lp.bottomToBottom = ConstraintSet.PARENT_ID;
        }
        return lp;
    }

    /**
     * step = 2
     * 0 , 1
     * 2 , 3
     * <p>
     * step = 3
     * 0 , 1 , 2
     * 3 , 4 , 5
     * 6 , 7 , 8
     *
     * @param index view 在父View中的下标
     * @param step  步长
     */
    private void configViewForFlexStyle(int index, int step, View v) {
        if (flexContainer == null) return;

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, 0);
        lp.dimensionRatio = "1:1";
        lp.topMargin = gapInFlex;
        // 第一个
        if (index % step == 0) {
            lp.leftToLeft = ConstraintSet.PARENT_ID;
            lp.leftMargin = gapInFlex;
            // 最后一个
            if (step == 1) {
                lp.rightToRight = ConstraintSet.PARENT_ID;
                lp.rightMargin = gapInFlex;
            } else {
//                其他情况有可能此View为最后一个，需要判断。 0,1,2 \n 3,_,_
                if (index + 1 == this.flexContainer.getChildCount()) {
                    lp.rightToRight = this.flexContainer.getChildAt(index - step).getId();
                } else {
                    lp.rightToLeft = this.flexContainer.getChildAt(index + 1).getId();
                }
            }
        } else if (index % step == step - 1) { // 最后一个
            lp.leftToRight = this.flexContainer.getChildAt(index - 1).getId();
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.leftMargin = gapInFlex;
            lp.rightMargin = gapInFlex;
        } else { // 中间
            lp.leftToRight = this.flexContainer.getChildAt(index - 1).getId();
            lp.leftMargin = gapInFlex;
//          其他情况有可能此View为最后一个，需要判断
            if (index + 1 == this.flexContainer.getChildCount()) {
                lp.rightToLeft = this.flexContainer.getChildAt(index + 1 - step).getId();
            } else {
                lp.rightToLeft = this.flexContainer.getChildAt(index + 1).getId();
            }
        }
        // TOP 修正
        if (index - step < 0) {
            lp.topToTop = ConstraintSet.PARENT_ID;
        } else {
            lp.topToBottom = this.flexContainer.getChildAt(index - step).getId();
        }
        // 赋值
        v.setLayoutParams(lp);
    }

    public int getLayoutStyle() {
        return layoutStyle;
    }

    public void setLayoutStyle(@IntRange(from = DynamicView.STYLE_FLEX, to = DynamicView.STYLE_SCROLL) int newLayoutStyle) {
        // this is important
        clearAnimation();
        this.layoutStyle = newLayoutStyle;
        setupViewWithStyle();
    }

    /**
     * override this method for custom requirement
     *
     * @param childCount 子 view 个数
     * @return step，隔多少个View换行
     */
    public int getStepByChildCount(int childCount) {
        if (childCount < 2)
            return 1;
        else if (childCount < 5)
            return 2;
        else return 3;
    }

    float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

}