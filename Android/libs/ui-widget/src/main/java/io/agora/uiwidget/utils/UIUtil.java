package io.agora.uiwidget.utils;

import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.agora.uiwidget.basic.BindingViewHolder;

public class UIUtil {
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static void runOnUiThreadSafe(WeakReference<Runnable> runRef) {
        if (sMainHandler.getLooper().getThread() == Thread.currentThread()) {
            Runnable run = runRef.get();
            if (run != null) {
                run.run();
            }
        } else {
            if(runRef.get() != null){
                sMainHandler.post(() -> {
                    Runnable run = runRef.get();
                    if (run != null) {
                        run.run();
                    }
                });
            }
        }
    }

    public static void runOnUiThread(Object token, Runnable runnable){
        if (sMainHandler.getLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        }else{
            runOnUiThread(token, runnable, 0);
        }
    }

    public static void runOnUiThread(Object token, Runnable runnable, int delayMillis){
        sMainHandler.postAtTime(runnable, token, SystemClock.uptimeMillis() + delayMillis);
    }

    public static void cleanUiThreadRuns(Object token){
        sMainHandler.removeCallbacksAndMessages(token);
    }

    public static <T extends View> T setViewCircle(T view) {
        view.post(() -> {
            Rect rect = new Rect();
            view.getGlobalVisibleRect(rect);
            float radius = Math.min(rect.width(), rect.height()) * 1.0f / 2;
            setViewRound(view, radius);
        });
        return view;
    }

    public static <T extends View> T setViewRound(T view, float radius) {
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                outline.setRoundRect(new Rect(0, 0, rect.width(), rect.height()), radius);
            }
        });
        view.setClipToOutline(true);
        return view;
    }

    @NonNull
    public static <Binding extends ViewBinding> BindingViewHolder<Binding> createBindingViewHolder(Class<?> aClass, @NonNull ViewGroup parent, int index) {
        Type genericSuperclass = aClass.getGenericSuperclass();
        Type[] actualTypeArguments;
        if (!(genericSuperclass instanceof ParameterizedType)) {
            return createBindingViewHolder(aClass.getSuperclass(), parent, index);
        } else {
            actualTypeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
            if (actualTypeArguments.length < (index + 1)) {
                return createBindingViewHolder(aClass.getSuperclass(), parent, index);
            }
        }

        Class<Binding> c = (Class<Binding>) actualTypeArguments[index];
        Binding binding = null;
        try {
            binding = (Binding) c.getDeclaredMethod("inflate", LayoutInflater.class).invoke(null, LayoutInflater.from(parent.getContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BindingViewHolder<>(binding);
    }

}
