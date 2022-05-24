package io.agora.example.base;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsAnimationControlListenerCompat;
import androidx.core.view.WindowInsetsAnimationControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings("unchecked")
@Keep
public class BaseUtil {

    public static void toast(Context context, String msg) {
        toast(context, msg, false);
    }

    public static void toast(Context context, String msg, boolean longTime) {
        int time = Toast.LENGTH_SHORT;
        if (longTime) time = Toast.LENGTH_LONG;
        Toast.makeText(context, msg, time).show();
    }

    public static void logD(String msg) {
        if (BuildConfig.DEBUG)
            Log.d("Agora-BaseUtil", msg);
    }

    public static void logE(String msg) {
        if (BuildConfig.DEBUG)
            Log.e("Agora-BaseUtil", msg);
    }

    public static void logE(Throwable e) {
        if (BuildConfig.DEBUG)
            Log.e("Agora-BaseUtil", e.getMessage());
    }

    public static float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static float sp2px(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    public static void hideKeyboard(Window window, View view) {
        WindowInsetsControllerCompat con = WindowCompat.getInsetsController(window, view);
        if (con != null) con.hide(WindowInsetsCompat.Type.ime());
        view.clearFocus();
    }

    public static void showKeyboardCompat(@NonNull View view){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            InputMethodManager systemService = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            systemService.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }else{
            showKeyboard(view);
        }
    }

    public static void showKeyboard(@NonNull View view) {
        int type = WindowInsetsCompat.Type.ime();
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(view);
        if (controller != null) {
            controller.show(type);
        }
    }

    @NonNull
    public static WindowInsetsAnimationControlListenerCompat listener = new WindowInsetsAnimationControlListenerCompat() {

        @Override
        public void onReady(@NonNull WindowInsetsAnimationControllerCompat controller, int types) {

        }

        @Override
        public void onFinished(@NonNull WindowInsetsAnimationControllerCompat controller) {

        }

        @Override
        public void onCancelled(@Nullable WindowInsetsAnimationControllerCompat controller) {

        }
    };

    public static void showKeyboard(Window window, View view) {

        WindowInsetsControllerCompat con = WindowCompat.getInsetsController(window, view);
        if (con != null) con.show(WindowInsetsCompat.Type.ime());
    }

    public static void shakeViewAndVibrateToAlert(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        view.postDelayed(() -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY), 100);
        view.postDelayed(() -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY), 300);

        ObjectAnimator o = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 50f, -50f, 0f, 50f, 0f);
        o.setInterpolator(new OvershootInterpolator());
        o.setDuration(500L);
        o.start();
    }

    /**
     * android.R.attr.actionBarSize
     */
    public static int getAttrResId(Context context, @AttrRes int resId) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(resId, tv, true);
        return tv.resourceId;
    }

    public static int getColorInt(Context context, @AttrRes int resId) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(resId, tv, true);

        if (tv.type == TypedValue.TYPE_STRING)
            return ContextCompat.getColor(context, tv.resourceId);
        return tv.data;
    }

    public static <T> Class<T> getGenericClass(Class<?> clz, int index) {
        Type type = clz.getGenericSuperclass();
        if (type == null) return null;
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[index];
    }

    public static Object getViewBinding(Class<?> bindingClass, LayoutInflater inflater) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class);
            inflateMethod.setAccessible(true);
            return inflateMethod.invoke(null, inflater);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T getViewBinding(Class<T> bindingClass, LayoutInflater inflater, ViewGroup container) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class, ViewGroup.class, Boolean.TYPE);
            return (T) inflateMethod.invoke(null, inflater, container, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}