package io.agora.sample.rtegame.ui.roompage;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.rtc2.RtcEngine;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.base.BaseFragment;
import io.agora.sample.rtegame.bean.GameInfo;
import io.agora.sample.rtegame.bean.PKApplyInfo;
import io.agora.sample.rtegame.bean.PKInfo;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.FragmentRoomBinding;
import io.agora.sample.rtegame.databinding.ItemRoomMessageBinding;
import io.agora.sample.rtegame.ui.roompage.hostdialog.HostListDialog;
import io.agora.sample.rtegame.ui.roompage.moredialog.MoreDialog;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.view.LiveHostCardView;
import io.agora.sample.rtegame.view.LiveHostLayout;

public class RoomFragment extends BaseFragment<FragmentRoomBinding> {

    private RoomViewModel mViewModel;

    private BaseRecyclerViewAdapter<ItemRoomMessageBinding, String, MessageHolder> mMessageAdapter;

    private RoomInfo currentRoom;
    private boolean aMHost;

    private AlertDialog currentDialog;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GlobalViewModel mGlobalModel = GameUtil.getViewModel(requireActivity(), GlobalViewModel.class);

        if (mGlobalModel.roomInfo.getValue() != null)
            currentRoom = mGlobalModel.roomInfo.getValue().peekContent();
        if (currentRoom == null) {
            findNavController().navigate(R.id.action_roomFragment_to_roomCreateFragment);
        } else {
            aMHost = currentRoom.getUserId().equals(GameApplication.getInstance().user.getUserId());
            mViewModel = GameUtil.getViewModel(this, RoomViewModel.class, new RoomViewModelFactory(requireContext(), currentRoom));
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

        hideBtnByCurrentRole();
    }

    private void hideBtnByCurrentRole() {
        mBinding.btnGameFgRoom.setVisibility(aMHost ? View.VISIBLE : View.GONE);
        mBinding.btnMoreFgRoom.setVisibility(aMHost ? View.VISIBLE : View.GONE);
        mBinding.btnDonateFgRoom.setVisibility(aMHost ? View.GONE : View.VISIBLE);
    }

    private void initListener() {
        // 沉浸处理
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

        // "更多"弹窗
        mBinding.btnMoreFgRoom.setOnClickListener((v) -> new MoreDialog().show(getChildFragmentManager(), MoreDialog.TAG));
        // "游戏"弹窗
        mBinding.btnGameFgRoom.setOnClickListener((v) -> new HostListDialog().show(getChildFragmentManager(), HostListDialog.TAG));
        // "退出直播间"按钮点击事件
        mBinding.btnExitFgRoom.setOnClickListener((v) -> requireActivity().onBackPressed());
        // 显示键盘按钮
        mBinding.inputFgRoom.setOnClickListener(v -> BaseUtil.showKeyboard(requireActivity().getWindow(), mBinding.editTextFgRoom));
        // RTC engine 初始化监听
        mViewModel.mEngine().observe(getViewLifecycleOwner(), this::onRTCInit);
        // 连麦成功《==》主播上线
        mViewModel.subRoomInfo().observe(getViewLifecycleOwner(), this::onHostOnline);

        if (aMHost) {
            // 主播，监听连麦信息
            mViewModel.pkApplyInfo().observe(getViewLifecycleOwner(), this::onPKApplyInfoChanged);
        } else {
            // 观众，监听主播上线
            mViewModel.hostUID().observe(getViewLifecycleOwner(), (uid) -> {
                if (String.valueOf(uid).equals(GameApplication.getInstance().user.getUserId())) {
                    onHostOnline(currentRoom);
                }
            });
        }
        // 监听PK信息
        mViewModel.pkInfo().observe(getViewLifecycleOwner(), this::onPKInfoChanged);
        mViewModel.gameInfo().observe(getViewLifecycleOwner(), this::onGameChanged);

    }

    /**
     * 观众：{@link GameInfo#PLAYING} 订阅视频流 ,{@link GameInfo#END} 取消订阅
     * 主播：@{@link GameInfo#IDLE} 加载WebView， {@link GameInfo#END} 卸载 WebView
     */
    private void onGameChanged(GameInfo gameInfo) {
        if (gameInfo == null) return;
        if (gameInfo.getStatus() == GameInfo.IDLE) {
//            if (aMHost) addWebView();
        } else if (gameInfo.getStatus() == GameInfo.PLAYING) {
//            if (!aMHost) addScreenShare();
        } else if (gameInfo.getStatus() == GameInfo.END) {
//            if (aMHost) removeWebView();
//            else removeScreenShare();
        }
    }

    /**
     * {@link PKInfo#AGREED} 加入对方频道，拉流 | {@link PKInfo#END} 退出频道
     */
    private void onPKInfoChanged(PKInfo pkInfo) {
        if (pkInfo == null) return;
        if (pkInfo.getStatus() == PKInfo.AGREED) {
            // this variable will only for join channel so room name doesn't matter.
            RoomInfo subRoom = new RoomInfo(pkInfo.getRoomId(), "", pkInfo.getUserId());
            mViewModel.joinSubRoom(subRoom, GameApplication.getInstance().user);
        } else if (pkInfo.getStatus() == PKInfo.END) {
            mViewModel.leaveSubRoom(pkInfo.getRoomId());
        }
    }

    /**
     * 仅主播调用
     */
    private void onPKApplyInfoChanged(PKApplyInfo pkApplyInfo) {
        if (pkApplyInfo == null) return;

        switch (pkApplyInfo.getStatus()) {
            case PKApplyInfo.APPLYING: {
                showPKDialog(pkApplyInfo);
                break;
            }
            case PKApplyInfo.REFUSED: {
                // 发起方提示
                if (pkApplyInfo.getRoomId().equals(currentRoom.getId())) {
                    if (currentDialog != null) currentDialog.dismiss();
                    mMessageAdapter.addItem("玩家拒绝");
                }
                break;
            }
            case PKApplyInfo.AGREED: {
                mViewModel.startPK(pkApplyInfo);
                break;
            }
            case PKApplyInfo.END: {
                mViewModel.endPK(pkApplyInfo.getTargetRoomId());
                break;
            }
        }
    }

    /**
     * 仅主播调用
     */
    private void showPKDialog(PKApplyInfo pkApplyInfo) {
        if (pkApplyInfo.getRoomId().equals(currentRoom.getId()))
            mMessageAdapter.addItem("你画我猜即将开始，等待其他玩家...");
//            currentDialog = new AlertDialog.Builder(requireContext()).setMessage("你画我猜即将开始，等待其他玩家...").setCancelable(false)
//                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
//                        mViewModel.cancelPK(pkApplyInfo);
//                        dialog.dismiss();
//                    }).show();
        else
            currentDialog = new AlertDialog.Builder(requireContext()).setMessage("您的好友邀请您加入游戏").setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        mViewModel.acceptPK(pkApplyInfo);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        mViewModel.cancelPK(pkApplyInfo);
                        dialog.dismiss();
                    }).show();
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

            mViewModel.joinRoom(GameApplication.getInstance().user);
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
        mViewModel.setupLocalView(view.renderTextureView, GameApplication.getInstance().user);
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
        mMessageAdapter.addItem("正在加载主播【" + currentRoom.getTempUserName() + "】视频");
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
        mMessageAdapter.addItem("正在加载连麦主播【" + subRoomInfo.getTempUserName() + "】视频");
        LiveHostCardView view = mBinding.hostContainerFgRoom.createSubHostView();
        mBinding.hostContainerFgRoom.setType(LiveHostLayout.Type.DOUBLE_IN_GAME);
        mViewModel.setupRemoteView(view.renderTextureView, subRoomInfo, false);
    }

}
