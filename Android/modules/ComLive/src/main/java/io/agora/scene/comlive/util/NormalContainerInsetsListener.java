package io.agora.scene.comlive.util;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseUtil;

/**
 * Normal WindowInsetsListener just for window container to adjust its layout(mainly Padding)
 */
public class NormalContainerInsetsListener implements OnApplyWindowInsetsListener {

    @SuppressLint("UnknownNullness")
    @Override
    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
        Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        v.setPadding(inset.left, inset.top, inset.right, inset.bottom);
        return WindowInsetsCompat.CONSUMED;
    }

}
