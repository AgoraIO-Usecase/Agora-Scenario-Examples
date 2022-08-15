package io.agora.scene.voice.widgets;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import io.agora.scene.voice.R;
import io.agora.scene.voice.databinding.VoiceUserOptionsDialogBinding;
import io.agora.scene.voice.databinding.VoiceUserOptionsItemBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.utils.StatusBarUtil;

public class UserOptionsDialog extends BottomSheetDialog {
    public static final Option OPTION_INVITE = new Option(R.string.voice_user_options_invite, Color.parseColor("#ff0099cc"));
    public static final Option OPTION_MUTE = new Option(R.string.voice_user_options_mute, Color.parseColor("#ffff4444"));


    private io.agora.scene.voice.databinding.VoiceUserOptionsDialogBinding mBinding;
    private BindingSingleAdapter<Option, VoiceUserOptionsItemBinding> mAdapter;
    private OnItemSelectedListener onItemSelectedListener;

    public UserOptionsDialog(@NonNull Context context) {
        super(context, io.agora.uiwidget.R.style.BottomSheetDialog);
        init();
    }

    private void init() {
        setCanceledOnTouchOutside(true);
        mBinding = VoiceUserOptionsDialogBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mAdapter = new BindingSingleAdapter<Option, VoiceUserOptionsItemBinding>() {
            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceUserOptionsItemBinding> holder, int position) {
                Option item = getItem(position);

                ViewGroup.LayoutParams layoutParams = holder.binding.getRoot().getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                } else {
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                holder.binding.getRoot().setLayoutParams(layoutParams);

                holder.binding.text.setText(item.textRes);
                holder.binding.text.setTextColor(item.textColor);
                final int _position =  position;
                holder.binding.getRoot().setOnClickListener(v -> {
                    if(onItemSelectedListener != null){
                        onItemSelectedListener.onItemSelected(UserOptionsDialog.this, _position, item);
                    }
                });
            }
        };
        mBinding.rvContent.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        mBinding.rvContent.setAdapter(mAdapter);

        mBinding.btnCancel.setOnClickListener(v -> dismiss());
    }

    public UserOptionsDialog addOptions(Option... options){
        mAdapter.insertAll(options);
        return this;
    }

    public UserOptionsDialog setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
        return this;
    }

    public interface OnItemSelectedListener{
        void onItemSelected(UserOptionsDialog dialog, int position, Option option);
    }

    public static final class Option{
        public int textRes;
        public int textColor = Color.BLUE;

        public Option(int textRes, int textColor) {
            this.textRes = textRes;
            this.textColor = textColor;
        }
    }
}
