package io.agora.uiwidget.function;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.uiwidget.databinding.RemoteSmallControlLayoutBinding;

public class RemoteSmallControlView extends FrameLayout {

    private RemoteSmallControlLayoutBinding mBinding;

    public RemoteSmallControlView(@NonNull Context context) {
        this(context, null);
    }

    public RemoteSmallControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemoteSmallControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mBinding = RemoteSmallControlLayoutBinding.inflate(LayoutInflater.from(getContext()), this, true);
    }

    public RemoteSmallControlView setOnCloseClickListener(OnClickListener onClickListener){
        mBinding.ivClose.setOnClickListener(onClickListener);
        return this;
    }

    public FrameLayout getVideoContainer(){
        return mBinding.videoContainer;
    }

    public RemoteSmallControlView setName(String name){
        mBinding.tvName.setText(name);
        return this;
    }


}
