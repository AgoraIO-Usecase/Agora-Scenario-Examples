package io.agora.example.base;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AttrRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
@Keep
public class BaseUtil {

    public static void checkPermissionBeforeNextOP(@NonNull ComponentActivity activity, @NonNull ActivityResultLauncher<String[]> launcher, @NonNull String[] permissions, @NonNull PermissionResultCallback<String[]> callback) {
        checkPermissionBeforeNextOP(activity, activity, launcher, permissions, callback);
    }

    public static void checkPermissionBeforeNextOP(@NonNull Fragment fragment, @NonNull ActivityResultLauncher<String[]> launcher, @NonNull String[] permissions, @NonNull PermissionResultCallback<String[]> callback) {
        checkPermissionBeforeNextOP(fragment.requireContext(), fragment, launcher, permissions, callback);
    }

    private static void checkPermissionBeforeNextOP(@NonNull Context context,@NonNull Object o, @NonNull ActivityResultLauncher<String[]> launcher, @NonNull String[] permissions, @NonNull PermissionResultCallback<String[]> callback) {
        ComponentActivity activity = null;
        Fragment fragment = null;
        ActivityResultCaller caller;

        if (o instanceof Fragment) {
            fragment = (Fragment) o;
            caller = fragment;
        } else if (o instanceof ComponentActivity) {
            activity = (ComponentActivity) o;
            caller = activity;
        } else {
            return;
        }

        // 小于 M 无需控制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback.onAllPermissionGranted();
            return;
        }

        // 检查权限是否通过
        boolean needRequest = false;

        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (!needRequest) {
            callback.onAllPermissionGranted();
            return;
        }

        boolean requestDirectly = true;
        if (fragment != null) {
            for (String requiredPermission : permissions)
                if (fragment.shouldShowRequestPermissionRationale(requiredPermission)) {
                    requestDirectly = false;
                    break;
                }
        } else {
            for (String requiredPermission : permissions)
                if (activity.shouldShowRequestPermissionRationale(requiredPermission)) {
                    requestDirectly = false;
                    break;
                }
        }
        // 直接申请
        if (requestDirectly) {
            launcher.launch(permissions);
        }
        // 显示申请理由
        else callback.showReasonDialog(permissions);
    }

    @NonNull
    public static ActivityResultLauncher<String[]> registerForActivityResult(@NonNull ActivityResultCaller caller, @NonNull PermissionResultCallback<String[]> callback) {
        return caller.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> {
            List<String> permissionsRefused = new ArrayList<>();
            for (String s : res.keySet()) {
                if (Boolean.TRUE != res.get(s))
                    permissionsRefused.add(s);
            }
            if (permissionsRefused.isEmpty())
                callback.onAllPermissionGranted();
            else {
                String[] refusedArray = new String[permissionsRefused.size()];
                callback.onPermissionRefused(permissionsRefused.toArray(refusedArray));
            }
        });
    }

    public interface PermissionResultCallback<T> {

        default void onCreate(){

        }

        void onAllPermissionGranted();

        default void onPermissionRefused(T refusedPermissions) {
        }

        default void showReasonDialog(T refusedPermissions) {
        }
    }

    public static void toast(@NonNull Context context, @NonNull String msg) {
        toast(context, msg, false);
    }

    public static void toast(@NonNull Context context, @NonNull String msg, boolean longTime) {
        int time = Toast.LENGTH_SHORT;
        if (longTime) time = Toast.LENGTH_LONG;
        Toast.makeText(context, msg, time).show();
    }

    public static void logD(@Nullable String msg) {
        if (BuildConfig.DEBUG)
            logD("Agora-BaseUtil", msg);
    }

    public static void logD(@NonNull String tag, @Nullable String msg) {
        if (BuildConfig.DEBUG)
            Log.d(tag, msg);
    }

    public static void logE(@Nullable String msg) {
        if (BuildConfig.DEBUG)
            logE("Agora-BaseUtil", msg);
    }

    public static void logE(@NonNull String tag, @Nullable String msg) {
        if (BuildConfig.DEBUG)
            Log.e(tag, msg);
    }

    public static void logE(@NonNull Throwable e) {
        if (BuildConfig.DEBUG)
            logE("Agora-BaseUtil", e);
    }


    public static void logE(@NonNull String tag, @NonNull Throwable e) {
        if (BuildConfig.DEBUG)
            Log.e(tag, e.getMessage());
    }

    public static float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static float sp2px(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }



    //<editor-fold desc="View related">

    //<editor-fold desc="Keyboard related">
    public static void hideKeyboardCompat(@NonNull View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } else {
            hideKeyboard(view);
        }
    }

    public static void hideKeyboard(@NonNull View view) {
        WindowInsetsControllerCompat con = ViewCompat.getWindowInsetsController(view);
        if (con != null) con.hide(WindowInsetsCompat.Type.ime());
    }

    public static void showKeyboardCompat(@NonNull View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else {
            showKeyboard(view);
        }
    }

    public static void showKeyboard(@NonNull View view) {
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(view);
        if (controller != null) controller.show(WindowInsetsCompat.Type.ime());
    }
    //</editor-fold>

    public static void shakeViewAndVibrateToAlert(@NonNull View view) {
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
    public static int getAttrResId(@NonNull Context context, @AttrRes int resId) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(resId, tv, true);
        return tv.resourceId;
    }

    public static int getColorInt(@NonNull Context context, @AttrRes int resId) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(resId, tv, true);

        if (tv.type == TypedValue.TYPE_STRING)
            return ContextCompat.getColor(context, tv.resourceId);
        return tv.data;
    }

    @NonNull
    public static ColorStateList getScrimColorSelector(@NonNull Context context){
        int scrimColor = getColorInt(context, R.attr.scrimBackground);
        return new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_hovered, android.R.attr.state_enabled},
                new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled},
                new int[]{},
        }, new int[]{scrimColor, scrimColor, Color.TRANSPARENT});
    }

    @Nullable
    public static <T> Class<T> getGenericClass(@NonNull Class<?> clz, int index) {
        Type type = clz.getGenericSuperclass();
        if (type == null) return null;
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[index];
    }

    @Nullable
    public static Object getViewBinding(@NonNull Class<?> bindingClass, @NonNull LayoutInflater inflater) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class);
            return inflateMethod.invoke(null, inflater);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T getViewBinding(@NonNull Class<T> bindingClass, @NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class, ViewGroup.class, Boolean.TYPE);
            return (T) inflateMethod.invoke(null, inflater, container, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //</editor-fold>
}