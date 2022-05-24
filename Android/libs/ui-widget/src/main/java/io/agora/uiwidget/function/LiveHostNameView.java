package io.agora.uiwidget.function;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.io.IOException;

import io.agora.uiwidget.R;

public class LiveHostNameView extends RelativeLayout {
    private static final int IMAGE_VIEW_ID = 1 << 4;

    private int mMaxWidth;
    private int mHeight;
    private AppCompatImageView mIconImageView;
    private AppCompatTextView mNameTextView;

    public LiveHostNameView(Context context) {
        this(context, null);
    }

    public LiveHostNameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveHostNameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        boolean isLight = false;
        if(attrs != null){
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LiveHostNameView);
            isLight = typedArray.getBoolean(R.styleable.LiveHostNameView_isLight, false);
            typedArray.recycle();
        }
        init(isLight);
    }

    private void init(boolean lightMode) {
        mMaxWidth = getResources().getDimensionPixelSize(R.dimen.live_host_name_max_width);
        mHeight = getResources().getDimensionPixelSize(R.dimen.live_host_name_height);

        if (lightMode) {
            setBackgroundResource(R.drawable.live_host_name_bg_light);
        } else {
            setBackgroundResource(R.drawable.live_host_name_bg);
        }

        LayoutParams params;

        mIconImageView = new AppCompatImageView(getContext());
        mIconImageView.setId(IMAGE_VIEW_ID);
        addView(mIconImageView);
        int iconPadding = getResources().getDimensionPixelSize(R.dimen.live_host_name_icon_padding);
        params = (LayoutParams) mIconImageView.getLayoutParams();
        int iconSize = mHeight - iconPadding * 2;
        params.width = iconSize;
        params.height = iconSize;
        params.leftMargin = iconPadding;
        params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        mIconImageView.setLayoutParams(params);

        mNameTextView = new AppCompatTextView(getContext());
        addView(mNameTextView);
        params = (LayoutParams) mNameTextView.getLayoutParams();
        params.addRule(RelativeLayout.END_OF, IMAGE_VIEW_ID);
        params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        params.leftMargin = mHeight / 5;
        params.rightMargin = params.leftMargin;
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        mNameTextView.setLayoutParams(params);

        int textSize = getResources().getDimensionPixelSize(R.dimen.live_host_name_text_size);
        mNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        if (lightMode) {
            mNameTextView.setTextColor(Color.BLACK);
        } else {
            mNameTextView.setTextColor(Color.WHITE);
        }

        mNameTextView.setSingleLine(true);
        mNameTextView.setFocusable(true);
        mNameTextView.setFocusableInTouchMode(true);
        mNameTextView.setSelected(true);
        mNameTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        mNameTextView.setMarqueeRepeatLimit(-1);
        mNameTextView.setTextAlignment(TextView.TEXT_ALIGNMENT_GRAVITY);
        mNameTextView.setGravity(Gravity.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMaxWidth, mHeight);
        int widthSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, heightSpec);
    }

    public void setName(String name) {
        mNameTextView.setText(name);
    }

    public void setIcon(@DrawableRes int drawableId) {
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(),
                BitmapFactory.decodeResource(getResources(), drawableId));
        drawable.setCircular(true);
        mIconImageView.setImageDrawable(drawable);
    }

    /**
     * For development only, test fake user icon
     * @param name
     */
    public void setAssetsIconResource(String name) {
        RoundedBitmapDrawable drawable = null;
        try {
            drawable = RoundedBitmapDrawableFactory.create(getResources(),
                    getResources().getAssets().open(name));
            drawable.setCircular(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIconImageView.setImageDrawable(drawable);
    }
}
