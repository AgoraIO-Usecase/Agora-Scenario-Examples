package io.agora.baselibrary.base;

import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

/**
 * 基础
 *
 * @author chenhengfei@agora.io
 */
public abstract class DataBindBaseActivity<V extends ViewDataBinding> extends BaseActivity {

    protected V mDataBinding;

    @Override
    protected void setCusContentView() {
        View view = LayoutInflater.from(this).inflate(getLayoutId(), null);
        mDataBinding = DataBindingUtil.bind(view);
        setContentView(view);
    }
}