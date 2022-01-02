package io.agora.uiwidget.function;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import io.agora.uiwidget.R;


public class LiveBottomView extends RelativeLayout{
    public static final int FUN_ICON_GIFT = R.drawable.live_bottom_btn_gift;
    public static final int FUN_ICON_PK = R.drawable.live_bottom_btn_pk;
    public static final int FUN_ICON_SHOP_CAR = R.drawable.live_bottom_btn_shopcart;
    public static final int FUN_ICON_MUSIC = R.drawable.live_bottom_button_music;
    public static final int FUN_ICON_MUSIC_LIGHT = R.drawable.live_bottom_button_music_light;
    public static final int FUN_ICON_BEAUTY = R.drawable.live_bottom_button_beauty;



    private AppCompatImageView mCancel;
    private AppCompatImageView mMore;
    private AppCompatImageView mFun1;
    private AppCompatImageView mFun2;
    private AppCompatTextView mInputText;
    private boolean mLight = false;

    public LiveBottomView(Context context) {
        this(context, null);
    }

    public LiveBottomView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveBottomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(attrs != null){
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LiveBottomView);
            mLight = typedArray.getBoolean(R.styleable.LiveBottomView_isLight, false);
            typedArray.recycle();
        }
        init();
    }

    private void init(){
        int layout = mLight ?
                R.layout.live_bottom_button_layout_light :
                R.layout.live_bottom_button_layout;
        LayoutInflater.from(getContext()).inflate(layout, this, true);
        mFun1 = findViewById(R.id.live_bottom_btn_fun1);
        mFun2 = findViewById(R.id.live_bottom_btn_fun2);
        mInputText = findViewById(R.id.live_bottom_message_input_hint);
        mCancel = findViewById(R.id.live_bottom_btn_close);
        mMore = findViewById(R.id.live_bottom_btn_more);
    }

    public LiveBottomView setupInputText(boolean visible, OnClickListener listener){
        mInputText.setVisibility(visible ? View.VISIBLE: View.GONE);
        mInputText.setOnClickListener(listener);
        return this;
    }

    public LiveBottomView setupCloseBtn(boolean visible, OnClickListener listener){
        mCancel.setVisibility(visible ? View.VISIBLE: View.GONE);
        mCancel.setOnClickListener(listener);
        return this;
    }

    public LiveBottomView setupMoreBtn(boolean visible, OnClickListener listener){
        mMore.setVisibility(visible ? View.VISIBLE: View.GONE);
        mMore.setOnClickListener(listener);
        return this;
    }


    public LiveBottomView setFun1Visible(boolean visible){
        mFun1.setVisibility(visible? View.VISIBLE: View.GONE);
        return this;
    }

    public LiveBottomView setFun1ImageResource(@DrawableRes int drawable){
        mFun1.setImageResource(drawable);
        return this;
    }

    public LiveBottomView setFun1ClickListener(OnClickListener listener){
        mFun1.setOnClickListener(listener);
        return this;
    }

    public LiveBottomView setFun1Activated(boolean activated){
        mFun1.setActivated(activated);
        return this;
    }


    public LiveBottomView setFun2Visible(boolean visible){
        mFun2.setVisibility(visible? View.VISIBLE: View.GONE);
        return this;
    }

    public LiveBottomView setFun2ImageResource(@DrawableRes int drawable){
        mFun2.setImageResource(drawable);
        return this;
    }

    public LiveBottomView setFun2ClickListener(OnClickListener listener){
        mFun2.setOnClickListener(listener);
        return this;
    }

    public LiveBottomView setFun2Activated(boolean activated){
        mFun2.setActivated(activated);
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = getResources().getDimensionPixelSize(R.dimen.live_bottom_layout_height);
        setMeasuredDimension(width, height);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightSpec);
    }

}
