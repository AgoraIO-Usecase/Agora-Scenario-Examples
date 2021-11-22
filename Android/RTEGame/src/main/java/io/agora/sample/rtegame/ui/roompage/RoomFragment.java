package io.agora.sample.rtegame.ui.roompage;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.rtc2.RtcEngine;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.base.BaseFragment;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.FragmentRoomBinding;
import io.agora.sample.rtegame.databinding.ItemRoomMessageBinding;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.view.LiveHostCardView;
import io.agora.sample.rtegame.view.LiveHostLayout;

public class RoomFragment extends BaseFragment<FragmentRoomBinding> {

    private GlobalViewModel mGlobalModel;
    private RoomViewModel mViewModel;

    private BaseRecyclerViewAdapter<ItemRoomMessageBinding, String, MessageHolder> mMessageAdapter;

    private RoomInfo currentRoom;
    private boolean aMHost;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlobalModel = new ViewModelProvider(requireActivity()).get(GlobalViewModel.class);

        if (mGlobalModel.roomInfo.getValue() != null)
            currentRoom = mGlobalModel.roomInfo.getValue().peekContent();
        if (currentRoom == null) {
            findNavController().navigate(R.id.action_roomFragment_to_roomCreateFragment);
        } else {
            aMHost = currentRoom.getUserId().equals(mGlobalModel.getLocalUser().getUserId());
            mViewModel = new ViewModelProvider(getViewModelStore(), new RoomViewModelFactory(requireContext())).get(RoomViewModel.class);
            initView();
            initListener();
        }
    }

    private void initView() {
        mBinding.avatarHostFgRoom.setImageResource(GameUtil.getAvatarFromUserId(currentRoom.getUserId()));
        mBinding.nameHostFgRoom.setText(currentRoom.getTempUserName());

        mMessageAdapter = new BaseRecyclerViewAdapter<>(null, MessageHolder.class);
        mBinding.recyclerViewFgRoom.setAdapter(mMessageAdapter);
        // Android 12 over_scroll animation is phenomenon
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            mBinding.recyclerViewFgRoom.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int desiredBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom - inset.bottom;
            // 整体
            mBinding.containerOverlayFgRoom.setPadding(inset.left, inset.top, inset.right, inset.bottom);
            // 输入框
            mBinding.inputLayoutFgRoom.setVisibility(insets.isVisible(WindowInsetsCompat.Type.ime()) ? View.VISIBLE : View.GONE);

            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mBinding.inputLayoutFgRoom.getLayoutParams();
            layoutParams.bottomMargin = desiredBottom;
            mBinding.inputLayoutFgRoom.setLayoutParams(layoutParams);

            return WindowInsetsCompat.CONSUMED;
        });

        mBinding.btnExitFgRoom.setOnClickListener((v) -> requireActivity().onBackPressed());
        mBinding.inputFgRoom.setOnClickListener(v -> BaseUtil.showKeyboard(requireActivity().getWindow(), mBinding.editTextFgRoom));
        // RTC engine
        mViewModel.mEngine().observe(getViewLifecycleOwner(), this::onRTCInit);
        // 连麦成功
        mViewModel.subRoomInfo().observe(getViewLifecycleOwner(), this::onHostOnline);
        // 主播上线
        if (!aMHost)
            mViewModel.hostUID().observe(getViewLifecycleOwner(), (uid) -> {
                if (String.valueOf(uid).equals(mGlobalModel.getLocalUser().getUserId())) {
                    onHostOnline(currentRoom);
                }
            });

    }

    /**
     * RTC 初始化成功
     */
    private void onRTCInit(RtcEngine engine) {
        if (engine == null) findNavController().popBackStack();
        else {
            mMessageAdapter.addItem("RTC 初始化成功");
            // 如果是房主，创建View开始直播
            if (aMHost) initLocalView();

            mViewModel.joinRoom(currentRoom, mGlobalModel.getLocalUser());
        }
    }

    /**
     * 本地 View 初始化
     * 仅主播本人调用
     */
    @MainThread
    private void initLocalView() {
        LiveHostCardView view = mBinding.hostContainerFgRoom.createHostView();
        mBinding.hostContainerFgRoom.setType(LiveHostLayout.Type.HOST_ONLY);
        mViewModel.setupLocalView(view.renderTextureView, mGlobalModel.getLocalUser());
        mMessageAdapter.addItem("画面加载完成");
    }


    /**
     * 有主播上线
     */
    @MainThread
    private void onHostOnline(RoomInfo roomInfo) {
        if (mBinding.hostContainerFgRoom.hostView == null)
            onHostJoin();
        if (!roomInfo.getId().equals(currentRoom.getId()))
            onSubHostJoin(roomInfo);
    }

    /**
     * 主播上线
     */
    @MainThread
    private void onHostJoin() {
        mMessageAdapter.addItem("正在加载主播【"+ currentRoom.getTempUserName() +"】视频");
        LiveHostLayout liveHost = mBinding.hostContainerFgRoom;

        LiveHostCardView view = liveHost.createHostView();
        liveHost.setType(liveHost.getChildCount() == 1 ? LiveHostLayout.Type.HOST_ONLY : liveHost.getType());
        mViewModel.setupRemoteView(view.renderTextureView, currentRoom, true);
    }

    /**
     * 连麦主播上线
     */
    @MainThread
    private void onSubHostJoin(RoomInfo subRoomInfo) {
        mMessageAdapter.addItem("正在加载连麦主播【"+ subRoomInfo.getTempUserName() +"】视频");
        LiveHostCardView view = mBinding.hostContainerFgRoom.createSubHostView();
        mBinding.hostContainerFgRoom.setType(LiveHostLayout.Type.DOUBLE_IN_GAME);
        mViewModel.setupRemoteView(view.renderTextureView, subRoomInfo, false);
    }

}
