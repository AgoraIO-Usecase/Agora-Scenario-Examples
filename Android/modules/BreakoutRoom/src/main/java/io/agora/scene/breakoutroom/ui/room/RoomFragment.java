package io.agora.scene.breakoutroom.ui.room;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import io.agora.example.base.BaseUtil;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomConstant;
import io.agora.scene.breakoutroom.RoomUtil;
import io.agora.scene.breakoutroom.ViewStatus;
import io.agora.scene.breakoutroom.base.BaseNavFragment;
import io.agora.scene.breakoutroom.bean.RoomInfo;
import io.agora.scene.breakoutroom.bean.SubRoomInfo;
import io.agora.scene.breakoutroom.databinding.RoomFragmentRoomBinding;

/**
 * @author lq
 * <p>
 * Fragment 监听 数据 展示 UI
 * RoomViewModel 获取并发送数据
 * MainViewModel 用于通用逻辑管理（退出频道等)
 */
public class RoomFragment extends BaseNavFragment<RoomFragmentRoomBinding> {
    public static final String roomKey = "CurrentRoom";

    private RoomViewModel mViewModel;
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        assert getArguments() != null;
        RoomInfo roomInfo = (RoomInfo) getArguments().getSerializable(roomKey);
        if (roomInfo != null) {
            mViewModel = new ViewModelProvider(getViewModelStore(), new RoomViewModelFactory(requireContext(), roomInfo)).get(RoomViewModel.class);
            // Set mic control CheckBox's position
            // 设置麦克风控制按钮的位置
            ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
                Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom);
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mBinding.checkboxMicFgRoom.getLayoutParams();
                lp.bottomMargin = desiredBottom;

                mBinding.tabLayoutFgRoom.setPadding(0, inset.top, 0, 0);

                return WindowInsetsCompat.CONSUMED;
            });

            initView();
            initListener();
        }
    }

    private void initView() {
        RoomUtil.configInputDialog(mBinding.viewInputFgRoom.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgRoom.inputLayoutViewInput);
        mBinding.viewInputFgRoom.titleViewInput.setText(R.string.room_fab_add_sub_room);

        // Add default Tab
        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        mBinding.tabLayoutFgRoom.addTab(tab);
        tab.setText(mViewModel.currentRoomInfo.getId());
    }

    private void initListener() {
        // Adjust view pos
        fixPosition();

        mBinding.tabLayoutFgRoom.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                CharSequence tabText = tab.getText();
                if (tabText != null) pendingSwitchRoom(tabText.toString().trim());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        mBinding.checkboxMicFgRoom.setOnCheckedChangeListener((buttonView, isChecked) -> mViewModel.mute(isChecked));

        // Inner Dialog
        mBinding.viewInputFgRoom.btnConfirmViewLayout.setOnClickListener(this::pendingCreateSubRoom);
        mBinding.viewInputFgRoom.getRoot().setOnClickListener(this::clearFocus);

        // Show Dialog
        mBinding.fabFgRoom.setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(true));
        mBinding.scrimFgRoom.setOnClickListener(v -> mBinding.fabFgRoom.setExpanded(false));

        // View Status
        mViewModel.viewStatus().observe(getViewLifecycleOwner(), this::onViewStatusChanged);

        // Observe SubRoom
        mViewModel.subRoomList().observe(getViewLifecycleOwner(), this::onSubRoomListUpdated);
        // RTC init 成功
        mViewModel.mEngine().observe(getViewLifecycleOwner(), this::onEngineInit);
        // 用户加入 RTC 频道
        mViewModel.rtcUserList().observe(getViewLifecycleOwner(), this::onRtcUserSetUpdated);
    }

    private void onViewStatusChanged(ViewStatus viewStatus) {
        if (viewStatus instanceof ViewStatus.Loading)
            showLoading();
        else if (viewStatus instanceof ViewStatus.Done) {
            dismissLoading();
            if (mBinding.scrimFgRoom.getVisibility() == View.VISIBLE) {
                mBinding.scrimFgRoom.performClick();
                mBinding.getRoot().postDelayed(() -> BaseUtil.hideKeyboardCompat(mBinding.viewInputFgRoom.editTextViewInput), 500);
            }
        } else if (viewStatus instanceof ViewStatus.Error) {
            dismissLoading();
            BaseUtil.toast(requireContext(), ((ViewStatus.Error) viewStatus).msg);
        }
    }

    private void onEngineInit(RtcEngine engine){
        // RTC 创建失败 ==》返回上一页面
        if (engine == null) findNavController().popBackStack();
            // RTC 创建成功 ==》创建本地视图，准备加入频道
        else onInitRTCSucceed();
    }

    /**
     * We use LinkedHashSet to keep the order.
     */
    private void onRtcUserSetUpdated(Set<Integer> set) {
        Map<Integer, Boolean> diffMap = getDiffMap(set);
        for (Integer i : diffMap.keySet()) {
            Boolean res = diffMap.get(i);
            if (res == null) continue;
            if (res){
                doAddUser(i);
            }else{
                doRemoveUser(i);
            }
        }
    }

    /**
     * @return map key 为 uid，value 为 是否添加
     * value = null,不改变
     * value = true,添加
     * value = false,删除
     */
    private Map<Integer,Boolean> getDiffMap(Set<Integer> dataSet){
        Map<Integer, Boolean> resMap = new HashMap<>();
        ConstraintLayout container = mBinding.dynamicViewFgRoom.flexContainer;
        int childCount = container == null ? 0 : container.getChildCount();

        // 将现有 View 的 Tag 添加到 map,赋值为false
        for (int i = 1; i < childCount; i++) {
            resMap.put((Integer) container.getChildAt(i).getTag(), false);
        }
        // 将新的即将呈现的 uid 添加到 map
        // 如果已经存在，则赋值为 null
        for (Integer data : dataSet) {
            Boolean previous = resMap.put(data, true);
            if (previous != null) resMap.put(data, null);
        }
        return resMap;
    }

    /**
     * Remove all sub every time it updated
     */
    private void onSubRoomListUpdated(List<SubRoomInfo> list) {
        // remove duplicated SubRoom
        // 移除重复的子房间
        if (list.size() > 1) {
            for (int i = list.size() - 1; i > 0; i--) {
                for (int j = i -1; j >= 0; j--) {
                    if (Objects.equals(list.get(i).getSubRoom(), list.get(j).getSubRoom())) {
                        list.remove(i);
                        break;
                    }
                }
            }
        }

        int desiredTabPos = 0;
        while (mBinding.tabLayoutFgRoom.getTabCount()>1)
            mBinding.tabLayoutFgRoom.removeTabAt(1);

        for (SubRoomInfo subRoomInfo : list) {
            if (Objects.equals(subRoomInfo.getSubRoom(), mViewModel.currentSubRoom))
                desiredTabPos = list.indexOf(subRoomInfo) + 1;
            addTab(subRoomInfo);
        }

        mBinding.tabLayoutFgRoom.selectTab(mBinding.tabLayoutFgRoom.getTabAt(desiredTabPos));
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
            int error = isValidRoomName(inputText);
            if (error == 0)
                mViewModel.createSubRoom(inputText);
            else RoomUtil.showNameIllegalError(inputLayout, error);
        } else {
            RoomUtil.showNameIllegalError(inputLayout, R.string.room_please_input_name);
        }
    }

    private void addTab(@NonNull SubRoomInfo subRoomInfo) {
        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        mBinding.tabLayoutFgRoom.addTab(tab);
        tab.setText(subRoomInfo.getSubRoom());
    }

    private void onInitRTCSucceed() {
        // 为了区分本地与远端，我们给本地加上 负号 Tag
        CardView childVideoCardView = RoomUtil.getChildVideoCardView(requireContext(), true, Integer.parseInt(RoomConstant.userId));
        mBinding.dynamicViewFgRoom.dynamicAddView(childVideoCardView);

        mViewModel.setupLocalView(childVideoCardView);
        mViewModel.joinRTCRoom(mViewModel.currentRoomInfo.getId());
    }

    private void pendingSwitchRoom(@NonNull String roomName) {
        ConstraintLayout container = mBinding.dynamicViewFgRoom.flexContainer;
        int childCount = container == null ? 0 : container.getChildCount();
        for (int i = 1; i < childCount; i++) {
            mBinding.dynamicViewFgRoom.dynamicRemoveView(container.getChildAt(i));
        }
        mViewModel.joinSubRoom(roomName);
    }

    @MainThread
    private void doAddUser(int uid) {
        CardView childVideoCardView = RoomUtil.getChildVideoCardView(requireContext(),false, uid);
        mBinding.dynamicViewFgRoom.dynamicAddView(childVideoCardView);

        mViewModel.setupRemoteView(childVideoCardView, uid);
    }

    /**
     * 切记在主线程操作，否则UI会卡住
     */
    @MainThread
    private void doRemoveUser(int uid) {
        BaseUtil.logD("doRemoveUser");
        mBinding.dynamicViewFgRoom.dynamicRemoveView(mBinding.dynamicViewFgRoom.findViewWithTag(uid));
        mViewModel.onUserLeft(uid);
    }

    @StringRes
    private int isValidRoomName(@NonNull String name) {
        int tabCount = mBinding.tabLayoutFgRoom.getTabCount();
        if (tabCount > 3) return R.string.room_room_count_limited;

        // Skip the first one
        for (int i = 1; i < tabCount; i++) {
            TabLayout.Tab tempTab = mBinding.tabLayoutFgRoom.getTabAt(i);
            if (tempTab == null) continue;

            CharSequence tabText = tempTab.getText();
            if (tabText == null) continue;

            if (tabText.toString().trim().equals(name))
                return R.string.room_room_name_duplicated;
        }
        Pattern pattern = Pattern.compile(requireContext().getString(R.string.room_room_name_regex));
        if (!pattern.matcher(name).matches())
            return R.string.room_room_name_restrict;
        return 0;
    }

    private void clearFocus(View v) {
        mBinding.viewInputFgRoom.inputLayoutViewInput.clearFocus();
        BaseUtil.hideKeyboardCompat(mBinding.viewInputFgRoom.inputLayoutViewInput);
    }

    private void fixPosition() {
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
        mBinding.appBarFgRoom.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
    }

    @Override
    public void onDestroyView() {
        mBinding.appBarFgRoom.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        super.onDestroyView();
    }

}