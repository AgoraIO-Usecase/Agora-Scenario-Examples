package io.agora.sample.rtegame.ui.room;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.Objects;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.rtc2.RtcEngine;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.base.BaseFragment;
import io.agora.sample.rtegame.bean.GameInfo;
import io.agora.sample.rtegame.bean.GiftInfo;
import io.agora.sample.rtegame.bean.PKApplyInfo;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.FragmentRoomBinding;
import io.agora.sample.rtegame.databinding.ItemRoomMessageBinding;
import io.agora.sample.rtegame.repo.GameRepo;
import io.agora.sample.rtegame.service.MediaProjectService;
import io.agora.sample.rtegame.ui.room.donate.DonateDialog;
import io.agora.sample.rtegame.ui.room.invite.HostListDialog;
import io.agora.sample.rtegame.ui.room.tool.MoreDialog;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.util.GiftUtil;
import io.agora.sample.rtegame.util.ViewStatus;
import io.agora.sample.rtegame.view.LiveHostCardView;
import io.agora.sample.rtegame.view.LiveHostLayout;

public class RoomFragment extends BaseFragment<FragmentRoomBinding> {

    private RoomViewModel mViewModel;

    private BaseRecyclerViewAdapter<ItemRoomMessageBinding, CharSequence, MessageHolder> mMessageAdapter;

    // 请求权限
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private RoomInfo currentRoom;
    private boolean aMHost;
    private boolean showInputBox = false;
    private AlertDialog currentDialog;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // During Live we limit the orientation
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        GlobalViewModel mGlobalModel = GameUtil.getViewModel(requireActivity(), GlobalViewModel.class);

        // hold current RoomInfo
        if (mGlobalModel.roomInfo.getValue() != null)
            currentRoom = mGlobalModel.roomInfo.getValue().peekContent();

        // GOTO create room
        if (currentRoom == null) {
            findNavController().navigate(R.id.action_roomFragment_to_roomCreateFragment);
        } else {
            mViewModel = GameUtil.getViewModel(this, RoomViewModel.class, new RoomViewModelFactory(requireContext(), currentRoom));

            //  See if current user is the host
            aMHost = currentRoom.getUserId().equals(GameApplication.getInstance().user.getUserId());

            initView();
            initListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mViewModel != null)
            mViewModel.handleScreenCapture(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mViewModel != null)
            mViewModel.handleScreenCapture(true);
    }

    @Override
    public void onDestroy() {
        stopScreenCaptureService();
        super.onDestroy();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void initView() {
        Glide.with(requireContext()).load(GameApplication.getInstance().user.getAvatar())
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher_round).into(mBinding.avatarHostFgRoom);
        mBinding.nameHostFgRoom.setText(currentRoom.getTempUserName());

        // config game view
        new Handler(Looper.getMainLooper()).post(() -> {
            int topMargin = (int) (mBinding.containerHostFgRoom.getBottom() + mBinding.containerHostFgRoom.getX());
            int marginBottom = mBinding.containerOverlayFgRoom.getMeasuredHeight() - mBinding.btnExitFgRoom.getTop() + ((int) BaseUtil.dp2px(12));
            int height = mBinding.recyclerViewFgRoom.getTop() - topMargin;
            mBinding.hostContainerFgRoom.initParams(!aMHost, topMargin, height, marginBottom);
        });

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
        // handle request screen record callback
        // since onActivityResult() is deprecated
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                if (mBinding.hostContainerFgRoom.webViewHostView != null) {
                    Rect rect = new Rect();
                    mBinding.hostContainerFgRoom.webViewHostView.getHitRect(rect);
                    mViewModel.startScreenCapture(result.getData(), rect);
                }
            }
        });

        // 沉浸处理
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int desiredBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom - inset.bottom;
            // 整体留白
            mBinding.containerOverlayFgRoom.setPadding(inset.left, inset.top, inset.right, inset.bottom);
            // 输入框显隐及位置偏移
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (imeVisible) {
                int desiredRecyclerViewMargin = 0;
                if (showInputBox) {
                    mBinding.inputLayoutFgRoom.setVisibility(View.VISIBLE);
                    mBinding.inputLayoutFgRoom.requestFocus();
                    desiredRecyclerViewMargin = (int) BaseUtil.dp2px(50);
                }
                GameUtil.setBottomMarginForConstraintLayoutChild(mBinding.recyclerViewFgRoom, desiredBottom + desiredRecyclerViewMargin);
            } else {
                showInputBox = false;
                mBinding.inputLayoutFgRoom.setVisibility(View.GONE);
                mBinding.inputLayoutFgRoom.clearFocus();
                GameUtil.setBottomMarginForConstraintLayoutChild(mBinding.recyclerViewFgRoom, (int) BaseUtil.dp2px(50));
            }

            GameUtil.setBottomMarginForConstraintLayoutChild(mBinding.inputLayoutFgRoom, desiredBottom);

            return WindowInsetsCompat.CONSUMED;
        });

        // 本地消息
        mBinding.editTextFgRoom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String msg = v.getText().toString().trim();
                if (!msg.isEmpty())
                    insertUserMessage(msg);
                BaseUtil.hideKeyboard(requireActivity().getWindow(), v);
                v.setText("");
            }
            return false;
        });
        // "更多"弹窗
        mBinding.btnMoreFgRoom.setOnClickListener(v -> new MoreDialog().show(getChildFragmentManager(), MoreDialog.TAG));
        // "游戏"弹窗
        mBinding.btnGameFgRoom.setOnClickListener(v -> new HostListDialog().show(getChildFragmentManager(), HostListDialog.TAG));
        // "退出游戏"按钮
        mBinding.btnExitGameFgRoom.setOnClickListener(v -> mViewModel.requestExitGame());
        // "终止连麦"按钮
        mBinding.btnExitPkFgRoom.setOnClickListener(v -> mViewModel.endPK());
        // "礼物"弹窗
        mBinding.btnDonateFgRoom.setOnClickListener(v -> new DonateDialog().show(getChildFragmentManager(), DonateDialog.TAG));
        // "退出直播间"按钮点击事件
        mBinding.btnExitFgRoom.setOnClickListener(v -> requireActivity().onBackPressed());
        // 显示键盘按钮
        mBinding.inputFgRoom.setOnClickListener(v -> {
            showInputBox = true;
            BaseUtil.showKeyboard(requireActivity().getWindow(), mBinding.editTextFgRoom);
        });
        // RTC engine 初始化监听
        mViewModel.mEngine().observe(getViewLifecycleOwner(), this::onRTCInit);
        // 连麦成功《==》主播上线
        mViewModel.subRoomInfo().observe(getViewLifecycleOwner(), this::onSubHostJoin);
        // 游戏开始
        mViewModel.gameInfo().observe(getViewLifecycleOwner(), this::onGameChanged);
        // 礼物监听
        mViewModel.gift().observe(getViewLifecycleOwner(), this::onGiftUpdated);

        // 主播，监听连麦信息
        if (aMHost) {
            mViewModel.applyInfo().observe(getViewLifecycleOwner(), this::onPKApplyInfoChanged);
        } else {
            mViewModel.localHostId().observe(getViewLifecycleOwner(), this::onLocalHostJoin);
        }

        mViewModel.viewStatus().observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Error)
                insertNewMessage(((ViewStatus.Error) viewStatus).msg);
        });
    }

    //<editor-fold desc="邀请 相关">
    private void onPKApplyInfoChanged(PKApplyInfo pkApplyInfo) {
        if (currentDialog != null) currentDialog.dismiss();
        if (pkApplyInfo == null) return;
        if (pkApplyInfo.getStatus() == PKApplyInfo.APPLYING) {
            showPKDialog(pkApplyInfo);
        } else if (pkApplyInfo.getStatus() == PKApplyInfo.REFUSED) {
            insertNewMessage("邀请已拒绝");
        }
    }

    /**
     * 仅主播调用
     */
    private void showPKDialog(PKApplyInfo pkApplyInfo) {
        if (pkApplyInfo.getRoomId().equals(currentRoom.getId())) {
            insertNewMessage("你画我猜即将开始，等待其他玩家...");
            currentDialog = new AlertDialog.Builder(requireContext()).setMessage("你画我猜即将开始，等待其他玩家...").setCancelable(false)
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        mViewModel.cancelPK(pkApplyInfo);
                        dialog.dismiss();
                    }).show();
        } else {
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
    }
    //</editor-fold>

    //<editor-fold desc="游戏相关">
    private void onGameChanged(GameInfo gameInfo) {
        BaseUtil.logD("game status->" + gameInfo.getStatus());
        if (gameInfo.getStatus() == GameInfo.IDLE) {
            if (aMHost) {
                mBinding.btnGameFgRoom.setVisibility(View.GONE);
                mBinding.btnExitGameFgRoom.setVisibility(View.VISIBLE);
                mBinding.btnExitPkFgRoom.setVisibility(View.GONE);
                needGameView(true);
                WebView webView = mBinding.hostContainerFgRoom.webViewHostView;
                if (webView != null) {
                    mViewModel.startGame(gameInfo, webView);
                }
                startScreenCaptureService();
            }
        } else if (gameInfo.getStatus() == GameInfo.PLAYING) {
            if (!aMHost) {
                insertNewMessage("加载远端画面");
                GameUtil.currentGame =  GameRepo.getGameDetail(gameInfo.getGameId());
                needGameView(true);
                if (mBinding.hostContainerFgRoom.gameHostView != null) {
                    mViewModel.setupScreenView(mBinding.hostContainerFgRoom.gameHostView.renderTextureView, gameInfo.getGameUid());
                }
            }
        } else if (gameInfo.getStatus() == GameInfo.END) {
            needGameView(false);
            if (aMHost) {
                mBinding.btnGameFgRoom.setVisibility(View.VISIBLE);
                mBinding.btnExitGameFgRoom.setVisibility(View.GONE);
                mBinding.btnExitPkFgRoom.setVisibility(View.VISIBLE);
                stopScreenCaptureService();
                mViewModel.endScreenCapture();
            }
        }
    }

    private void needGameView(boolean need) {
        if (need) {
            mBinding.hostContainerFgRoom.createDefaultGameView();
            mBinding.hostContainerFgRoom.setType(LiveHostLayout.Type.DOUBLE_IN_GAME);
        } else {
            mBinding.hostContainerFgRoom.removeGameView();
            mBinding.hostContainerFgRoom.setType(LiveHostLayout.Type.DOUBLE);
        }
    }
    //</editor-fold>


    /**
     * 主播 || 自己送的==》消息提示
     * 不是主播 ==》显示特效
     */
    private void onGiftUpdated(GiftInfo giftInfo) {
        if (giftInfo == null) return;

        mBinding.giftImageFgRoom.setVisibility(View.VISIBLE);

        String giftDesc = GiftUtil.getGiftDesc(requireContext(), giftInfo);
        if (giftDesc != null) insertNewMessage(giftDesc);

        if (!aMHost) {
            int giftId = GiftUtil.getGiftIdFromGiftInfo(requireContext(), giftInfo);
            Glide.with(requireContext()).asGif().load(GiftUtil.getGifByGiftId(giftId))
                    .listener(new RequestListener<GifDrawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                            mBinding.giftImageFgRoom.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                            resource.setLoopCount(1);
                            resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                                @Override
                                public void onAnimationEnd(Drawable drawable) {
                                    super.onAnimationEnd(drawable);
                                    mBinding.giftImageFgRoom.setVisibility(View.GONE);
                                }
                            });
                            return false;
                        }
                    })
                    .into(mBinding.giftImageFgRoom);
        }
    }

    /**
     * RTC 初始化成功
     */
    private void onRTCInit(RtcEngine engine) {
        if (engine == null) findNavController().popBackStack();
        else {
            insertNewMessage("RTC 初始化成功");
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
        insertNewMessage("画面加载完成");
    }

    /**
     * 主播上线
     */
    @MainThread
    private void onLocalHostJoin(Integer uid) {
        BaseUtil.logD("uid:" + uid);
        if (uid == null) return;
        insertNewMessage("正在加载主播【" + currentRoom.getTempUserName() + "】视频");
        LiveHostLayout liveHost = mBinding.hostContainerFgRoom;

        LiveHostCardView view = liveHost.createHostView();
        liveHost.setType(liveHost.getChildCount() == 1 ? LiveHostLayout.Type.HOST_ONLY : liveHost.getType());
        mViewModel.setupRemoteView(view.renderTextureView, currentRoom, true);
    }

    /**
     * 连麦主播上线
     */
    @MainThread
    private void onSubHostJoin(@Nullable RoomInfo subRoomInfo) {
        BaseUtil.logD("room status->" + (subRoomInfo == null));
        LiveHostLayout container = mBinding.hostContainerFgRoom;
        // remove subHostView
        if (subRoomInfo == null) {
            insertNewMessage("连麦结束");
            container.removeView(container.subHostView);
            container.setType(LiveHostLayout.Type.HOST_ONLY);
            mBinding.btnExitPkFgRoom.setVisibility(View.GONE);
        } else {
            insertNewMessage("正在加载连麦主播【" + subRoomInfo.getTempUserName() + "】视频");
            LiveHostCardView view = container.createSubHostView();
            if (container.isCurrentlyInGame())
                container.setType(LiveHostLayout.Type.DOUBLE_IN_GAME);
            else
                container.setType(LiveHostLayout.Type.DOUBLE);
            mViewModel.setupRemoteView(view.renderTextureView, subRoomInfo, false);

            if (!aMHost) return;
            if (mBinding.hostContainerFgRoom.isCurrentlyInGame())
                mBinding.btnExitPkFgRoom.setVisibility(View.GONE);
            else
                mBinding.btnExitPkFgRoom.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 用户发送消息
     * 直播间滚动消息
     */
    private void insertUserMessage(String msg) {
        Spanned userMessage = Html.fromHtml(getString(R.string.user_msg, mViewModel.localUser.getName(), msg));
        insertNewMessage(userMessage);
    }

    /**
     * 直播间滚动消息
     */
    private void insertNewMessage(CharSequence msg) {
        mMessageAdapter.addItem(msg);
        int count = mMessageAdapter.getItemCount();
        if (count > 0)
            mBinding.recyclerViewFgRoom.smoothScrollToPosition(count - 1);
    }

    private void startScreenCaptureService() {
        Intent mediaProjectionIntent = new Intent(requireActivity(), MediaProjectService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(mediaProjectionIntent);
        } else {
            requireContext().startService(mediaProjectionIntent);
        }

        MediaProjectionManager mpm = (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mpm.createScreenCaptureIntent();
        activityResultLauncher.launch(intent);
    }

    private void stopScreenCaptureService() {
        Intent mediaProjectionIntent = new Intent(requireActivity(), MediaProjectService.class);
        requireContext().stopService(mediaProjectionIntent);
    }

}
