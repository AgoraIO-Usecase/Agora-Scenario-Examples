package io.agora.scene.comlive.util;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import io.agora.example.base.BaseUtil;

/**
 * Add translationY to a view when Keyboard shows
 * TODO currently just support the RoomFragment, considering to support common view
 */
public class TranslateInsetsAnimationListener extends WindowInsetsAnimationCompat.Callback implements OnApplyWindowInsetsListener {
    private final int desiredAnimationInsets = WindowInsetsCompat.Type.ime();
    private final View view;
    private WindowInsetsCompat lastWindowInsets = null;

    private boolean deferredThisTime;

    public TranslateInsetsAnimationListener(@NonNull View view) {
        super(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP);
        this.view = view;
    }

    @SuppressLint("UnknownNullness")
    @Override
    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
        // Keep this time
        lastWindowInsets = insets;
        boolean imeVisible = insets.isVisible(desiredAnimationInsets);
        if (!imeVisible && view != null)
            view.setVisibility(GONE);

        boolean imeVisibleOnScreen = insets.getInsets(desiredAnimationInsets).bottom > 0f;
        setTranslationY(insets, imeVisibleOnScreen ? 1f : 0f);
        // Normal-op
        return insets;
    }

    @Override
    public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        if ((animation.getTypeMask() & desiredAnimationInsets) != 0 && view != null && view.getVisibility() == View.VISIBLE)
            deferredThisTime = true;
    }

    @NonNull
    @Override
    public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
        if (deferredThisTime && !runningAnimations.isEmpty())
            setTranslationY(insets, runningAnimations.get(0).getInterpolatedFraction());
        return insets;
    }

    @Override
    public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
        if (deferredThisTime && (animation.getTypeMask() & desiredAnimationInsets) != 0){
            deferredThisTime = false;
            if (lastWindowInsets != null && view != null) {
                setTranslationY(lastWindowInsets, 1);
                ViewCompat.dispatchApplyWindowInsets(view, lastWindowInsets);
                view.post(this::checkFocus);
            }
        }

    }

    private void setTranslationY(WindowInsetsCompat insets, float fraction){
        if (view != null){
            Insets desiredAnimationInset = insets.getInsets(desiredAnimationInsets);
            float desiredY = ComLiveUtil.lerp(view.getMeasuredHeight(), -desiredAnimationInset.bottom, fraction);
            view.setTranslationY(desiredY);
        }
    }

    private void checkFocus() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
        if (insets != null) {
            boolean imeVisible = insets.isVisible(desiredAnimationInsets);
            if (imeVisible) {
                view.requestFocus();
            } else {
                view.clearFocus();
            }
        }
    }
}
