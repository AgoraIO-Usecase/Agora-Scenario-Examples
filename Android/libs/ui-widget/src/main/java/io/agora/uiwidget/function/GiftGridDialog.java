package io.agora.uiwidget.function;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.agora.uiwidget.R;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.GiftGridDialogItemBinding;
import io.agora.uiwidget.databinding.GiftGridDialogLayoutBinding;
import io.agora.uiwidget.utils.RandomUtil;
import io.agora.uiwidget.utils.StatusBarUtil;

public class GiftGridDialog extends BottomSheetDialog {

    public static final GiftItem GIFT_ITEM_01 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_bell, R.drawable.gift_01_bell, R.drawable.gift_anim_bell, 20);
    public static final GiftItem GIFT_ITEM_02 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_ice_cream, R.drawable.gift_02_icecream, R.drawable.gift_anim_icecream, 30);
    public static final GiftItem GIFT_ITEM_03 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_wine, R.drawable.gift_03_wine, R.drawable.gift_anim_wine, 40);
    public static final GiftItem GIFT_ITEM_04 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_cake, R.drawable.gift_04_cake, R.drawable.gift_anim_cake, 50);
    public static final GiftItem GIFT_ITEM_05 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_ring, R.drawable.gift_05_ring, R.drawable.gift_anim_ring, 60);
    public static final GiftItem GIFT_ITEM_06 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_watch, R.drawable.gift_06_watch, R.drawable.gift_anim_watch, 70);
    public static final GiftItem GIFT_ITEM_07 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_diamond, R.drawable.gift_07_diamond, R.drawable.gift_anim_diamond, 80);
    public static final GiftItem GIFT_ITEM_08 = new GiftItem(RandomUtil.randomId(), R.string.gift_grid_default_gift_item_rocket, R.drawable.gift_08_rocket, R.drawable.gift_anim_rocket, 90);

    public static final List<GiftItem> DEFAULT_GIFT_LIST = Arrays.asList(
            GIFT_ITEM_01,
            GIFT_ITEM_02,
            GIFT_ITEM_03,
            GIFT_ITEM_04,
            GIFT_ITEM_05,
            GIFT_ITEM_06,
            GIFT_ITEM_07,
            GIFT_ITEM_08
    );

    private GiftGridDialogLayoutBinding mBinding;
    private final List<GiftItem> giftItemList = new ArrayList<>();
    private int selectedItemPosition = 0;
    private GiftItemAdapter mAdapter;
    private OnGiftItemSelectListener selectListener;
    private OnGiftSendClickListener sendClickListener;

    public GiftGridDialog(@NonNull Context context) {
        this(context, R.style.BottomSheetDialog);
    }

    public GiftGridDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    private void init() {
        giftItemList.addAll(DEFAULT_GIFT_LIST);
        setCanceledOnTouchOutside(true);
        mBinding = GiftGridDialogLayoutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);
        mBinding.giftGridRecycler.setLayoutManager(new GridLayoutManager(getContext(), 4));
        mAdapter = new GiftItemAdapter();
        mBinding.giftGridRecycler.setAdapter(mAdapter);
        mBinding.giftGridSendBtn.setOnClickListener(v -> {
            if (sendClickListener != null) {
                if (selectedItemPosition < giftItemList.size()) {
                    GiftItem giftItem = giftItemList.get(selectedItemPosition);
                    sendClickListener.onGiftSendClicked(GiftGridDialog.this, giftItem, selectedItemPosition);
                }
            }
        });
    }

    public GiftGridDialog resetGiftList(List<GiftItem> list) {
        selectedItemPosition = 0;
        giftItemList.clear();
        giftItemList.addAll(list);
        mAdapter.notifyDataSetChanged();
        return this;
    }

    public GiftGridDialog setOnGiftItemSelectListener(OnGiftItemSelectListener selectListener) {
        this.selectListener = selectListener;
        return this;
    }

    public GiftGridDialog setOnGiftSendClickListener(OnGiftSendClickListener sendClickListener) {
        this.sendClickListener = sendClickListener;
        return this;
    }

    private class GiftItemAdapter extends RecyclerView.Adapter<BindingViewHolder<GiftGridDialogItemBinding>> {

        @NonNull
        @Override
        public BindingViewHolder<GiftGridDialogItemBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BindingViewHolder<>(GiftGridDialogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BindingViewHolder<GiftGridDialogItemBinding> holder, int position) {
            GiftItem info = giftItemList.get(position);
            holder.binding.liveRoomActionSheetGiftItemName.setText(info.name_res);
            holder.binding.liveRoomActionSheetGiftItemValue.setText(holder.binding.getRoot().getContext().getResources().getString(R.string.gift_grid_coin_value_format, info.coin_point));
            holder.binding.getRoot().setActivated(selectedItemPosition == position);
            holder.binding.liveRoomActionSheetGiftItemIcon.setImageResource(info.icon_res);
            holder.binding.getRoot().setOnClickListener(v -> {
                selectedItemPosition = position;
                notifyDataSetChanged();
                if (selectListener != null) {
                    selectListener.onGiftItemSelected(GiftGridDialog.this, info, position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return giftItemList.size();
        }
    }

    public static class GiftItem {
        public final int gift_id;
        public final int name_res;
        public final int icon_res;
        public final int anim_res;
        public final int coin_point;

        public GiftItem(int gift_id, int name_res, int icon_res, int anim_res, int coin_point) {
            this.gift_id = gift_id;
            this.name_res = name_res;
            this.icon_res = icon_res;
            this.anim_res = anim_res;
            this.coin_point = coin_point;
        }
    }

    public interface OnGiftItemSelectListener {
        void onGiftItemSelected(GiftGridDialog dialog, GiftItem item, int position);
    }

    public interface OnGiftSendClickListener {
        void onGiftSendClicked(GiftGridDialog dialog, GiftItem item, int position);
    }

}
