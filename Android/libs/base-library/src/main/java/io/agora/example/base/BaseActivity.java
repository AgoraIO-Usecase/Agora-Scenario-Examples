package io.agora.example.base;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.Type;

public abstract class BaseActivity<B extends ViewBinding> extends AppCompatActivity {
    public B mBinding;
    private AlertDialog mLoadingDialog = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = getViewBindingByReflect(getLayoutInflater());
        if (mBinding == null) {
            BaseUtil.toast(this, "Inflate Error");
            finish();
        } else
            super.setContentView(mBinding.getRoot());

//        WindowCompat.setDecorFitsSystemWindows(getWindow(), true)
    }

    public void showLoading() {
        showLoading(true);
    }

    public void showLoading(boolean cancelable) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new AlertDialog.Builder(this).create();
            mLoadingDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true);
            progressBar.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            mLoadingDialog.setView(progressBar);
        }
        mLoadingDialog.setCancelable(cancelable);
        mLoadingDialog.show();
    }

    public void dismissLoading() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismiss();
    }

    @SuppressWarnings("unchecked")
    private B getViewBindingByReflect(@NonNull LayoutInflater inflater) {
        try {
            Type type = getClass().getGenericSuperclass();
            if (type == null) return null;
            Class<B> c = BaseUtil.getGenericClass(getClass(), 0);
            if (c != null)
                return (B) BaseUtil.getViewBinding(c, inflater);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}