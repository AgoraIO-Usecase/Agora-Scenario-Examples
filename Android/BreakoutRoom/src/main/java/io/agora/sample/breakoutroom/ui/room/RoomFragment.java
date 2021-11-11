package io.agora.sample.breakoutroom.ui.room;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.navigation.Navigation;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Objects;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseUtil;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomConstant;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.bean.SubRoomInfo;
import io.agora.sample.breakoutroom.databinding.FragmentRoomBinding;
import io.agora.sample.breakoutroom.ui.MainViewModel;

/**
 * @author lq
 * <p>
 * Fragment 监听 数据 展示 UI
 * RoomViewModel 获取并发送数据
 * MainViewModel 用于通用逻辑管理（退出频道等)
 */
public class RoomFragment extends BaseFragment<FragmentRoomBinding> {
    public static final String currentRoom = "CurrentRoom";

    private RoomViewModel mViewModel;
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        assert getArguments() != null;
        RoomInfo roomInfo = (RoomInfo) getArguments().getSerializable(currentRoom);
        assert roomInfo != null;
        mViewModel = new ViewModelProvider(getViewModelStore(), new RoomViewModelFactory(roomInfo)).get(RoomViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), desiredBottom);
            return insets;
        });

        initView();
        initListener();

        mViewModel.initRTC(requireContext(), new IRtcEngineEventHandler() {
            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                mViewModel.mute(mBinding.checkboxMicFgRoom.isChecked());
                mViewModel.setupLocalView((ViewGroup) mBinding.dynamicViewFgRoom.flexContainer.getChildAt(0));
            }

            @Override
            public void onError(int err) {
                BaseUtil.logE(RtcEngine.getErrorDescription(err));
            }

            /**
             * {@see <a href="https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler.html?platform=Android#a31b2974a574ec45e62bb768e17d1f49e">}
             */
            @Override
            public void onConnectionStateChanged(int state, int reason) {
                BaseUtil.logD("onConnectionStateChanged:" + state + "," + reason);
            }

            @Override
            public void onUserJoined(int uid, int elapsed) {
                mBinding.getRoot().post(() -> doAddUser(uid));
            }

            @Override
            public void onUserOffline(int uid, int reason) {
                mBinding.getRoot().post(() -> doRemoveUser(uid));
            }
        });
    }

    private void initView() {
        RoomUtil.configInputDialog(mBinding.viewInputFgRoom.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgRoom.inputLayoutViewInput);
        mBinding.viewInputFgRoom.titleViewInput.setText(R.string.fab_add_sub_room);

        // Add default Tab
        TabLayout.Tab tab = mBinding.tabLayoutFgRoom.newTab();
        mBinding.tabLayoutFgRoom.addTab(tab);
        tab.setText(mViewModel.currentRoomInfo.getId());

    }

    private void initListener() {

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
        mViewModel.viewStatus().observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Loading)
                showLoading();
            else if (viewStatus instanceof ViewStatus.Done) {
                dismissLoading();
                if (mBinding.scrimFgRoom.getVisibility() == View.VISIBLE) {
                    mBinding.scrimFgRoom.performClick();
                    mBinding.getRoot().postDelayed(() -> BaseUtil.hideKeyboard(requireActivity().getWindow(), mBinding.viewInputFgRoom.editTextViewInput), 500);
                }
            } else if (viewStatus instanceof ViewStatus.Error) {
                dismissLoading();
                BaseUtil.toast(requireContext(), ((ViewStatus.Error) viewStatus).msg);
            }
        });

        // Observe SubRoom
        mViewModel.subRoomList().observe(getViewLifecycleOwner(), this::onSubRoomListUpdated);

        // RTC init 成功
        mViewModel.mEngine().observe(getViewLifecycleOwner(), engine -> {
            // RTC 创建失败 ==》返回上一页面
            if (engine == null) Navigation.findNavController(mBinding.getRoot()).popBackStack();
                // RTC 创建成功 ==》创建本地视图，准备加入频道
            else onInitRTCSucceed();
        });
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

    private void onInitRTCSucceed() {
        CardView childVideoCardView = RoomViewModel.getChildVideoCardView(requireContext(), Integer.parseInt(RoomConstant.userId));
        mBinding.dynamicViewFgRoom.dynamicAddView(childVideoCardView);

        mViewModel.setupLocalView(childVideoCardView);
        mViewModel.joinRoom(mViewModel.currentRoomInfo.getId());
    }

    private void pendingSwitchRoom(@NonNull String roomName) {
        ConstraintLayout container = mBinding.dynamicViewFgRoom.flexContainer;
        int childCount = container.getChildCount();
        for (int i = 1; i < childCount; i++) {
            mBinding.dynamicViewFgRoom.dynamicRemoveView(container.getChildAt(i));
        }
        mViewModel.joinSubRoom(roomName);
    }

    @MainThread
    private void doAddUser(int uid) {
        CardView childVideoCardView = RoomViewModel.getChildVideoCardView(requireContext(), uid);
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
    }

    @StringRes
    private int isValidRoomName(@NonNull String name) {
        int tabCount = mBinding.tabLayoutFgRoom.getTabCount();
        if (tabCount > 3) return R.string.room_count_limited;

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

    private void showNameIllegalError(@StringRes int illegalReason) {
        TextInputLayout input = mBinding.viewInputFgRoom.inputLayoutViewInput;
        if (input.isErrorEnabled())
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