package io.agora.example.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewbinding.ViewBinding;

public class BaseDialogFragment<B extends ViewBinding> extends DialogFragment {

    public B mBinding;

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        onBeforeCreateView();
        if (container != null)
            mBinding = getViewBindingByReflect(inflater, container);
        if (mBinding == null)
            return null;
        return mBinding.getRoot();
    }

    protected void onBeforeCreateView(){

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private B getViewBindingByReflect(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        try {
            Class<B> c = BaseUtil.getGenericClass(getClass(), 0);
            if (c != null)
                return (B) BaseUtil.getViewBinding(c, inflater, container);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
