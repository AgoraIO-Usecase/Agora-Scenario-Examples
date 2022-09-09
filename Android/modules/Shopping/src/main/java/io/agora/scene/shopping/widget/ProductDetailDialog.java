package io.agora.scene.shopping.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import io.agora.scene.shopping.R;
import io.agora.scene.shopping.RoomManager;
import io.agora.scene.shopping.databinding.ShoppingProductDetailLayoutBinding;

public class ProductDetailDialog  extends Dialog {
    private final RoomManager.ShoppingModel product;
    private ShoppingProductDetailLayoutBinding mBinding;


    public ProductDetailDialog(@NonNull Context context, RoomManager.ShoppingModel product) {
        super(context, R.style.shoppingDialogProductDetail);
        this.product = product;
        init();
    }

    private void init(){
        mBinding = ShoppingProductDetailLayoutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mBinding.getRoot());

        mBinding.productDetailBigPicture.setImageResource(RoomManager.ShoppingBigImgResMap.get(product.imageName));
        mBinding.productActualPrice.setText(String.valueOf((int)product.price));
        mBinding.productWindowDescriptionText.setText(product.desc);

        mBinding.productDetailBack.setOnClickListener(v -> dismiss());
        mBinding.productBuyNowBtn.setOnClickListener(v -> dismiss());
    }

    public FrameLayout getVideoPlaceholder(){
        return mBinding.ownerVideo;
    }

    public void setOnVideoClockListener(View.OnClickListener listener){
        mBinding.productDetailVideoCloseBtn.setOnClickListener(v -> {
            mBinding.productDetailOwnerVideoLayout.setVisibility(View.GONE);
            if(listener != null){
                listener.onClick(v);
            }
        });
    }


}
