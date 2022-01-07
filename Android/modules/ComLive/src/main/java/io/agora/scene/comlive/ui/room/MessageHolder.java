package io.agora.scene.comlive.ui.room;

import android.content.res.ColorStateList;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.databinding.ComLiveItemRoomMessageBinding;

public class MessageHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ComLiveItemRoomMessageBinding, MessageHolder.LiveMessage> {
    public MessageHolder(@NonNull ComLiveItemRoomMessageBinding mBinding) {
        super(mBinding);

    }

    @Override
    public void binding(@Nullable LiveMessage message, int selectedIndex) {
        if (message == null) return;
        // TODO using AppCompatTextView#setTextFuture to reduce layout time
        mBinding.getRoot().setText(message.msg);
        int bgdTintColor;
        if (message.type == 0)
            bgdTintColor = ContextCompat.getColor(mBinding.getRoot().getContext(), R.color.com_live_colorHalfTransparent);
        else
            bgdTintColor = ContextCompat.getColor(mBinding.getRoot().getContext(), R.color.com_live_alert_message_color);
        mBinding.getRoot().setBackgroundTintList(ColorStateList.valueOf(bgdTintColor));
    }
    public static class LiveMessage{
        private final int type;
        private final CharSequence msg;

        public LiveMessage(@IntRange(from = 0, to = 1) int type, @NonNull CharSequence msg) {
            this.type = type;
            this.msg = msg;
        }

        public int getType() {
            return type;
        }

        @NonNull
        public CharSequence getMsg() {
            return msg;
        }
    }
}