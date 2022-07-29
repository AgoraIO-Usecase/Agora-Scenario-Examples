package io.agora.example.base;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class BaseBottomSheetDialogFragment<B extends ViewBinding> extends BottomSheetDialogFragment {
    public B mBinding;

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        onBeforeCreateView();
        mBinding = getViewBindingByReflect(inflater, container);
        return mBinding != null ? mBinding.getRoot() : null;
    }

    protected void onBeforeCreateView(){

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);
        requireDialog().setOnShowListener(dialog -> ((ViewGroup) view.getParent()).setBackgroundColor(Color.TRANSPARENT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private B getViewBindingByReflect(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        try {
            Class<B> c = BaseUtil.getGenericClass(getClass(), 0);
            return BaseUtil.getViewBinding(c, inflater, container);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}