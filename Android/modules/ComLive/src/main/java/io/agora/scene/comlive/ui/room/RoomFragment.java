package io.agora.scene.comlive.ui.room;

import static android.view.View.GONE;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.base.BaseNavFragment;
import io.agora.scene.comlive.bean.AgoraGame;
import io.agora.scene.comlive.bean.GameInfo;
import io.agora.scene.comlive.bean.GiftInfo;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.databinding.ComLiveFragmentRoomBinding;
import io.agora.scene.comlive.databinding.ComLiveItemRoomMessageBinding;
import io.agora.scene.comlive.ui.room.donate.DonateDialog;
import io.agora.scene.comlive.ui.room.game.GameListDialog;
import io.agora.scene.comlive.ui.room.tool.MoreDialog;
import io.agora.scene.comlive.util.BlurTransformation;
import io.agora.scene.comlive.util.ComLiveUtil;
import io.agora.scene.comlive.util.EventObserver;
import io.agora.scene.comlive.util.GiftUtil;
import io.agora.scene.comlive.util.ViewStatus;
import io.agora.scene.comlive.view.LiveHostCardView;
import io.agora.scene.comlive.view.LiveHostLayout;

public class RoomFragment extends BaseNavFragment<ComLiveFragmentRoomBinding> {

    private RoomViewModel mViewModel;

    private BaseRecyclerViewAdapter<ComLiveItemRoomMessageBinding, MessageHolder.LiveMessage, MessageHolder> mMessageAdapter;

    private RoomInfo currentRoom;
    private boolean aMHost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (GlobalViewModel.localUser == null) return null;

        GlobalViewModel mGlobalModel = ComLiveUtil.getAndroidViewModel(this, GlobalViewModel.class);
        // hold current RoomInfo
        if (mGlobalModel.roomInfo.getValue() != null)
            currentRoom = mGlobalModel.roomInfo.getValue().peekContent();
        if (currentRoom == null) {
            findNavController().navigate(R.id.action_roomFragment_to_roomCreateFragment);
            return null;
        }
        mViewModel = ComLiveUtil.getViewModel(this, RoomViewModel.class, new RoomViewModelFactory(requireContext(), currentRoom));
        aMHost = currentRoom.getUserId().equals(mViewModel.localUser.getUserId());
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setRoomBgd();
        initView();
        initListener();
        initObserver();
    }

    private void initView() {
        Glide.with(this).load(mViewModel.localUser.getAvatar())
                .centerCrop()
                .placeholder(R.mipmap.com_live_ic_launcher_round).into(mBinding.layoutRoomInfo.avatarHostFgRoom);
        mBinding.layoutRoomInfo.nameHostFgRoom.setText(currentRoom.getTempUserName());

        // config game view
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mBinding != null) {
                // GameView = WebView || 屏幕共享View
                int dp12 = (int) BaseUtil.dp2px(12);
                // 设置 Game View 距离顶部距离 (+ 12dp)
                int topMargin = (int) (mBinding.layoutRoomInfo.getRoot().getBottom() + dp12);
                // Game Mode 下摄像头预览距离底部距离
                int marginBottom = mBinding.getRoot().getMeasuredHeight() - mBinding.btnExitFgRoom.getTop() + dp12;
                // Game View 高度
                int height = mBinding.btnExitFgRoom.getTop() - mBinding.recyclerViewFgRoom.getMeasuredHeight() - dp12 * 2 - topMargin;
                mBinding.hostContainerFgRoom.initParams(false, topMargin, height, marginBottom, mBinding.recyclerViewFgRoom.getPaddingLeft());
            }
        });

        mMessageAdapter = new BaseRecyclerViewAdapter<>(null, MessageHolder.class);
        mBinding.recyclerViewFgRoom.setAdapter(mMessageAdapter);
        // Android 12 over_scroll animation is phenomenon
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            mBinding.recyclerViewFgRoom.setOverScrollMode(View.OVER_SCROLL_NEVER);

        hideBtnByCurrentRole();
    }

    private void initListener() {
        handleWindowInset();

        // 用户输入弹幕时监听会车
        mBinding.editTextFgRoom.setOnEditorActionListener(this::inputActionChanged);
        // "更多"弹窗
        mBinding.btnMoreFgRoom.setOnClickListener(v -> new MoreDialog().show(getChildFragmentManager(), MoreDialog.TAG));
        // 游戏弹窗
        mBinding.btnGameFgRoom.setOnClickListener(v -> new GameListDialog().show(getChildFragmentManager(), GameListDialog.TAG));
        // "退出游戏"按钮
        mBinding.btnExitGameFgRoom.setOnClickListener(v -> mViewModel.requestExitGame());
        // "礼物"弹窗
        mBinding.btnDonateFgRoom.setOnClickListener(v -> new DonateDialog().show(getChildFragmentManager(), DonateDialog.TAG));
        // "退出直播间"按钮点击事件
        mBinding.btnExitFgRoom.setOnClickListener(v -> requireActivity().onBackPressed());
        mBinding.editTextFgRoom.setShowSoftInputOnFocus(true);
    }

    private void initObserver() {
        // RTC engine 初始化监听
        mViewModel.rtcEngine.observe(getViewLifecycleOwner(), this::onRTCInit);
        // 礼物监听
        mViewModel.gift.observe(getViewLifecycleOwner(), new EventObserver<>(this::onGiftUpdated));
        // 房间内游戏监听
        mViewModel.gameInfo.observe(getViewLifecycleOwner(), this::onGameInfoChanged);
        // 当前游戏状态监听
        mViewModel.currentGame.observe(getViewLifecycleOwner(), this::onCurrentGameChanged);
        mViewModel.viewStatus.observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Message)
                insertNewMessage(((ViewStatus.Message) viewStatus).msg);
            else if (viewStatus instanceof ViewStatus.Error) {
                BaseUtil.toast(requireContext(), ((ViewStatus.Error) viewStatus).msg);
                findNavController().popBackStack();
            }
        });
        mViewModel.gameStartUrl.observe(getViewLifecycleOwner(), stringEvent -> {
            WebView webView = mBinding.hostContainerFgRoom.webViewHostView;
            if (webView != null)
                webView.loadUrl(stringEvent.getContentIfNotHandled());
        });
    }

    // 用户输入弹幕时监听会车 => 响应操作
    private boolean inputActionChanged(android.widget.TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            String msg = v.getText().toString().trim();
            if (!msg.isEmpty())
                insertUserMessage(msg);
            BaseUtil.hideKeyboardCompat(v);
            v.setText("");
        }
        return false;
    }

    private void hideBtnByCurrentRole() {
        mBinding.btnMoreFgRoom.setVisibility(aMHost ? View.VISIBLE : GONE);
        mBinding.btnGameFgRoom.setVisibility(aMHost ? View.VISIBLE : GONE);
        mBinding.btnExitGameFgRoom.setVisibility(aMHost ? View.VISIBLE : GONE);
    }

    //<editor-fold desc="游戏相关">
    private void onGameInfoChanged(GameInfo gameInfo) {
        if (gameInfo.getStatus() == GameInfo.END) {
            mViewModel.exitGame();
        }
    }

    /**
     * 游戏开始 或者 停止
     * UI 改变
     */
    private void onCurrentGameChanged(AgoraGame game) {
        if (game != null) {
            needGameView(true);
            WebView webView = mBinding.hostContainerFgRoom.webViewHostView;
            if (webView != null) {
                mViewModel.startGame(game.getGameId());
            }
            if (aMHost) {
                mBinding.btnGameFgRoom.setVisibility(GONE);
                mBinding.btnExitGameFgRoom.setVisibility(View.VISIBLE);
            }
        } else {
            needGameView(false);
            if (aMHost) {
                mBinding.btnGameFgRoom.setVisibility(View.VISIBLE);
                mBinding.btnExitGameFgRoom.setVisibility(GONE);
            }
        }
    }

    private void needGameView(boolean need) {
        if (need) {
            mBinding.hostContainerFgRoom.createDefaultGameView();
            WebView webViewHostView = mBinding.hostContainerFgRoom.webViewHostView;
            if (webViewHostView != null)
                webViewHostView.addJavascriptInterface(new AgoraJsBridge(mViewModel), "agoraJSBridge");

            onLayoutTypeChanged(LiveHostLayout.Type.HOST_IN_GAME);
        } else {
            WebView webViewHostView = mBinding.hostContainerFgRoom.webViewHostView;
            if (webViewHostView != null) webViewHostView.destroy();
            mBinding.hostContainerFgRoom.removeGameView();
            onLayoutTypeChanged(LiveHostLayout.Type.HOST_ONLY);
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
            Glide.with(this).asGif().load(GiftUtil.getGifByGiftId(giftId))
                    .listener(new RequestListener<GifDrawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                            mBinding.giftImageFgRoom.setVisibility(GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                            resource.setLoopCount(1);
                            resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                                @Override
                                public void onAnimationEnd(Drawable drawable) {
                                    super.onAnimationEnd(drawable);
                                    mBinding.giftImageFgRoom.setVisibility(GONE);
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
            mViewModel.joinRoom(engine);

            if (aMHost) initLocalView(); // 如果是房主，创建View开始直播
            else initRemoteView(); // 如果是观众，加载主播画面
        }
    }

    /**
     * 本地 View 初始化
     * 仅主播本人调用
     */
    @MainThread
    private void initLocalView() {
        LiveHostCardView view = mBinding.hostContainerFgRoom.createHostView();
        onLayoutTypeChanged(LiveHostLayout.Type.HOST_ONLY);
        mViewModel.setupLocalView(view.renderTextureView);
        insertNewMessage("加载画面");
    }

    /**
     * 主播上线
     */
    @MainThread
    private void initRemoteView() {
        insertNewMessage("加载主播【" + currentRoom.getTempUserName() + "】视频流");
        LiveHostLayout liveHost = mBinding.hostContainerFgRoom;
        LiveHostCardView view = liveHost.createHostView();
        onLayoutTypeChanged(liveHost.getChildCount() == 1 ? LiveHostLayout.Type.HOST_ONLY : liveHost.getType());
        mViewModel.setupRemoteView(view.renderTextureView);
    }

    /**
     * 用户发送消息
     * 直播间滚动消息
     */
    private void insertUserMessage(String msg) {
        Spanned userMessage = Html.fromHtml(getString(R.string.com_live_user_msg, mViewModel.localUser.getName(), msg));
        insertNewMessage(userMessage);
    }

    /**
     * 公告消息
     */
    private void insertAlertMessage() {

    }

    private void insertNewMessage(CharSequence msg) {
        mMessageAdapter.addItem(new MessageHolder.LiveMessage(0, msg));
        int count = mMessageAdapter.getItemCount();
        if (count > 0)
            mBinding.recyclerViewFgRoom.smoothScrollToPosition(count - 1);
    }

    private void insertNewMessage(MessageHolder.LiveMessage msg) {
        mMessageAdapter.addItem(msg);
        int count = mMessageAdapter.getItemCount();
        if (count > 0)
            mBinding.recyclerViewFgRoom.smoothScrollToPosition(count - 1);
    }

    private void onLayoutTypeChanged(LiveHostLayout.Type type) {
        BaseUtil.logD("onLayoutTypeChanged:" + type.ordinal());
        mBinding.hostContainerFgRoom.setType(type);

        if (aMHost) {
            if (type == LiveHostLayout.Type.HOST_ONLY) {
                mBinding.btnExitGameFgRoom.setVisibility(GONE);
            } else {
                mBinding.btnExitGameFgRoom.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setRoomBgd() {
        Glide.with(this).asDrawable().load(ComLiveUtil.getBgdByRoomBgdId(currentRoom.getId()))
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(requireContext()))).into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                mBinding.getRoot().setBackground(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        });

    }

    private void handleWindowInset() {
        // 沉浸处理
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 整体留白
            mBinding.containerOverlayFgRoom.setPadding(inset.left, inset.top, inset.right, inset.bottom);
            // 输入框显隐及位置偏移
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (!imeVisible)
                mBinding.editTextFgRoom.clearFocus();
            mBinding.editTextFgRoom.setText("");
//            if (imeVisible) {
//                mBinding.inputLayoutFgRoom.setVisibility(View.VISIBLE);
//                mBinding.inputLayoutFgRoom.requestFocus();
//                int desiredY = -insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
//                mBinding.inputLayoutFgRoom.setTranslationY(desiredY);
//            } else {
//                mBinding.inputLayoutFgRoom.setVisibility(GONE);
//                mBinding.inputLayoutFgRoom.clearFocus();
//            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

}
