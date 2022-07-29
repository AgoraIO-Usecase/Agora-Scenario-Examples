package io.agora.uiwidget.function;

import android.app.Dialog;
import android.content.Context;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import io.agora.uiwidget.R;
import pl.droidsonroids.gif.GifImageView;

public class GiftAnimPlayDialog extends Dialog {

    private GifImageView mImageView;

    public GiftAnimPlayDialog(@NonNull Context context) {
        this(context, R.style.GiftAnimPlayDialog);
    }

    public GiftAnimPlayDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    private void init() {
        FrameLayout layout = new FrameLayout(getContext());
        mImageView = new GifImageView(getContext());
        layout.addView(mImageView);
        setContentView(layout);
    }

    public GiftAnimPlayDialog setAnimRes(@DrawableRes int animRes){
        mImageView.setImageResource(animRes);
        pl.droidsonroids.gif.GifDrawable drawable = (pl.droidsonroids.gif.GifDrawable)mImageView.getDrawable();
        drawable.start();
        mImageView.postDelayed(GiftAnimPlayDialog.this::dismiss, drawable.getDuration());
        return this;
    }

    @Override
    public void show() {
        super.show();
    }

}
