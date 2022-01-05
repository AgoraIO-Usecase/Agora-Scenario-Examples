package io.agora.uiwidget.function;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.uiwidget.databinding.PreviewControlLayoutBinding;
import io.agora.uiwidget.utils.RandomUtil;

public class PreviewControlView extends FrameLayout {

    private PreviewControlLayoutBinding mBinding;

    public PreviewControlView(@NonNull Context context) {
        this(context, null);
    }

    public PreviewControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBinding = PreviewControlLayoutBinding.inflate(LayoutInflater.from(getContext()), this, true);

        mBinding.roomNameEdit.setText(RandomUtil.randomLiveRoomName(getContext()));
        mBinding.randomBtn.setOnClickListener(v -> mBinding.roomNameEdit.setText(RandomUtil.randomLiveRoomName(getContext())));

        mBinding.previewControlPolicyClose.setOnClickListener(v -> mBinding.previewControlPolicyCautionLayout.setVisibility(View.GONE));
    }

    public void setBackIcon(boolean visible, OnClickListener onClickListener){
        mBinding.previewControlClose.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBinding.previewControlClose.setOnClickListener(onClickListener);
    }

    public void setBeautyIcon(boolean visible, OnClickListener onClickListener){
        mBinding.previewControlBeautyBtn.setVisibility(visible ? View.VISIBLE: View.INVISIBLE);
        mBinding.previewControlBeautyBtn.setOnClickListener(onClickListener);
    }

    public void setSettingIcon(boolean visible, OnClickListener onClickListener){
        mBinding.previewControlSettingBtn.setVisibility(visible ? View.VISIBLE: View.INVISIBLE);
        mBinding.previewControlSettingBtn.setOnClickListener(onClickListener);
    }

    public void setCameraIcon(boolean visible, OnClickListener onClickListener){
        mBinding.previewControlSwitchCamera.setVisibility(visible ? View.VISIBLE: View.INVISIBLE);
        mBinding.previewControlSwitchCamera.setOnClickListener(onClickListener);
    }

    public void setGoLiveBtn(GoLiveListener liveBtn){
        mBinding.previewControlGoLiveBtn.setOnClickListener(v -> {
            if(liveBtn != null){
                liveBtn.onClick(v, mBinding.roomNameEdit.getText().toString());
            }
        });
    }

    public FrameLayout getVideoContainer(){
        return mBinding.videoContainer;
    }

    public interface GoLiveListener {
        void onClick(View view, String randomName);
    }

}
