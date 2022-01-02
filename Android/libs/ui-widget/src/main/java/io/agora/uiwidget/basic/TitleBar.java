package io.agora.uiwidget.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.uiwidget.R;

public class TitleBar extends FrameLayout {
    private TextView mTitleTv;
    private View mDeliverView;
    private ImageView mBackIv;
    private ImageView mUserIv;
    private ImageView mBgIv;

    public TitleBar(@NonNull Context context) {
        this(context, null);
    }

    public TitleBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitleBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.titile_bar_layout, this, true);
        mTitleTv = findViewById(R.id.title_bar_name);
        mDeliverView = findViewById(R.id.title_bar_deliver);
        mBackIv = findViewById(R.id.title_bar_back);
        mUserIv = findViewById(R.id.title_bar_icon_user);
        mBgIv = findViewById(R.id.title_bar_bg);
    }

    public void setTitleName(CharSequence name, int color){
        mTitleTv.setText(name);
        if(color != 0){
            mTitleTv.setTextColor(color);
        }
    }

    public void setBgDrawable(@DrawableRes int drawableRes){
        mBgIv.setImageResource(drawableRes);
    }

    public void setUserIcon(boolean visible, @DrawableRes int drawableRes, OnClickListener onClickListener){
        mUserIv.setOnClickListener(onClickListener);
        mUserIv.setVisibility(visible ? View.VISIBLE : View.GONE);
        if(drawableRes != View.NO_ID){
            mUserIv.setImageResource(drawableRes);
        }
    }

    public void setBackIcon(boolean visible, @DrawableRes int drawableRes, OnClickListener onClickListener){
        mBackIv.setOnClickListener(onClickListener);
        mBackIv.setVisibility(visible ? View.VISIBLE : View.GONE);
        if(drawableRes != View.NO_ID){
            mBackIv.setImageResource(drawableRes);
        }
    }


}
