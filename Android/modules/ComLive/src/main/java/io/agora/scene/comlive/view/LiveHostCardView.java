package io.agora.scene.comlive.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

public class LiveHostCardView extends CardView {

    @NonNull
    public TextureView renderTextureView;

    public LiveHostCardView(@NonNull Context context) {
        this(context, null);
    }

    public LiveHostCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LiveHostCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(@NonNull Context context){
        if (!isInEditMode()) {
            setCardElevation(0);
            setRadius(0);
            setCardBackgroundColor(Color.TRANSPARENT);
        }
        renderTextureView = new TextureView(context);
        addView(renderTextureView);
    }
}
