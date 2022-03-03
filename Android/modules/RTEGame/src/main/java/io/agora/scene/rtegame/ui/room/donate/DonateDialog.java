package io.agora.scene.rtegame.ui.room.donate;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.DividerDecoration;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.rtegame.GlobalViewModel;
import io.agora.scene.rtegame.R;
import io.agora.scene.rtegame.bean.Gift;
import io.agora.scene.rtegame.bean.GiftInfo;
import io.agora.scene.rtegame.databinding.GameDialogDonateBinding;
import io.agora.scene.rtegame.databinding.GameItemDialogGiftBinding;
import io.agora.scene.rtegame.ui.room.RoomViewModel;
import io.agora.scene.rtegame.util.GameUtil;
import io.agora.scene.rtegame.util.GiftUtil;

public class DonateDialog extends BaseBottomSheetDialogFragment<GameDialogDonateBinding> implements OnItemClickListener<Gift> {
    public static final String TAG = "DonateDialog";

    private RoomViewModel roomViewModel;

    private BaseRecyclerViewAdapter<GameItemDialogGiftBinding, Gift, GiftHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Edge to edge
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        roomViewModel = GameUtil.getViewModel(RoomViewModel.class, requireParentFragment());
        initView();
        fetchAllGift();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, GiftHolder.class);
        mBinding.recyclerViewDialogDonate.setAdapter(mAdapter);
        mBinding.recyclerViewDialogDonate.addItemDecoration(new DividerDecoration(4, 12,0));
        mBinding.btnDonateDialogDonate.setOnClickListener(this::doDonate);
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
    }

    private void doDonate(View view) {
        if (mAdapter.selectedIndex < 0 || mAdapter.selectedIndex >= mAdapter.getItemCount()) {
            BaseUtil.shakeViewAndVibrateToAlert(mBinding.recyclerViewDialogDonate);
        }else {
            Gift gift = mAdapter.getItemData(mAdapter.selectedIndex);
            if (gift != null) {
                GiftInfo giftInfo = new GiftInfo(GiftUtil.getGiftResNameById(requireContext(), gift.getId()), gift.getValue(), gift.getName(), GlobalViewModel.localUser.getUserId());
                roomViewModel.donateGift(giftInfo);
            }
            dismiss();
        }
    }

    public void fetchAllGift() {
        List<Gift> gifts = new ArrayList<>();
        String[] giftNames = getResources().getStringArray(R.array.game_gift_name_list);
        for (int i = 0; i < 8; i++) {
            Gift gift = new Gift(i, 10 * (i + 1), giftNames[i]);
            gift.iconRes = GiftUtil.getIconByGiftId(i);
            gift.gifRes = GiftUtil.getGifByGiftId(i);
            gifts.add(gift);
        }
        mAdapter.submitListAndPurge(gifts);
    }

    @Override
    public void onItemClick(@NonNull Gift data, @NonNull View view, int position, long viewType) {
        GiftHolder holder = (GiftHolder) mBinding.recyclerViewDialogDonate.findViewHolderForAdapterPosition(mAdapter.selectedIndex);
        if (holder != null) {
            ((MaterialButton)holder.itemView).setChecked(false);
        }
        if (view instanceof MaterialButton) {
            if (((MaterialButton) view).isChecked())
                mAdapter.selectedIndex = position;
            else {
                mAdapter.selectedIndex = -1;
            }
        }
    }
}
