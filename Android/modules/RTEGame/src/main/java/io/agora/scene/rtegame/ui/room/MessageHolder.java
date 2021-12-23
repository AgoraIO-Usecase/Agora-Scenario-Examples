package io.agora.scene.rtegame.ui.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.scene.rtegame.databinding.GameItemRoomMessageBinding;

public class MessageHolder extends BaseRecyclerViewAdapter.BaseViewHolder<GameItemRoomMessageBinding, CharSequence> {
    public MessageHolder(@NonNull GameItemRoomMessageBinding mBinding) {
        super(mBinding);

    }

    @Override
    public void binding(@Nullable CharSequence data, int selectedIndex) {
        // TODO using AppCompatTextView#setTextFuture to reduce layout time
        mBinding.getRoot().setText(data);
    }
}
