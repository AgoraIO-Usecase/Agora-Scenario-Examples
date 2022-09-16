package io.agora.scene.shopping.widget;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import io.agora.scene.shopping.R;
import io.agora.scene.shopping.RoomManager;
import io.agora.scene.shopping.databinding.ShoppingProductItemBinding;
import io.agora.scene.shopping.databinding.ShoppingProductListDialogBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;

public class ProductListDialog extends BottomSheetDialog {

    private final RoomManager roomManager = RoomManager.getInstance();
    private final String roomId;
    private final boolean isAudience;
    private ShoppingProductListDialogBinding mBinding;
    private BindingSingleAdapter<RoomManager.ShoppingModel, ShoppingProductItemBinding> listAdapter;
    private OnDetailVideoListener onDetailVideoListener;
    private final Runnable shoppingUpdateRun = new Runnable() {
        @Override
        public void run() {
            mBinding.getRoot().post(() -> updateListData());
        }
    };

    public ProductListDialog(String roomId, boolean isAudience, @NonNull Context context) {
        super(context, R.style.BottomSheetDialog);
        this.roomId = roomId;
        this.isAudience = isAudience;
        init();
    }

    private void init() {
        mBinding = ShoppingProductListDialogBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());
        setCanceledOnTouchOutside(true);

        listAdapter = new BindingSingleAdapter<RoomManager.ShoppingModel, ShoppingProductItemBinding>() {
            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<ShoppingProductItemBinding> holder, int position) {
                RoomManager.ShoppingModel item = getItem(position);
                holder.binding.ivIcon.setImageResource(RoomManager.ShoppingImgResMap.get(item.imageName));
                holder.binding.tvDescription.setText(item.desc);
                holder.binding.tvPrice.setText(holder.binding.getRoot().getContext().getString(
                        R.string.shopping__product_price_format, (int) item.price
                ));
                if (!isAudience) {
                    holder.binding.tvAction.setText(
                            isCurrentUnListed() ? R.string.shopping__product_action_unlisted : R.string.shopping__product_action_list
                    );
                    holder.binding.tvAction.setOnClickListener(v -> {
                        if (isCurrentUnListed()) {
                            roomManager.downShoppingModel(roomId, item);
                        } else {
                            roomManager.upShoppingModel(roomId, item);
                        }
                    });
                } else {
                    holder.binding.tvAction.setText(R.string.shopping__product_action_detail);
                    holder.binding.tvAction.setOnClickListener(v -> {
                        ProductDetailDialog productDetailDialog = new ProductDetailDialog(getContext(), item);
                        productDetailDialog.setOnShowListener(dialog -> {
                            if(onDetailVideoListener != null){
                                onDetailVideoListener.onDetailVideoChanged(productDetailDialog.getVideoPlaceholder(), true);
                            }
                        });
                        productDetailDialog.setOnDismissListener(dialog -> {
                            if(onDetailVideoListener != null){
                                onDetailVideoListener.onDetailVideoChanged(productDetailDialog.getVideoPlaceholder(), false);
                            }
                        });
                        productDetailDialog.setOnVideoClockListener(v1 -> {
                            if(onDetailVideoListener != null){
                                onDetailVideoListener.onDetailVideoChanged(productDetailDialog.getVideoPlaceholder(), false);
                            }
                        });
                        productDetailDialog.show();
                    });
                }
            }
        };
        mBinding.rvList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int space = parent.getContext().getResources().getDimensionPixelOffset(R.dimen.shopping_product_item_space);
                outRect.top = outRect.bottom = space;
            }
        });
        mBinding.rvList.setAdapter(listAdapter);

        mBinding.tlType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateListData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        if (isAudience) {
            mBinding.tlType.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        roomManager.subscriptShoppingModelEvent(roomId, shoppingUpdateRun);
        updateListData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        roomManager.unSubscriptShoppingModelEvent(roomId, shoppingUpdateRun);
    }

    private void updateListData() {
        listAdapter.removeAll();

        if (isAudience || isCurrentUnListed()) {
            roomManager.getUpShoppingModels(roomId, dataList -> {
                mBinding.getRoot().post(() -> listAdapter.insertAll(dataList));
            });
        } else {
            roomManager.getNormalShoppingModels(roomId, dataList -> {
                mBinding.getRoot().post(() -> listAdapter.insertAll(dataList));
            });
        }
    }

    private boolean isCurrentUnListed() {
        return mBinding.tlType.getSelectedTabPosition() != 0;
    }

    public void setOnDetailVideoListener(OnDetailVideoListener onDetailVideoListener) {
        this.onDetailVideoListener = onDetailVideoListener;
    }

    public interface OnDetailVideoListener {
        void onDetailVideoChanged(FrameLayout contain, boolean show);
    }
}
