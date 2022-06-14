package io.agora.scene.voice.widgets;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Arrays;
import java.util.List;

import io.agora.scene.voice.R;
import io.agora.scene.voice.databinding.VoiceBackgourndMusicDialogBinding;
import io.agora.scene.voice.databinding.VoiceBackgroundMusicItemBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.utils.StatusBarUtil;

public class BgMusicDialog extends BottomSheetDialog {

    private VoiceBackgourndMusicDialogBinding mBinding;
    private Listener mListener;
    private BindingSingleAdapter<MusicInfo, VoiceBackgroundMusicItemBinding> mMusicAdapter;

    public static void showDialog(Context context, Listener listener){
        new BgMusicDialog(context)
                .addMusics(Arrays.asList(
                        new MusicInfo("1", "happyrock", "Bensound", "https://agora-adc-artifacts.oss-cn-beijing.aliyuncs.com/albgm/bensound-happyrock.mp3"),
                        new MusicInfo("2", "jazzyfrenchy", "Bensound", "https://agora-adc-artifacts.oss-cn-beijing.aliyuncs.com/albgm/bensound-jazzyfrenchy.mp3"),
                        new MusicInfo("3", "ukulele", "Bensound", "https://agora-adc-artifacts.oss-cn-beijing.aliyuncs.com/albgm/bensound-ukulele.mp3")
                ))
                .setListener(listener)
                .show();
    }


    private BgMusicDialog(@NonNull Context context) {
        super(context, io.agora.uiwidget.R.style.BottomSheetDialog);
        init();
    }

    private void init(){
        setCanceledOnTouchOutside(true);
        mBinding = VoiceBackgourndMusicDialogBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mBinding.sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 音量改变
                if(mListener != null){
                    mListener.onVolumeChanged(seekBar.getMax(), progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mMusicAdapter = new BindingSingleAdapter<MusicInfo, VoiceBackgroundMusicItemBinding>() {
            private int _selectedPosition = -1;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceBackgroundMusicItemBinding> holder, int position) {
                final int _position = position;
                MusicInfo item = getItem(_position);
                holder.binding.tvTitle.setText(item.musicName);
                holder.binding.tvArtist.setText(item.singer);
                holder.binding.ivMusic.setActivated(_selectedPosition == _position);
                holder.binding.getRoot().setOnClickListener(v -> {
                    if (_selectedPosition != _position) {
                        int oPosition = _selectedPosition;
                        _selectedPosition = _position;
                        notifyItemChanged(oPosition);
                        notifyItemChanged(_position);
                        if (mListener != null) {
                            mListener.onMusicSelected(item, true);
                        }
                    }else{
                        int oPosition = _selectedPosition;
                        _selectedPosition = -1;
                        notifyItemChanged(oPosition);
                        notifyItemChanged(_position);
                        if (mListener != null) {
                            mListener.onMusicSelected(item, false);
                        }
                    }
                });
            }
        };
        mBinding.rvMusic.setAdapter(mMusicAdapter);

        String hint = getContext().getResources().getString(R.string.voice_background_music_credit_hint);
        String link = getContext().getResources().getString(R.string.voice_background_music_credit_link);
        String credit = hint + link;
        SpannableString creditSpan = new SpannableString(credit);
        creditSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#FFababab")),
                0, hint.length(), SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);
        creditSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#0088EB")),
                hint.length(), credit.length(), SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);
        mBinding.tvCredit.setText(creditSpan);
    }

    public BgMusicDialog addMusics(List<MusicInfo> list){
        mMusicAdapter.insertAll(list);
        return this;
    }

    public BgMusicDialog setListener(Listener listener) {
        this.mListener = listener;
        return this;
    }

    public interface Listener{
        void onVolumeChanged(int max, int volume);
        void onMusicSelected(MusicInfo musicInfo, boolean isSelected);
    }

    public static class MusicInfo {
        public String musicId;
        public String musicName;
        public String singer;
        public String url;
        public MusicInfo(String musicId, String musicName, String singer, String url) {
            this.musicId = musicId;
            this.musicName = musicName;
            this.singer = singer;
            this.url = url;
        }
    }
}
