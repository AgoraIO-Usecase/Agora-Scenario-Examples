package io.agora.sample.breakoutroom.ui.room;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseUtil;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.SubRoomInfo;
import io.agora.sample.breakoutroom.databinding.FragmentRoomBinding;
import io.agora.sample.breakoutroom.ui.MainViewModel;
import io.agora.sample.breakoutroom.view.ScrollableLinearLayout;


/**
 * @author lq
 *
 * Fragment 监听 数据 展示 UI
 * RoomViewModel 获取并发送数据
 * MainViewModel 用于通用逻辑管理（退出频道等)
 */
public class RoomFragment extends BaseFragment<FragmentRoomBinding> {

    private MainViewModel mainViewModel;
    private RoomViewModel mViewModel;
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mViewModel = new ViewModelProvider(getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(RoomViewModel.class);
        mViewModel.currentRoomInfo = mainViewModel.currentRoom;

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), desiredBottom);
            return insets;
        });

        initView();
        initListener();

        mViewModel.fetchAllSubRooms();
    }

    private void initView() {
        RoomUtil.configInputDialog(mBinding.viewInputFgRoom.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgRoom.inputLayoutViewInput);
        mBinding.viewInputFgRoom.titleViewInput.setText(R.string.fab_add_sub_room);

        // Add default Tab
        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        mBinding.tabLayoutFgRoom.addTab(tab);
        tab.setText(mViewModel.currentRoomInfo.getId());

        // Adjust view pos
        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mBinding.appBarFgRoom.getViewTreeObserver().isAlive())
                    mBinding.appBarFgRoom.getViewTreeObserver().removeOnPreDrawListener(this);

                int appBarHeight = mBinding.appBarFgRoom.getMeasuredHeight();
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

    private void initListener() {
        // Inner Dialog
        mBinding.viewInputFgRoom.btnConfirmViewLayout.setOnClickListener(this::pendingCreateSubRoom);
        mBinding.viewInputFgRoom.getRoot().setOnClickListener(this::clearFocus);

        // Show Dialog
        mBinding.fabFgRoom.setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(true));
        mBinding.scrimFgRoom.setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(false));

        // View Status
        mViewModel.viewStatus().observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Loading)
                showLoading();
            else if (viewStatus instanceof ViewStatus.Done)
                dismissLoading();
            else if (viewStatus instanceof ViewStatus.Error) {
                dismissLoading();
                BaseUtil.toast(requireContext(), ((ViewStatus.Error) viewStatus).msg);
            }
        });

        // Observe SubRoom
        mViewModel.subRoomList().observe(getViewLifecycleOwner(), list -> {
            for (SubRoomInfo subRoomInfo : list) {
                addTab(subRoomInfo);
            }
        });

        // 创建子房间成功 或者清除成功
        mViewModel.pendingSubRoom().observe(getViewLifecycleOwner(), subRoomInfo -> {
            if (subRoomInfo == null) return;
            if (mBinding.scrimFgRoom.getVisibility() == View.VISIBLE)
                mBinding.scrimFgRoom.performClick();
            mBinding.getRoot().postDelayed(() -> BaseUtil.hideKeyboard(requireActivity().getWindow(), mBinding.viewInputFgRoom.editTextViewInput),500);
            addTab(subRoomInfo);
            mViewModel.clearPendingSubRoom();
        });
    }

    /**
     * 准备创建房间
     */
    private void pendingCreateSubRoom(View v) {
        TextInputLayout inputLayout = mBinding.viewInputFgRoom.inputLayoutViewInput;
        Editable text = mBinding.viewInputFgRoom.editTextViewInput.getText();
        String inputText = null;
        if (text != null) inputText = text.toString().trim();

        if (inputText != null && !inputText.isEmpty()) {
            int err = isValidRoomName(inputText);
            if (err == 0)
                mViewModel.createSubRoom(inputText);
            else showNameIllegalError(err);
        } else {
            if (inputLayout.isErrorEnabled())
                BaseUtil.shakeViewAndVibrateToAlert(inputLayout);
            else
                inputLayout.setError(getString(R.string.room_name_invalid));
        }
    }

    private void addTab(@NonNull SubRoomInfo subRoomInfo) {
        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        mBinding.tabLayoutFgRoom.addTab(tab);
        tab.setText(subRoomInfo.getSubRoom());
    }

    private void addView() {
        mBinding.dynamicViewFgRoom.dynamicAddView(ScrollableLinearLayout.getChildAudioCardView(requireContext(), null, "User_" + new Random(System.currentTimeMillis()).nextInt(9999)));
    }

    @StringRes
    private int isValidRoomName(@NonNull String name){
        int tabCount = mBinding.tabLayoutFgRoom.getTabCount();
        if (tabCount > 2) return R.string.room_count_limited;

        // Skip the first one
        for (int i = 1; i < tabCount; i++) {
            TabLayout.Tab tempTab = mBinding.tabLayoutFgRoom.getTabAt(i);
            if (tempTab == null) continue;

            CharSequence tabText = tempTab.getText();
            if (tabText == null) continue;

            if (tabText.toString().trim().equals(name))
                return R.string.room_name_duplicated;
        }
        return 0;
    }
    private void clearFocus(View v) {
        mBinding.viewInputFgRoom.inputLayoutViewInput.clearFocus();
        BaseUtil.hideKeyboard(requireActivity().getWindow(), mBinding.viewInputFgRoom.inputLayoutViewInput);
    }

    private void fixPosition() {
        mBinding.appBarFgRoom.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
    }

    private void showNameIllegalError(@StringRes int illegalReason){
        TextInputLayout input = mBinding.viewInputFgRoom.inputLayoutViewInput;
        if(input.isErrorEnabled())
            BaseUtil.shakeViewAndVibrateToAlert(input);
        else
            input.setError(getString(illegalReason));
    }

    @Override
    public void onDestroyView() {
        mBinding.appBarFgRoom.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        super.onDestroyView();
    }

}
