package io.agora.uiwidget.function;

import android.content.Context;
import android.graphics.Color;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.agora.uiwidget.R;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.VideoSettingDialogLayoutBinding;
import io.agora.uiwidget.databinding.VideoSettingDialogMainLayoutBinding;
import io.agora.uiwidget.databinding.VideoSettingListItemTextOnlyBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class VideoSettingDialog extends BottomSheetDialog {

    private VideoSettingDialogLayoutBinding mBinding;
    private VideoSettingDialogMainLayoutBinding mMainBinding;

    private final List<Size> mResolutions = new ArrayList<>();
    private final List<Integer> mFrameRates = new ArrayList<>();
    private int minBitrate = 0;
    private int maxBitrate = 1000;

    private OnValuesChangeListener onValuesChangeListener;

    public VideoSettingDialog(@NonNull Context context) {
        this(context, R.style.BottomSheetDialog);
    }

    public VideoSettingDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    private void init(){
        setCanceledOnTouchOutside(true);
        mBinding = VideoSettingDialogLayoutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mMainBinding = VideoSettingDialogMainLayoutBinding.inflate(LayoutInflater.from(getContext()), mBinding.videoSettingContainer, true);

        // 分辨率
        mMainBinding.videoSettingResolution.setOnClickListener(v -> {
            View view = showResolutionLayout();
            mBinding.videoSettingBack.setVisibility(View.VISIBLE);
            mBinding.videoSettingBack.setOnClickListener(v1 -> {
                mBinding.videoSettingBack.setVisibility(View.GONE);
                mBinding.videoSettingContainer.removeView(view);
            });
        });
        // 帧率
        mMainBinding.videoSettingFramerate.setOnClickListener(v -> {
            View view = showFrameRateLayout();
            mBinding.videoSettingBack.setVisibility(View.VISIBLE);
            mBinding.videoSettingBack.setOnClickListener(v1 -> {
                mBinding.videoSettingBack.setVisibility(View.GONE);
                mBinding.videoSettingContainer.removeView(view);
            });
        });
        // 码率
        mMainBinding.liveRoomSettingBitrateProgressBar.setMax(maxBitrate - minBitrate);
        mMainBinding.liveRoomSettingBitrateProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bitrate = minBitrate + progress;
                mMainBinding.videoSettingBitrateValueText.setText(getContext().getString(R.string.video_setting_dialog_bitrate_value_format, bitrate));

                if(onValuesChangeListener != null){
                    onValuesChangeListener.onBitrateChanged(bitrate);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private View showFrameRateLayout() {
        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<BindingViewHolder<VideoSettingListItemTextOnlyBinding>>() {
            @NonNull
            @Override
            public BindingViewHolder<VideoSettingListItemTextOnlyBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new BindingViewHolder<>(VideoSettingListItemTextOnlyBinding.inflate(LayoutInflater.from(parent.getContext())));
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VideoSettingListItemTextOnlyBinding> holder, int position) {
                int frameRate = mFrameRates.get(position);
                String text = String.format(Locale.US, "%d", frameRate);
                holder.binding.videoSettingItemText.setText(text);
                holder.binding.videoSettingItemText.setOnClickListener(v -> {
                    mBinding.videoSettingBack.setVisibility(View.GONE);
                    mBinding.videoSettingContainer.removeView(recyclerView);
                    mMainBinding.videoSettingMainFramerateText.setText(text);
                    if(onValuesChangeListener != null){
                        onValuesChangeListener.onFrameRateChanged(frameRate);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return mFrameRates.size();
            }
        });
        recyclerView.setBackgroundColor(Color.WHITE);
        recyclerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mBinding.videoSettingContainer.addView(recyclerView);
        return recyclerView;
    }

    private View showResolutionLayout() {
        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<BindingViewHolder<VideoSettingListItemTextOnlyBinding>>() {
            @NonNull
            @Override
            public BindingViewHolder<VideoSettingListItemTextOnlyBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new BindingViewHolder<>(VideoSettingListItemTextOnlyBinding.inflate(LayoutInflater.from(parent.getContext())));
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VideoSettingListItemTextOnlyBinding> holder, int position) {
                Size size = mResolutions.get(position);
                String text = String.format(Locale.US, "%dX%d", size.getWidth(), size.getHeight());
                holder.binding.videoSettingItemText.setText(text);
                holder.binding.videoSettingItemText.setOnClickListener(v -> {
                    mBinding.videoSettingBack.setVisibility(View.GONE);
                    mBinding.videoSettingContainer.removeView(recyclerView);
                    mMainBinding.videoSettingMainResolutionText.setText(text);
                    if(onValuesChangeListener != null){
                        onValuesChangeListener.onResolutionChanged(size);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return mResolutions.size();
            }
        });
        recyclerView.setBackgroundColor(Color.WHITE);
        recyclerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mBinding.videoSettingContainer.addView(recyclerView);
        return recyclerView;
    }


    public VideoSettingDialog setResolutions(List<Size> resolutions){
        mResolutions.clear();
        mResolutions.addAll(resolutions);
        return this;
    }

    public VideoSettingDialog setFrameRates(List<Integer> frameRates){
        mFrameRates.clear();
        mFrameRates.addAll(frameRates);
        return this;
    }

    public VideoSettingDialog setBitRateRange(int min, int max){
        minBitrate = min;
        maxBitrate = max;
        mMainBinding.liveRoomSettingBitrateProgressBar.setMax(max - min);
        return this;
    }

    public VideoSettingDialog setDefaultValues(Size resolution, int framerate, int bitrate){
        mMainBinding.videoSettingMainResolutionText.setText(String.format(Locale.US, "%dX%d", resolution.getWidth(), resolution.getHeight()));
        mMainBinding.videoSettingMainFramerateText.setText(String.valueOf(framerate));
        mMainBinding.videoSettingBitrateValueText.setText(getContext().getString(R.string.video_setting_dialog_bitrate_value_format, bitrate));
        mMainBinding.liveRoomSettingBitrateProgressBar.setProgress(bitrate - minBitrate);
        return this;
    }

    public VideoSettingDialog setOnValuesChangeListener(OnValuesChangeListener listener){
        this.onValuesChangeListener = listener;
        return this;
    }

    public interface OnValuesChangeListener {
        void onResolutionChanged(Size resolution);
        void onFrameRateChanged(int framerate);
        void onBitrateChanged(int bitrate);
    }
}
