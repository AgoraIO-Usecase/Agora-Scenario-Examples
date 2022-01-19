package io.agora.scene.onelive.ui.room;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import io.agora.example.base.BaseUtil;
import io.agora.scene.onelive.GlobalViewModel;
import io.agora.scene.onelive.R;
import io.agora.scene.onelive.base.BaseNavFragment;
import io.agora.scene.onelive.bean.AgoraGame;
import io.agora.scene.onelive.bean.GameInfo;
import io.agora.scene.onelive.bean.RoomInfo;
import io.agora.scene.onelive.databinding.OneFragmentRoomBinding;
import io.agora.scene.onelive.repo.GameRepo;
import io.agora.scene.onelive.ui.room.game.GameListDialog;
import io.agora.scene.onelive.util.Event;
import io.agora.scene.onelive.util.NormalContainerInsetsListener;
import io.agora.scene.onelive.util.OneUtil;
import io.agora.scene.onelive.util.ViewStatus;

public class RoomFragment extends BaseNavFragment<OneFragmentRoomBinding> {
    private boolean amHost;
    private RoomInfo currentRoom;
    private BottomSheetBehavior<FrameLayout> sheetBehavior;

    private RoomViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        GlobalViewModel mGlobalModel = OneUtil.getAndroidViewModel(this, GlobalViewModel.class);
        // hold current RoomInfo
        if (mGlobalModel.roomInfo.getValue() != null)
            currentRoom = mGlobalModel.roomInfo.getValue().peekContent();
        if (currentRoom == null) {
            findNavController().navigate(R.id.action_roomFragment_to_roomCreateFragment);
            return null;
        }
        mViewModel = OneUtil.getViewModel(this, RoomViewModel.class, new RoomViewModelFactory(requireContext(), mGlobalModel.localUser, currentRoom));
        //  See if current user is the host
        amHost = currentRoom.getUserId().equals(mViewModel.localUser.getUserId());

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupInsets();

        initView();
        initListener();
        initLiveDataObserver();
    }


    @SuppressLint("SetJavaScriptEnabled")
    public void initView() {
        // BottomSheet indicator
        mBinding.indicatorSheetFgRoom.setIndicatorColor(Color.WHITE);
        // WebView
        WebSettings settings = mBinding.gameViewFgRoom.getSettings();
        settings.setJavaScriptEnabled(true);

        mBinding.gameViewFgRoom.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().getScheme().startsWith("http"))
                    view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
        mBinding.gameViewFgRoom.addJavascriptInterface(new AgoraJsBridge(mViewModel), "agoraJSBridge");
        // 给 WebView 添加透明背景
        mBinding.gameViewFgRoom.setBackgroundColor(BaseUtil.getColorInt(requireContext(), R.attr.colorPrimary));
        // 给 WebView 添加圆角
        float webViewCorner = BaseUtil.dp2px(8);
        mBinding.gameViewFgRoom.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(view.getLeft(), view.getTop(), view.getRight(), (int) (view.getBottom() + webViewCorner), webViewCorner);
            }
        });
        mBinding.gameViewFgRoom.setClipToOutline(true);

        // BottomSheetBehavior
        sheetBehavior = BottomSheetBehavior.from(mBinding.bottomSheetFgRoom);
        sheetBehavior.setGestureInsetBottomIgnored(true);
        sheetBehavior.setDraggable(false);
        sheetBehavior.setHideable(false);
        if (amHost)
            onSingleHostState();
        else
            onDoubleHostState();
    }

    private void initListener() {

        // 监听"返回键"
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // No-OP
            }
        });

        // Indicator
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                RoomFragment.this.onSheetStateChanged(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                RoomFragment.this.onSlide(slideOffset);
            }
        });
        // 游戏 Sheet 指示器
        mBinding.indicatorSheetFgRoom.setOnClickListener(v -> toggleBottomSheet());
        // 翻转相机 按钮
        mBinding.btnFlipFgRoom.setOnClickListener(v -> mViewModel.flipCamera());
        // 游戏 按钮
        mBinding.btnStartGameFgRoom.setOnClickListener(v -> RoomFragment.this.showGameCenterDialog());
        mBinding.btnExitGameFgRoom.setOnClickListener(v -> showAlertExitGameDialog());
        // 退出房间按钮
        mBinding.btnEndLiveFgRoom.setOnClickListener(v -> {
            if (amHost)
                RoomFragment.this.showAlertEndLiveDialog();
            else
                findNavController().popBackStack();
        });
        // 停止连麦 按钮
        mBinding.btnEndCallFgRoom.setOnClickListener(v -> RoomFragment.this.showAlertEndCallDialog());

        mBinding.btnMicFgRoom.addOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed()) mViewModel.enableMic(button.isChecked());
        });
        mBinding.btnMic2FgRoom.addOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed()) mViewModel.enableMic(button.isChecked());
        });

    }

    private void initLiveDataObserver() {
        // RTC 对方用户
        mViewModel.targetUser().observe(getViewLifecycleOwner(), localUser -> {
            if (localUser != null) {
                mBinding.hostViewFgRoom.setSingleHost(false);
                RoomFragment.this.onDoubleHostState();
                int uid = -1;
                try {
                    uid = Integer.parseInt(localUser.getUserId());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (uid != -1)
                    mViewModel.setupRemoteView(mBinding.hostViewFgRoom.getTargetViewport(), uid);
            } else {
                mBinding.hostViewFgRoom.setSingleHost(true);
                RoomFragment.this.onSingleHostState();
            }
        });

        // RTC 引擎初始化
        mViewModel.mEngine().observe(getViewLifecycleOwner(), rtcEngineEx -> {
            if (rtcEngineEx != null) {
                BaseUtil.logD("RTC ready");
                mViewModel.setupLocalView(mBinding.hostViewFgRoom.getCurrentViewport());
            } else {
                BaseUtil.toast(requireContext(), "RTC init error");
                findNavController().popBackStack();
            }
        });

        // 当前游戏信息
        mViewModel.gameInfo().observe(getViewLifecycleOwner(), this::onGameStatusChanged);

        // 静音状态
        mViewModel.isLocalMicMuted.observe(getViewLifecycleOwner(), isMuted -> {
            int resId = isMuted ? R.drawable.one_ic_microphone_off : R.drawable.one_ic_microphone;
            mBinding.btnMicFgRoom.setIconResource(resId);
            mBinding.btnMicFgRoom.setChecked(!isMuted);
            mBinding.btnMic2FgRoom.setIconResource(resId);
            mBinding.btnMic2FgRoom.setChecked(!isMuted);
        });

        mViewModel.viewStatus().observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Error) {
                BaseUtil.toast(requireContext(), "对方已挂断");
                findNavController().popBackStack();
            }
        });

        mViewModel.gameStartUrl.observe(getViewLifecycleOwner(), stringEvent -> mBinding.gameViewFgRoom.loadUrl(stringEvent.getContentIfNotHandled()));
    }

    //<editor-fold desc="Dialog Related">

    /**
     * 终止连线 弹窗
     * 主播：通知 观众 下线==》观众下线 ==》endGame、单用户视图
     * 观众：endGame、退出
     */
    private void showAlertEndCallDialog() {
        new AlertDialog.Builder(requireContext()).setTitle(R.string.one_end_call)
                .setMessage(R.string.one_end_call_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    findNavController().popBackStack();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 退出游戏 弹窗
     * 主播、观众：endGame ==》双人视图
     */
    private void showAlertExitGameDialog() {
        new AlertDialog.Builder(requireContext()).setTitle(R.string.one_exit_game)
                .setMessage(R.string.one_exit_game_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> mViewModel.requestEndGame())
                .setCancelable(false)
                .show();
    }

    /**
     * 关闭直播间 弹窗
     */
    private void showAlertEndLiveDialog() {
        new AlertDialog.Builder(requireContext()).setTitle(R.string.one_exit_room)
                .setMessage(R.string.one_exit_room_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> findNavController().popBackStack())
                .setCancelable(false)
                .show();
    }

    private void showGameCenterDialog() {
        new GameListDialog().show(getChildFragmentManager(), GameListDialog.TAG);
    }

    private void showGameInviteDialog(GameInfo game) {
        new AlertDialog.Builder(requireContext()).setTitle(R.string.one_invite_received)
                .setMessage(R.string.one_invite_received_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, ((dialog, which) -> RoomFragment.this.startGame(game)))
                .setCancelable(false)
                .show();
    }

    private void startGame(GameInfo gameInfo) {
        // FIXME DO IN BACKGROUND
        mViewModel.currentGame = new AgoraGame(gameInfo.getGameId(), "");

        mBinding.btnStartGameFgRoom.setEnabled(false);
        mBinding.btnStartGameFgRoom.setAlpha(0.5f);
        mBinding.overlayFgRoom.setVisibility(View.VISIBLE);
        mBinding.getRoot().post(() -> sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));

        mViewModel.startGame(gameInfo.getGameId());
    }
    //</editor-fold>

    private void onSingleHostState() {
        BaseUtil.logD("onSingleHostState");
        mBinding.hostViewFgRoom.setSingleHost(true);

        mBinding.btnEndLiveFgRoom.setVisibility(View.VISIBLE);
        mBinding.btnStartGameFgRoom.setVisibility(View.GONE);
        mBinding.btnEndCallFgRoom.setVisibility(View.GONE);

        mBinding.topBtnGroupFgRoom.setVisibility(View.GONE);
        mBinding.overlayFgRoom.setVisibility(View.GONE);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void onDoubleHostState() {
        BaseUtil.logD("onDoubleHostState");
        mBinding.hostViewFgRoom.setSingleHost(false);

        mBinding.btnEndLiveFgRoom.setVisibility(View.GONE);
        mBinding.btnStartGameFgRoom.setVisibility(View.VISIBLE);
        mBinding.btnEndCallFgRoom.setVisibility(View.VISIBLE);

        mBinding.topBtnGroupFgRoom.setVisibility(View.GONE);
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
            mBinding.scrimBgdFgRoom.setAlpha(0f);

        if (mViewModel.currentGame != null) {
            mBinding.overlayFgRoom.setVisibility(View.VISIBLE);
        } else {
            mBinding.overlayFgRoom.setVisibility(View.GONE);
        }
    }

    private void onGameStatusChanged(@NonNull GameInfo gameInfo) {
        BaseUtil.logD("onGameStatusChanged:" + gameInfo.getStatus());

        if (gameInfo.getStatus() == GameInfo.START) {
            if (mViewModel.startedByMe)
                startGame(gameInfo);
            else
                showGameInviteDialog(gameInfo);

            mViewModel.startedByMe = false;
        } else {

            mBinding.btnStartGameFgRoom.setEnabled(true);
            mBinding.btnStartGameFgRoom.setAlpha(1f);

            mBinding.gameViewFgRoom.loadUrl("");
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mBinding.overlayFgRoom.setVisibility(View.GONE);
        }
    }

    private void onSheetStateChanged(int newState) {
        boolean isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED;
        mBinding.topBtnGroupFgRoom.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        if (isExpanded)
            mBinding.scrimBgdFgRoom.setAlpha(1f);
        else if (newState == BottomSheetBehavior.STATE_COLLAPSED)
            mBinding.scrimBgdFgRoom.setAlpha(0f);
    }

    private void onSlide(float slideOffset) {
        mBinding.indicatorSheetFgRoom.setCurrentFraction(slideOffset);
        mBinding.scrimBgdFgRoom.setAlpha(slideOffset);
    }

    private void toggleBottomSheet() {
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.containerFgRoom, new NormalContainerInsetsListener());
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.hostViewFgRoom, (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mBinding.hostViewFgRoom.getViewportContainer().getLayoutParams();
            lp.rightMargin = (int) (inset.right + BaseUtil.dp2px(16));
            lp.topMargin = (int) (inset.top + BaseUtil.dp2px(16));
            mBinding.hostViewFgRoom.getViewportContainer().setLayoutParams(lp);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.overlayFgRoom, (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.overlayFgRoom.setPadding(0, inset.top, 0, 0);
            sheetBehavior.setPeekHeight((int) BaseUtil.dp2px(36) + inset.bottom);
            return insets;
        });
    }

}
