package io.agora.uiwidget;

import static android.view.Gravity.CENTER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cardview.widget.CardView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;

import java.util.Random;

/**
 * @author lq
 * Feature 1: Can scroll( follow its orientation)
 * Feature 2: Can resizing & hide it self with a respectly gravity
 * Feature 3: Can have a indicator ( need a trigger to show it again when it's already hide)
 */
public class ScrollableLinearLayout extends LinearLayoutCompat {


    //////////////////////// INDICATOR PART //////////////////////////////////////////////
    @Nullable
    public IndicatorView mIndicatorView = null;

    // currentContentSize == preferContentSize[0] 《==》0f
    // currentContentSize > preferContentSize[0] 《==》 0f - 1f
    // translationX/Y / params.W/H 《==》 -1f - 0f
    private float currentFraction = 0f;

    //////////////////////// SIZE PART //////////////////////////////////////////////
    // View's size will only be in this range
    @NonNull
    public int[] preferContentSize = new int[]{80, 160};
    // When view restore needed ( like orientation changed ) we need to know current size.
    private int currentPreferSizeIndex = 0;
    // Spacer between children
    private int gapInDp = 12;
    // Total children length (based on current orientation) + total spacer
    private int mTotalLength;


    //////////////////////// GESTURE PART //////////////////////////////////////////////

    // indicate which gesture direction this view accept
    @IndicatorView.GravityFlag
    private int gravityFlag = Gravity.BOTTOM;
    // true for BOTTOM/END, otherwise TOP/START
    public boolean fitEnd = true;
    private final int mTouchSlop;
    // first touched point in float value
    private final PointF firstPointF = new PointF();
    // last touched point in float value
    private final PointF lastPointF = new PointF();
    // fot the scroll fling animation use
    private VelocityTracker velocityTracker;
    // VERTICAL or HORIZONTAL, otherwise -1;
    private int currentDirection = -1;


    //////////////////////// ANIMATION PART //////////////////////////////////////////////
    // These animations only perform when a fling needed

    // can perform translationX/translationY
    private boolean canTrans = false;

    // change view's Width/Height based on current Orientation.
    private FlingAnimation sizeAnimation;

    private FlingAnimation scrollXAnimation;
    private FlingAnimation scrollYAnimation;
    private FlingAnimation transXAnimation;
    private FlingAnimation transYAnimation;

    //<editor-fold desc="INIT">
    public ScrollableLinearLayout(@NonNull Context context) {
        this(context, null);
    }

    public ScrollableLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollableLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        setClipToPadding(false);

        preferContentSize[0] = (int) dp2px(preferContentSize[0]);
        preferContentSize[1] = (int) dp2px(preferContentSize[1]);
        setGapInDp(gapInDp);
        setPadding(gapInDp, gapInDp, gapInDp, gapInDp);

        setVisibility(GONE);
        initAnimation();

    }

    private void initAnimation() {
        sizeAnimation = new FlingAnimation(this, new FloatPropertyCompat<ScrollableLinearLayout>("size") {
            @Override
            public float getValue(ScrollableLinearLayout object) {
                return getCurrentContentSize();
            }

            @Override
            public void setValue(ScrollableLinearLayout object, float value) {
                if (getOrientation() == VERTICAL)
                    getLayoutParams().width = (int) value + getRelatedPadding(HORIZONTAL);
                else
                    getLayoutParams().height = (int) value + getRelatedPadding(VERTICAL);
                requestLayout();
            }
        })
                .setMaxValue(preferContentSize[1])
                .setMinValue(preferContentSize[0]);

        scrollXAnimation = new FlingAnimation(this, DynamicAnimation.SCROLL_X);
        scrollYAnimation = new FlingAnimation(this, DynamicAnimation.SCROLL_Y);

        transXAnimation = new FlingAnimation(this, DynamicAnimation.TRANSLATION_X);
        transYAnimation = new FlingAnimation(this, DynamicAnimation.TRANSLATION_Y);
    }
    //</editor-fold>

    //<editor-fold desc="Override function">

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        int action = ev.getActionMasked();

        if ((action == MotionEvent.ACTION_MOVE) && (currentDirection != -1)) {
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            firstPointF.x = ev.getRawX();
            firstPointF.y = ev.getRawY();
            lastPointF.x = ev.getRawX();
            lastPointF.y = ev.getRawY();

            ensureCurrentGravityFlag();

            // DON'T KNOW WHY THIS ISN'T WORKING
//            clearAnimation();

            sizeAnimation.cancel();
            scrollXAnimation.cancel();
            scrollYAnimation.cancel();
            transXAnimation.cancel();
            transYAnimation.cancel();
        } else if (action == MotionEvent.ACTION_MOVE) {
            float distanceX = lastPointF.x - ev.getRawX();
            float distanceY = lastPointF.y - ev.getRawY();
            if (Math.abs(distanceX) > mTouchSlop || Math.abs(distanceY) > mTouchSlop) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Ensure a velocityTracker is tracking every action
     * <p>
     * DOWN: clear all animation
     * MOVE:
     * 1. Calculate distanceX and distanceX
     * 2. Save RawX,RawY to {@link this#lastPointF}
     * 3. Set canTrans( split the Sizing and Translation)
     * 4. invoke {@link this#onScroll)}
     * UP:
     * 1. computeCurrentVelocity
     * 2. invoke {@link this#onFling} )}
     * 3. reset {@link this#currentDirection} and {@link this#velocityTracker}
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();

        if (velocityTracker == null)
            velocityTracker = VelocityTracker.obtain();
        MotionEvent testEvent = MotionEvent.obtain(event);
        testEvent.setLocation(event.getRawX(), event.getRawY());
        velocityTracker.addMovement(testEvent);
        testEvent.recycle();


        if (action == MotionEvent.ACTION_MOVE) {

            float distanceX = lastPointF.x - event.getRawX();
            float distanceY = lastPointF.y - event.getRawY();

            lastPointF.x = event.getRawX();
            lastPointF.y = event.getRawY();

            // calculate current gesture direction
            if (currentDirection == -1) {
                // control the angle at 30º
                if (distanceX * distanceX * 3 <= distanceY * distanceY) {
                    currentDirection = VERTICAL;
                } else if (distanceY * distanceY * 3 <= distanceX * distanceX) {
                    currentDirection = HORIZONTAL;
                } else {
                    return false;
                }
                canTrans(distanceX, distanceY);
            }
            onScroll(distanceX, distanceY);
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            velocityTracker.computeCurrentVelocity(1000);
            // We want view to its fixed status so we need fling in any condition
            onFling(velocityTracker.getXVelocity(), velocityTracker.getYVelocity());

            // reset status
            currentDirection = -1;
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }

            // Handle the warning.
            if (Math.abs(firstPointF.x - lastPointF.x) < mTouchSlop &&
                    Math.abs(firstPointF.y - lastPointF.y) < mTouchSlop)
                performClick();

            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        saveCurrentPreferSize();

        if (getChildCount() == 0) {
            setVisibility(GONE);
            return;
        }

        float oldTotalLength = mTotalLength;

        // calculate the total length
        mTotalLength = 0;
        if (getOrientation() == VERTICAL)
            for (int i = 0; i < getChildCount(); i++) {
                mTotalLength += getChildAt(i).getLayoutParams().height;
            }
        else
            for (int i = 0; i < getChildCount(); i++) {
                mTotalLength += getChildAt(i).getLayoutParams().width;
            }
        if (getChildCount() > 1)
            mTotalLength += gapInDp * (getChildCount() - 1);

        // Prevent scrollX/scrollY changes when add/remove views.
//        if (currentDirection == -1) return;

        // Since the children's size has changed
        // The formal scrollX/scrollY is deprecated
        // So we need to update it
        if (getOrientation() == VERTICAL) {
            if (getScrollY() != 0) {
                int verticalContentSize = getMeasuredHeight() - getRelatedPadding(VERTICAL);
                float previousFraction = getScrollY() / (oldTotalLength - verticalContentSize);
                int desiredScrollY = (int) (previousFraction * (mTotalLength - verticalContentSize));
                setScrollY(fixScrollY(desiredScrollY));
            }
        } else {
            if (getScrollX() != 0) {
                int horizontalContentSize = getMeasuredWidth() - getRelatedPadding(HORIZONTAL);

                float previousFraction = getScrollX() / (oldTotalLength - horizontalContentSize);
                int desiredScrollX = (int) (previousFraction * (mTotalLength - horizontalContentSize));
                setScrollX(fixScrollX(desiredScrollX));
            }
        }
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    @Override
    public boolean canScrollVertically(int direction) {
        if (getChildCount() == 0 || direction == 0) return false;

        final int range = computeVerticalScrollRange();

        if (range <= 0) return false;
        if (direction < 0) {
            return getScrollY() < range;
        } else {
            return getScrollY() > 0;
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mTotalLength - getMeasuredHeight() + getRelatedPadding(VERTICAL);
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (getChildCount() == 0 || direction == 0) return false;

        final int range = computeHorizontalScrollRange();

        if (range <= 0) return false;

        // SCROLL LEFT
        if (direction < 0) {
            return getScrollX() < range;
        } else { // SCROLL RIGHT
            return getScrollX() > 0;
        }
    }


    /**
     * 可滚动区间大小
     */
    @Override
    protected int computeHorizontalScrollRange() {
        return mTotalLength - getMeasuredWidth() + getRelatedPadding(HORIZONTAL);
    }

    @Override
    public void addView(@NonNull View child, int index) {
        LayoutParams lp = new LayoutParams(getCurrentContentSize(), getCurrentContentSize());
        lp.setMargins(0, 0, gapInDp, gapInDp);
        super.addView(child, index, lp);
        setVisibility(VISIBLE);
    }

    /**
     * Step 1: Change All children's LP
     * Step 2: Update currentFraction if needed
     */
    @Override
    public void requestLayout() {
        if (getLayoutParams() != null && getCurrentContentSize() >= preferContentSize[0] && getCurrentContentSize() <= preferContentSize[1]) {
            int contentSize = getCurrentContentSize();
            ViewGroup.LayoutParams lp;
            for (int i = 0; i < getChildCount(); i++) {
                lp = getChildAt(i).getLayoutParams();
                lp.width = contentSize;
                lp.height = contentSize;
            }
        }
        if (!(gravityFlag == Gravity.START && getTranslationX() < 0)
                && !(gravityFlag == Gravity.TOP && getTranslationY() < 0)
                && !(gravityFlag == Gravity.END && getTranslationX() > 0)
                && !(gravityFlag == Gravity.BOTTOM && getTranslationY() > 0)
                && this.getLayoutParams() != null) {
            if (getOrientation() == HORIZONTAL)
                setCurrentFraction((1f * this.getLayoutParams().height - getRelatedPadding(VERTICAL) - preferContentSize[0]) / (preferContentSize[1] - preferContentSize[0]));
            else
                setCurrentFraction((1f * this.getLayoutParams().width - getRelatedPadding(HORIZONTAL) - preferContentSize[0]) / (preferContentSize[1] - preferContentSize[0]));
        }
        super.requestLayout();
    }


    @Override
    public void setLayoutParams(@NonNull ViewGroup.LayoutParams params) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = preferContentSize[currentPreferSizeIndex] + getRelatedPadding(VERTICAL);
        } else {
            params.width = preferContentSize[currentPreferSizeIndex] + getRelatedPadding(HORIZONTAL);
        }
        super.setLayoutParams(params);
    }

    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
        if (mIndicatorView != null) mIndicatorView.setTranslationX(translationX);
        if (getOrientation() == VERTICAL) {
            float resFraction = translationX / (preferContentSize[0] + getRelatedPadding(HORIZONTAL));
            if (resFraction > 0) resFraction = -resFraction;
            setCurrentFraction(resFraction);
        }
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        if (mIndicatorView != null) mIndicatorView.setTranslationY(translationY);
        if (getOrientation() == HORIZONTAL) {
            float resFraction = translationY / (preferContentSize[0] + getRelatedPadding(VERTICAL));
            if (resFraction > 0) resFraction = -resFraction;
            setCurrentFraction(resFraction);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mIndicatorView != null) {
            mIndicatorView.setVisibility(visibility);
        }
    }
    //</editor-fold>

    //<editor-fold desc="SCROLL/Sizing/Translation ">

    /**
     * POSITIVE means slide to LEFT/UP
     * NEGATIVE means slide to RIGHT/DOWN
     */
    @SuppressLint("WrongConstant")
    public void onScroll(float distanceX, float distanceY) {
        if (currentDirection == getOrientation()) {
            // Scroll Vertically
            if (getOrientation() == VERTICAL) {
                if ((distanceY > 0 && canScrollVertically(-1)) ||
                        (distanceY < 0 && canScrollVertically(1))) {

                    int desiredScrollY = (int) (getScrollY() + distanceY);
                    scrollTo(0, fixScrollY(desiredScrollY));
                }
                // Scroll Horizontally
            } else {
                if ((distanceX > 0 && canScrollHorizontally(-1)) ||
                        (distanceX < 0 && canScrollHorizontally(1))) {

                    int desiredScrollX = getScrollX() + (int) distanceX;
                    scrollTo(fixScrollX(desiredScrollX), 0);
                }
            }
        } else {
            if (canTrans) {
                startTrans(distanceX, distanceY);
            } else {
                startSizing(distanceX, distanceY);
            }

        }

    }

    private void startTrans(float distanceX, float distanceY) {
        if (gravityFlag == Gravity.START || gravityFlag == Gravity.END)
            setTranslationX(fixTransX(getTranslationX() - distanceX));
        else
            setTranslationY(fixTransY(getTranslationY() - distanceY));
    }

    private void startSizing(float distanceX, float distanceY) {
        int currentSize = getCurrentContentSize();


        float controlVariable = (gravityFlag == Gravity.TOP || gravityFlag == Gravity.BOTTOM) ? distanceY : distanceX;

        if (!fitEnd) controlVariable = -controlVariable;

        // If view reaches its minimum/maximum size
        // we block the intent to change size this time
        boolean blocked = (currentSize == preferContentSize[0] && controlVariable < 0) ||
                (currentSize == preferContentSize[1] && controlVariable > 0);


        if (!blocked) {
            int desiredSize = (int) (currentSize + controlVariable);
            if (getOrientation() == HORIZONTAL)
                this.getLayoutParams().height = fixSize(desiredSize) + getRelatedPadding(VERTICAL);
            else
                this.getLayoutParams().width = fixSize(desiredSize) + getRelatedPadding(HORIZONTAL);
            requestLayout();
        }
    }

    @SuppressLint("WrongConstant")
    private void onFling(float xScrollVelocity, float yScrollVelocity) {
        if (currentDirection == -1) return;

        // SIZING/TRANS ANIMATION
        if ((currentDirection != getOrientation())) {
            int currentSize = getCurrentContentSize();
            float fraction = 1f * (currentSize - preferContentSize[0]) / (preferContentSize[1] - preferContentSize[0]);
            // ANIMATE THE SIZE
            if (fraction > 0 && fraction < 1) {
                startSizeAnimation(xScrollVelocity, yScrollVelocity, fraction, false);
            } else if (fraction == 0f && canTrans) {
                startTransFling(xScrollVelocity, yScrollVelocity);
            }
        } else {// SCROLL ANIMATION
            startScrollFling(xScrollVelocity, yScrollVelocity);
        }
    }

    /**
     * @param velocityX POSITIVE means swipe LEFT
     * @param velocityY POSITIVE means swipe UP
     */
    private void startTransFling(float velocityX, float velocityY) {
        float defaultSpeed = 2000f;
        if (gravityFlag == Gravity.START) {
            transXAnimation.setMinValue(-preferContentSize[0] - getRelatedPadding(HORIZONTAL))
                    .setMaxValue(0)
                    .setStartVelocity(velocityX > 0f ? defaultSpeed : -defaultSpeed).start();
        } else if (gravityFlag == Gravity.TOP) {
            transYAnimation.setMinValue(-preferContentSize[0] - getRelatedPadding(VERTICAL))
                    .setMaxValue(0)
                    .setStartVelocity(velocityY > 0f ? defaultSpeed : -defaultSpeed).start();
        } else if (gravityFlag == Gravity.END) {
            transXAnimation.setMinValue(0)
                    .setMaxValue(preferContentSize[0] + getRelatedPadding(HORIZONTAL))
                    .setStartVelocity(velocityX > 0f ? defaultSpeed : -defaultSpeed).start();
        } else {
            transYAnimation.setMinValue(0)
                    .setMaxValue(preferContentSize[0] + getRelatedPadding(VERTICAL))
                    .setStartVelocity(velocityY > 0f ? defaultSpeed : -defaultSpeed).start();
        }
    }

    private void startScrollFling(float xScrollVelocity, float yScrollVelocity) {
        if (getOrientation() == VERTICAL && yScrollVelocity != 0) {
            if (canScrollVertically(1) || canScrollVertically(-1)) {
                startScrollYAnimation(yScrollVelocity);
            }
        } else if (getOrientation() == HORIZONTAL && xScrollVelocity != 0) {
            if (canScrollHorizontally(1) || canScrollHorizontally(-1)) {
                startScrollXAnimation(xScrollVelocity);
            }
        }
    }

    private void startScrollXAnimation(float xScrollVelocity) {
        scrollXAnimation.setStartVelocity(-xScrollVelocity)
                .setMaxValue(computeHorizontalScrollRange())
                .setMinValue(0f)
                .start();
    }

    private void startScrollYAnimation(float yScrollVelocity) {
        scrollYAnimation.setStartVelocity(-yScrollVelocity)
                .setMaxValue(computeVerticalScrollRange())
                .setMinValue(0f)
                .start();
    }


    private void startSizeAnimation(float velocityX, float velocityY, float fraction, boolean isByUser) {
        float velocity = 0f;
        float defaultSpeed = 1000f;

        if(velocityX != 0) velocityX = velocityX > 0f ? defaultSpeed : -defaultSpeed;
        if(velocityY != 0) velocityY = velocityY > 0f ? defaultSpeed : -defaultSpeed;

        switch (gravityFlag){
            case Gravity.START:
                velocity = velocityX;
                break;
            case Gravity.TOP:
                velocity = velocityY;
                break;
            case Gravity.END:
                velocity = -velocityX;
                break;
            case Gravity.BOTTOM:
                velocity = -velocityY;
                break;
        }

        if (fraction == 0f || fraction == 1) {
            if (isByUser) velocity = (fraction == 1) ? -defaultSpeed : defaultSpeed;
            else return;
        }

        if (velocity == 0f) {
            velocity = (fraction >= 0.5f) ? defaultSpeed : -defaultSpeed;
        }
        sizeAnimation.setStartVelocity(velocity).start();
    }
    //</editor-fold>

    //<editor-fold desc="Helper function">
    private int getCurrentContentSize() {
        return getOrientation() == VERTICAL
                ? getLayoutParams().width - getPaddingLeft() - getPaddingRight()
                : getLayoutParams().height - getPaddingTop() - getPaddingBottom();
    }

    /**
     * @return given padding according to the orientation
     */
    private int getRelatedPadding(int orientation) {
        if (orientation == HORIZONTAL)
            return getPaddingLeft() + getPaddingRight();
        else if (orientation == VERTICAL)
            return getPaddingTop() + getPaddingBottom();
        else return getRelatedPadding(getOrientation());
    }

    /**
     * If this gesture action can reach to translation part
     */
    @SuppressLint("WrongConstant")
    private void canTrans(float distanceX, float distanceY) {
        boolean res = false;

        // We don't handle this when is a scroll action
        if (getOrientation() != currentDirection) {

            switch (this.gravityFlag) {
                case Gravity.START: {
                    if (getTranslationX() < 0 || (distanceX > 0 && getCurrentContentSize() == preferContentSize[0]))
                        res = true;
                    break;
                }
                case Gravity.TOP: {
                    if (getTranslationY() < 0 || (distanceY > 0 && getCurrentContentSize() == preferContentSize[0]))
                        res = true;
                    break;
                }
                case Gravity.END: {
                    if (getTranslationX() > 0 || (distanceX < 0 && getCurrentContentSize() == preferContentSize[0]))
                        res = true;
                    break;
                }
                case Gravity.BOTTOM: {
                    if (getTranslationY() > 0 || (distanceY < 0 && getCurrentContentSize() == preferContentSize[0]))
                        res = true;
                    break;
                }
            }
        }

        canTrans = res;
    }

    private void ensureCurrentGravityFlag() {
        if (getOrientation() == VERTICAL)
            this.gravityFlag = fitEnd ? Gravity.END : Gravity.START;
        else
            this.gravityFlag = fitEnd ? Gravity.BOTTOM : Gravity.TOP;
    }

    private int fixSize(int size) {
        if (size < preferContentSize[0]) return preferContentSize[0];
        else return Math.min(size, preferContentSize[1]);
    }

    private int fixScrollX(int desiredScrollX) {
        if (desiredScrollX < 0) return 0;
        else {
            int range = computeHorizontalScrollRange();
            return Math.min(desiredScrollX, range);
        }
    }

    private int fixScrollY(int desiredScrollY) {
        if (desiredScrollY < 0) return 0;
        else {
            int range = computeVerticalScrollRange();
            return Math.min(desiredScrollY, range);
        }
    }

    private float fixTransX(float desiredTransX) {
        int min;
        int max;
        if (fitEnd) {
            min = 0;
            max = preferContentSize[0] + getRelatedPadding(HORIZONTAL);

        } else {
            min = -(preferContentSize[0] + getRelatedPadding(HORIZONTAL));
            max = 0;
        }

        if (desiredTransX < min) return min;
        else if (desiredTransX > max) return max;
        else return desiredTransX;
    }

    private float fixTransY(float desiredTransY) {
        int min;
        int max;
        if (fitEnd) {
            min = 0;
            max = preferContentSize[0] + getRelatedPadding(VERTICAL);

        } else {
            min = -(preferContentSize[0] + getRelatedPadding(VERTICAL));
            max = 0;
        }

        if (desiredTransY < min) return min;
        else if (desiredTransY > max) return max;
        else return desiredTransY;
    }

    public void saveCurrentPreferSize() {
        int size = getCurrentContentSize();
        if (size == preferContentSize[0]) currentPreferSizeIndex = 0;
        else if (size == preferContentSize[1]) currentPreferSizeIndex = 1;
    }


    public void setGapInDp(int gapInDp) {
        this.gapInDp = (int) dp2px(gapInDp);
    }
    //</editor-fold>

    float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    //<editor-fold desc="INDICATOR">
    private void setCurrentFraction(float currentFraction) {
        this.currentFraction = currentFraction;
        if (mIndicatorView != null)
            mIndicatorView.setCurrentFraction(currentFraction);
    }

    public void attachToIndicator(@NonNull IndicatorView indicatorView) {
        this.mIndicatorView = indicatorView;
        mIndicatorView.setCurrentFraction(currentFraction);
        mIndicatorView.setVisibility(getVisibility());
        mIndicatorView.setOnClickListener(v -> {
            if (currentFraction == -1f) {
                startTransFling(gravityFlag == Gravity.START ? 1 : -1, gravityFlag == Gravity.TOP ? 1 : -1);
            } else if (currentFraction == 1f) {
                startSizeAnimation(0f, 0f, 1f, true);
            }
        });
    }
    //</editor-fold>

    /**
     * Help function
     *
     * @return a CardView contains a TextView
     */
    @NonNull
    public static CardView getChildAudioCardView(@NonNull Context context, @Nullable Object tag, @Nullable String title) {
        CardView cardView = new CardView(context);

        TextView titleText = new TextView(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = CENTER;
        titleText.setLayoutParams(lp);
        titleText.setGravity(CENTER);
        titleText.setText(title);

        cardView.setTag(tag);
        cardView.setRadius(dp2px(context, 16));
        cardView.setCardBackgroundColor(Color.rgb(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256)));
        cardView.addView(titleText);
        return cardView;
    }

    /**
     * Help function
     *
     * @return a CardView contains a Texture
     */
    @NonNull
    public static CardView getChildVideoCardView(@NonNull Context context, @Nullable Object tag) {
        CardView cardView = new CardView(context);

        TextureView textureView = new TextureView(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        textureView.setLayoutParams(lp);

        cardView.setRadius(dp2px(context, 16));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.addView(textureView);
        if (tag != null)
            cardView.setTag(tag);
        return cardView;
    }

    public static float dp2px(@NonNull Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}