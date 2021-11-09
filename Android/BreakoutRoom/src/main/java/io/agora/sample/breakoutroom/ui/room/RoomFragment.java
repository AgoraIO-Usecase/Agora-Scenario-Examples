package io.agora.sample.breakoutroom.ui.room;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseUtil;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.databinding.FragmentRoomBinding;
import io.agora.sample.breakoutroom.view.ScrollableLinearLayout;


public class RoomFragment extends BaseFragment<FragmentRoomBinding> {

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,inset.bottom);
            v.setPadding(v.getPaddingLeft(),v.getPaddingTop(),v.getPaddingRight(),desiredBottom);
            return insets;
        });

        initView();
        initListener();
    }

    private void initView() {
        RoomUtil.configInputDialog(mBinding.viewInputFgRoom.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgRoom.inputLayoutViewInput);
        mBinding.viewInputFgRoom.titleViewInput.setText(R.string.fab_add_sub_room);

        // Add default Tab
        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        mBinding.tabLayoutFgRoom.addTab(tab);
        tab.setText(R.string.app_name);
    }

    private void initListener() {
        // Inner Dialog
        mBinding.viewInputFgRoom.btnConfirmViewLayout.setOnClickListener(this::addSubRoom);
        mBinding.viewInputFgRoom.getRoot().setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(false));

        // Show Dialog
        mBinding.fabFgRoom.setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(true));
        mBinding.scrimFgRoom.setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(false));

        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mBinding.appBarFgRoom.getViewTreeObserver().isAlive())
                    mBinding.appBarFgRoom.getViewTreeObserver().removeOnPreDrawListener(this);

                int appBarHeight = mBinding.appBarFgRoom.getMeasuredHeight();
                BaseUtil.logD("height ->" + appBarHeight);
                int fabHeight = mBinding.fabFgRoom.getMeasuredHeight();

                CoordinatorLayout.LayoutParams lpFab = (CoordinatorLayout.LayoutParams) mBinding.fabFgRoom.getLayoutParams();
                CoordinatorLayout.LayoutParams lpDynamic = (CoordinatorLayout.LayoutParams) mBinding.dynamicViewFgRoom.getLayoutParams();

                if (appBarHeight != lpDynamic.topMargin) {
                    lpFab.topMargin = (int) (appBarHeight - fabHeight + BaseUtil.dp2px(12));
                    mBinding.fabFgRoom.setLayoutParams(lpFab);

                    lpDynamic.topMargin = appBarHeight;
                    mBinding.dynamicViewFgRoom.setLayoutParams(lpDynamic);
                }
                return false;
            }
        };
        fixPosition();
    }

    private void addSubRoom(View v) {
        TextInputLayout inputLayout = mBinding.viewInputFgRoom.inputLayoutViewInput;
        if(RoomUtil.isInputValid(inputLayout)) {
            addView();
        }else{
            if(inputLayout.isErrorEnabled())
                BaseUtil.shakeViewAndVibrateToAlert(inputLayout);
            else
            inputLayout.setError(getString(R.string.room_name_invalid));
        }
    }

    private void addView() {
        mBinding.dynamicViewFgRoom.dynamicAddView(ScrollableLinearLayout.getChildAudioCardView(requireContext(), null, "User_" + new Random(System.currentTimeMillis()).nextInt(9999)));
    }

    private void fixPosition() {
        mBinding.appBarFgRoom.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
    }

    @Override
    public void onDestroyView() {
        mBinding.appBarFgRoom.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        super.onDestroyView();
    }
}
