package io.agora.uiwidget.function;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Size;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.io.IOException;
import java.util.Locale;

import io.agora.uiwidget.R;


public class LiveRoomUserView extends RelativeLayout {

    private int mHeight;
    private int mIconSize;
    private int mIconMargin;
    private AppCompatTextView mCountText;
    private RelativeLayout mIconLayout;
    private View mNotification;
    private View mTotalLayout;

    private int leftestUserIconId;
    private int userIconMaxCount = 4;

    public LiveRoomUserView(Context context) {
        this(context, null);
    }

    public LiveRoomUserView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public LiveRoomUserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        boolean isLight = false;
        if(attrs != null){
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LiveRoomUserView);
            isLight = typedArray.getBoolean(R.styleable.LiveRoomUserView_isLight, false);
            typedArray.recycle();
        }
        init(isLight);
    }

    public void init(boolean lightMode) {
        mHeight = getResources().getDimensionPixelSize(R.dimen.live_room_user_height);
        mIconSize = getResources().getDimensionPixelSize(R.dimen.live_room_user_icon_size);
        mIconMargin = getResources().getDimensionPixelSize(R.dimen.live_room_user_icon_margin);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(lightMode ?
                R.layout.live_room_user_layout_light :
                R.layout.live_room_user_layout, this, true);
        mIconLayout = layout.findViewById(R.id.icon_layout);
        mCountText = layout.findViewById(R.id.live_participant_count_text);
        mTotalLayout = layout.findViewById(R.id.live_participant_total_layout);
        mNotification = findViewById(R.id.notification_point);
        setGravity(Gravity.CENTER_VERTICAL);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, mHeight);
        int heightSpec = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightSpec);
    }

    public void setUserCount(int total) {
        String value = countToString(total);
        mCountText.setText(value);
    }

    private String countToString(int number) {
        if (number <  1e3f) {
            return String.valueOf(number);
        } else if (number < 1e6f) {
            int quotient = (int) (number / 1e3f);
            return String.format(Locale.getDefault(), "%dK", quotient);
        } else if (number < 1e9f) {
            int quotient = (int) (number / 1e6f);
            return String.format(Locale.getDefault(), "%dM", quotient);
        } else {
            int quotient = (int) (number / 1e9f);
            return String.format(Locale.getDefault(), "%dB", quotient);
        }
    }

    public void showNotification(boolean show) {
        if (mNotification != null) {
            int visibility = show ? VISIBLE : GONE;
            mNotification.setVisibility(visibility);
        }
    }

    public boolean notificationShown() {
        return mNotification != null &&
                mNotification.getVisibility() == VISIBLE;
    }

    public void setUserIconMaxCount(@Size(min = 1, max = 4) int userIconMaxCount) {
        this.userIconMaxCount = userIconMaxCount;
        int childCount = mIconLayout.getChildCount();
        while (childCount > userIconMaxCount){
            mIconLayout.removeViewAt(0);
            childCount --;
        }
    }

    public void resetUserIcon(){
        leftestUserIconId = 0;
        mIconLayout.removeAllViews();
    }

    public void removeUserIconByTag(Object tag){
        View view = mIconLayout.findViewWithTag(tag);
        if(view != null){
            mIconLayout.removeView(view);
            leftestUserIconId = mIconLayout.getChildAt(mIconLayout.getChildCount() - 1).getId();
        }
    }

    public void addUserIconFromAssets(String assetsName, Object tag){
        RoundedBitmapDrawable drawable = null;
        try {
            drawable = RoundedBitmapDrawableFactory.create(getResources(),
                    getResources().getAssets().open(assetsName));
            drawable.setCircular(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        addUserIconInner(drawable, tag);
    }

    public void addUserIcon(@DrawableRes int drawableId, Object tag){
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(),
                BitmapFactory.decodeResource(getResources(), drawableId));
        drawable.setCircular(true);

        addUserIconInner(drawable, tag);
    }

    private void addUserIconInner(RoundedBitmapDrawable drawable, Object tag) {
        if(userIconMaxCount <= 0 || drawable == null){
            return;
        }
        if(mIconLayout.getChildCount() >= userIconMaxCount){
            mIconLayout.removeViewAt(0);
            if(mIconLayout.getChildCount() > 0){
                View rightestView = mIconLayout.getChildAt(0);
                LayoutParams params = (LayoutParams) rightestView.getLayoutParams();
                params.removeRule(RelativeLayout.LEFT_OF);
                params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                rightestView.setLayoutParams(params);
            }
        }

        LayoutParams params = new
                LayoutParams(mIconSize, mIconSize);
        params.rightMargin = mIconMargin;
        if (leftestUserIconId > 0) {
            params.addRule(RelativeLayout.LEFT_OF, leftestUserIconId);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        }

        int iconViewId = View.generateViewId();
        this.leftestUserIconId = iconViewId;
        AppCompatImageView imageView = new AppCompatImageView(getContext());
        imageView.setId(iconViewId);
        imageView.setImageDrawable(drawable);
        imageView.setTag(tag);
        mIconLayout.addView(imageView, params);
    }

    public void setTotalLayoutClickListener(OnClickListener listener){
        mTotalLayout.setOnClickListener(listener);
    }

}
