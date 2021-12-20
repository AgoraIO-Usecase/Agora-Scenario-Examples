package io.agora.scene.rtegame.ui.room;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.TextureView;
import android.webkit.WebView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;

import io.agora.example.base.BaseUtil;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.internal.RtcEngineImpl;
import io.agora.rtc2.video.ScreenCaptureParameters;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.scene.rtegame.GlobalViewModel;
import io.agora.scene.rtegame.R;
import io.agora.scene.rtegame.bean.GameApplyInfo;
import io.agora.scene.rtegame.bean.GameInfo;
import io.agora.scene.rtegame.bean.GiftInfo;
import io.agora.scene.rtegame.bean.LocalUser;
import io.agora.scene.rtegame.bean.PKApplyInfo;
import io.agora.scene.rtegame.bean.PKInfo;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.repo.GameRepo;
import io.agora.scene.rtegame.repo.RoomApi;
import io.agora.scene.rtegame.util.Event;
import io.agora.scene.rtegame.util.GamSyncEventListener;
import io.agora.scene.rtegame.util.GameConstants;
import io.agora.scene.rtegame.util.GameUtil;
import io.agora.scene.rtegame.util.ViewStatus;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;


/**
 * @author lq
 */
@Keep
public class RoomViewModel extends ViewModel implements RoomApi {

    //<editor-fold desc="Persistent variable">
    public final RoomInfo currentRoom;
    @NonNull
    public final LocalUser localUser;
    public final boolean amHost;
    @Nullable
    public SceneReference currentSceneRef = null;
    @Nullable
    private SceneReference targetSceneRef = null;

    public boolean isLocalVideoMuted = false;
    public boolean isLocalMicMuted = false;

    private final String screenShareUId = "101024";
    private final RtcConnection screenShareConnection = new RtcConnection();

    private PKInfo pkInfo = null;
    //</editor-fold>


    //<editor-fold desc="Live data">
    // RTC engine 初始化结果
    private final MutableLiveData<RtcEngineEx> _mEngine = new MutableLiveData<>();

    @NonNull
    public LiveData<RtcEngineEx> mEngine() {
        return _mEngine;
    }

    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();

    @NonNull
    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    ///////////////////// 本房间主播 在 RTC 中的 id /////////////////////////
    private final MutableLiveData<Integer> _LocalHostId = new MutableLiveData<>();

    @NonNull
    public LiveData<Integer> localHostId() {
        return _LocalHostId;
    }

    // 直播间礼物信息
    private final MutableLiveData<Event<GiftInfo>> _gift = new MutableLiveData<>(new Event<>(null));

    @NonNull
    public LiveData<Event<GiftInfo>> gift() {
        return _gift;
    }

    // 连麦房间信息
    private final MutableLiveData<RoomInfo> _subRoomInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<RoomInfo> subRoomInfo() {
        return _subRoomInfo;
    }

    // 当前在玩游戏信息
    private final MutableLiveData<GameApplyInfo> _currentGame = new MutableLiveData<>();

    @NonNull
    public LiveData<GameApplyInfo> currentGame() {
        return _currentGame;
    }

    // 连麦房间信息
    private final MutableLiveData<GameInfo> _gameShareInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<GameInfo> gameShareInfo(){
        return _gameShareInfo;
    }

    private final MutableLiveData<PKApplyInfo> _applyInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<PKApplyInfo> applyInfo() {
        return _applyInfo;
    }

    private final MutableLiveData<Event<Boolean>> _pkResult = new MutableLiveData<>();

    @NonNull
    public LiveData<Event<Boolean>> pkResult() {
        return _pkResult;
    }
    //</editor-fold>

    //<editor-fold desc="Init and end">
    public RoomViewModel(@NonNull Context context, @NonNull RoomInfo roomInfo) {
        this.currentRoom = roomInfo;
        screenShareConnection.channelId = currentRoom.getId();
        screenShareConnection.localUid = Integer.parseInt(screenShareUId);

        localUser = GlobalViewModel.localUser == null ? GlobalViewModel.checkLocalOrGenerate(context) : GlobalViewModel.localUser;
        this.amHost = Objects.equals(currentRoom.getUserId(), localUser.getUserId());

        initRTC(context, new IRtcEngineEventHandler() {
            @Override
            public void onUserJoined(int uid, int elapsed) {
                BaseUtil.logD("onUserJoined:" + uid);
                if (String.valueOf(uid).equals(currentRoom.getUserId()))
                    RoomViewModel.this._LocalHostId.postValue(uid);
            }

        });
    }

    private void onJoinRTMSucceed(@NonNull SceneReference sceneReference) {
        BaseUtil.logD("onJoinRTMSucceed");
        currentSceneRef = sceneReference;
        _viewStatus.postValue(new ViewStatus.Error("加入RTM成功"));
        if (currentSceneRef != null) {
            subscribeAttr(currentSceneRef, currentRoom);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        GameUtil.currentGame = null;
        new Thread(() -> {
            // destroy RTE
            RtcEngine engine = _mEngine.getValue();
            if (engine != null) {
                engine.leaveChannel();
                RtcEngine.destroy();
            }
            // destroy RTM
            if (currentSceneRef != null) {
                if (amHost)
                    currentSceneRef.delete(new Sync.Callback() {
                        @Override
                        public void onSuccess() {
                            BaseUtil.logD("delete onSuccess");
                        }

                        @Override
                        public void onFail(SyncManagerException exception) {
                            BaseUtil.logD("delete onFail");
                        }
                    });
                else currentSceneRef.unsubscribe(null);
            }
            if (targetSceneRef != null) {
                currentSceneRef.unsubscribe(null);
                currentSceneRef = null;
            }
        }).start();
    }
    //</editor-fold>

    //<editor-fold desc="ScreenCapture">
    /**
     * Step 1. set parameters and startScreenCapture.
     * Step 2. joinChannelEx and publishScreenTrack on this channel
     * Step 3. update GameInfo
     */
    public void startScreenCapture(@NonNull Intent intent, @NonNull Rect rect) {
        RtcEngineEx rtcEngine = _mEngine.getValue();
        if (rtcEngine != null) {
            // public abstract int joinChannelEx(String token, RtcConnection connection, ChannelMediaOptions options, IRtcEngineEventHandler eventHandler);

            // Step 1
            ScreenCaptureParameters parameters = new ScreenCaptureParameters();
            parameters.setFrameRate(15);
            parameters.setVideoDimensions(new VideoEncoderConfiguration.VideoDimensions(rect.width(), rect.height()));
//            parameters.setCropRect(rect);
            rtcEngine.startScreenCapture(intent, parameters);

            // Step 2
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = false;
            options.autoSubscribeVideo = false;
            options.publishScreenTrack = true;
            options.publishCameraTrack = false;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            Context context = ((RtcEngineImpl) rtcEngine).getContext();
            rtcEngine.joinChannelEx(context.getString(R.string.rtc_app_token), screenShareConnection, options, new IRtcEngineEventHandler() {});

            // Step 3
            if (currentSceneRef != null && GameUtil.currentGame != null) {
                BaseUtil.logD("GAME_INFO update->"+ System.currentTimeMillis());
                GameInfo gameInfo = new GameInfo(GameInfo.START, screenShareUId, GameUtil.currentGame.getGameId());
                currentSceneRef.update(GameConstants.GAME_INFO, gameInfo, null);
            }
        }
    }

    public void endScreenCapture() {
        RtcEngineEx rtcEngine = _mEngine.getValue();
        if (rtcEngine != null) {
            rtcEngine.stopScreenCapture();
            rtcEngine.leaveChannelEx(screenShareConnection);
        }
    }

    public void handleScreenCapture(boolean needCapture) {
        BaseUtil.logD("needCapture:" + needCapture);
        RtcEngineEx rtcEngine = _mEngine.getValue();
        if (rtcEngine != null) {
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishScreenTrack = needCapture;

            rtcEngine.updateChannelMediaOptionsEx(options, screenShareConnection);
        }
    }
    //</editor-fold>

    //<editor-fold desc="SyncManager related">

    public void subscribeAttr(@NonNull SceneReference sceneRef, @NonNull RoomInfo targetRoom) {
        if (Objects.equals(targetRoom.getId(), currentRoom.getId())) {
            BaseUtil.logD("subscribe current room attr");

            sceneRef.get(GameConstants.GIFT_INFO, (GetAttrCallback) this::tryHandleGetGiftInfo);
            sceneRef.get(GameConstants.PK_INFO, (GetAttrCallback) this::tryHandlePKInfo);
            sceneRef.subscribe(GameConstants.PK_INFO, new GamSyncEventListener(GameConstants.PK_INFO, this::tryHandlePKInfo));
            sceneRef.subscribe(GameConstants.GIFT_INFO, new GamSyncEventListener(GameConstants.GIFT_INFO, this::tryHandleGiftInfo));

            if (amHost) {
                sceneRef.get(GameConstants.PK_APPLY_INFO, (GetAttrCallback) this::tryHandleApplyPKInfo);
                sceneRef.subscribe(GameConstants.PK_APPLY_INFO, new GamSyncEventListener(GameConstants.PK_APPLY_INFO, this::tryHandleApplyPKInfo));

                sceneRef.get(GameConstants.GAME_APPLY_INFO, (GetAttrCallback) RoomViewModel.this::tryHandleGameApplyInfo);
                sceneRef.subscribe(GameConstants.GAME_APPLY_INFO, new GamSyncEventListener(GameConstants.GAME_APPLY_INFO, this::tryHandleGameApplyInfo));
            }else{
                sceneRef.get(GameConstants.PK_APPLY_INFO, (GetAttrCallback) this::justFetchValue);
                sceneRef.subscribe(GameConstants.PK_APPLY_INFO, new GamSyncEventListener(GameConstants.PK_APPLY_INFO, this::justFetchValue));

                sceneRef.get(GameConstants.GAME_INFO, (GetAttrCallback) this::tryHandleGameInfo);
                sceneRef.subscribe(GameConstants.GAME_INFO, new GamSyncEventListener(GameConstants.GAME_INFO, this::tryHandleGameInfo));
            }
        } else {
            BaseUtil.logD("subscribe other room attr");
            sceneRef.subscribe(GameConstants.PK_APPLY_INFO, new GamSyncEventListener(GameConstants.PK_APPLY_INFO, this::tryHandleApplyPKInfo));
            sceneRef.subscribe(GameConstants.GAME_APPLY_INFO, new GamSyncEventListener(GameConstants.GAME_APPLY_INFO, this::tryHandleGameApplyInfo));
        }
    }

    //<editor-fold desc="Gift related">

    private void tryHandleGiftInfo(IObject item) {
        BaseUtil.logD("tryHandleGiftInfo->"+ System.currentTimeMillis());
        GiftInfo giftInfo = handleIObject(item, GiftInfo.class);
        if (giftInfo != null) {
            _gift.postValue(new Event<>(giftInfo));
        }
    }

    /**
     * 加入房间先获取当前 Gift
     */
    private void tryHandleGetGiftInfo(IObject item) {
        GiftInfo giftInfo = handleIObject(item, GiftInfo.class);
        if (giftInfo != null) {
            Event<GiftInfo> giftInfoEvent = new Event<>(giftInfo);
            giftInfoEvent.getContentIfNotHandled(); // consume this time
            _gift.postValue(giftInfoEvent);
        }
    }

    public void donateGift(@NonNull GiftInfo gift) {
        if (currentSceneRef != null && _gift.getValue() != null) {
            GiftInfo currentGift = _gift.getValue().peekContent();
            if (currentGift != null) {
                gift.setCoin(currentGift.getCoin() + gift.getCoin());
            }
            currentSceneRef.update(GameConstants.GIFT_INFO, gift, null);
        }
        // Currently in game mode, report it
        GameInfo gameInfo = _gameShareInfo.getValue();
        if (gameInfo != null && gameInfo.getStatus() == GameInfo.START){
            try {
                PKApplyInfo pkApplyInfo = _applyInfo.getValue();
                if (pkApplyInfo != null) {
                    GameRepo.sendGift(localUser, Integer.parseInt(pkApplyInfo.getTargetRoomId()), Integer.parseInt(currentRoom.getUserId()), gift);
                }else{  // 有一种情况为本地没有 PKApplyInfo 的信息，说明客户端为发起方
                    // FIXME 临时解决方法
                    RoomInfo roomInfo = _subRoomInfo.getValue();
                    if (roomInfo != null)
                        GameRepo.sendGift(localUser, Integer.parseInt(roomInfo.getId()), Integer.parseInt(currentRoom.getUserId()), gift);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="PKApplyInfo related">
    private void tryHandleApplyPKInfo(IObject item) {
        PKApplyInfo pkApplyInfo = handleIObject(item, PKApplyInfo.class);
        if (pkApplyInfo != null) {
            if (_applyInfo.getValue() == null || !Objects.equals(_applyInfo.getValue().toString(), pkApplyInfo.toString()))
                onPKApplyInfoChanged(pkApplyInfo);
        }
    }

    private void justFetchValue(IObject item){
        PKApplyInfo pkApplyInfo = handleIObject(item, PKApplyInfo.class);
        if (pkApplyInfo != null) {
            _applyInfo.postValue(pkApplyInfo);
        }
    }

    /**
     * 仅主播调用
     */
    private void onPKApplyInfoChanged(@NonNull PKApplyInfo pkApplyInfo) {
        BaseUtil.logD("onPKApplyInfoChanged:"+pkApplyInfo.toString());
        _applyInfo.postValue(pkApplyInfo);
        switch (pkApplyInfo.getStatus()) {
            case PKApplyInfo.APPLYING: {
                // 当前不在PK && 收到其他主播的游戏邀请《==》加入对方RTM频道(同时监听对方频道的属性, 支持退出游戏后可以再次邀请进入游戏。)
                if (targetSceneRef == null && Objects.equals(pkApplyInfo.getTargetRoomId(), currentRoom.getId()))
                    Sync.Instance().joinScene(pkApplyInfo.getRoomId(), new Sync.JoinSceneCallback() {
                        @Override
                        public void onSuccess(SceneReference sceneReference) {
                            if (targetSceneRef == null)
                                targetSceneRef = sceneReference;
                            targetSceneRef.subscribe(GameConstants.PK_APPLY_INFO, new GamSyncEventListener(GameConstants.PK_APPLY_INFO, RoomViewModel.this::tryHandleApplyPKInfo));
                        }

                        @Override
                        public void onFail(SyncManagerException exception) {

                        }
                    });
                break;
            }
            case PKApplyInfo.AGREED: {
                startApplyPK(pkApplyInfo);
                break;
            }
            case PKApplyInfo.REFUSED:
            case PKApplyInfo.END: {
                endPK();
                break;
            }
        }
    }

    public void acceptApplyPK(@NonNull PKApplyInfo pkApplyInfo) {
        PKApplyInfo pkApplyInfo1 = pkApplyInfo.clone();
        pkApplyInfo1.setStatus(PKApplyInfo.AGREED);
        if (currentSceneRef != null)
            currentSceneRef.update(GameConstants.PK_APPLY_INFO, pkApplyInfo1, null);
    }

    public void cancelApplyPK(@NonNull PKApplyInfo pkApplyInfo) {
        PKApplyInfo desiredPK = pkApplyInfo.clone();
        desiredPK.setStatus(PKApplyInfo.REFUSED);

        boolean startedByMe = Objects.equals(localUser.getUserId(), desiredPK.getUserId());
        if (startedByMe) {
            if (targetSceneRef != null)
                targetSceneRef.update(GameConstants.PK_APPLY_INFO, desiredPK, null);
        }else{
            if (currentSceneRef != null)
                currentSceneRef.update(GameConstants.PK_APPLY_INFO, desiredPK, null);
        }
    }

    /**
     * 仅主播调用
     * 主播开始PK，为接收方接受发起方PK请求后的调用
     * Step 1. 根据当前角色生成 PKInfo
     * Step 2. 更新频道内{@link GameConstants#PK_INFO} 参数
     * Step 3. 更新频道内{@link GameConstants#GAME_APPLY_INFO} 参数
     */
    public void startApplyPK(@NonNull PKApplyInfo pkApplyInfo) {
        PKInfo pkInfo;
        GameApplyInfo gameApplyInfo = new GameApplyInfo(GameApplyInfo.PLAYING, pkApplyInfo.getGameId());
        if (Objects.equals(localUser.getUserId(), pkApplyInfo.getUserId())) {//      客户端为发起方
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getTargetRoomId(), pkApplyInfo.getTargetUserId());
            if (targetSceneRef != null) targetSceneRef.update(GameConstants.GAME_APPLY_INFO, gameApplyInfo, null);
        } else {//      客户端为接收方,当前房间内所有人需要知道发起方的 roomId 和 UserId
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getRoomId(), pkApplyInfo.getUserId());
        }

        if (currentSceneRef != null) {
            currentSceneRef.update(GameConstants.PK_INFO, pkInfo, null);
        }
    }

    /**
     * 向其他主播(不同的频道)发送PK邀请
     * <p>
     * **RTM 限制订阅只能在加入频道的情况下发生**
     * <p>
     * 1. 加入对方频道
     * 2. 监听对方频道属性
     * 3. 往对方频道添加属性 pkApplyInfo
     *
     * @param roomViewModel We want to separate the logic with different UI, but a RoomViewModel is still needed.
     * @param targetRoom    对方的 RoomInfo
     * @param gameId        Currently only have one game. Ignore this.
     */
    public void sendApplyPKInvite(@NonNull RoomViewModel roomViewModel, @NonNull RoomInfo targetRoom, int gameId) {
        if (targetSceneRef != null)
            doSendApplyPKInvite(roomViewModel, targetSceneRef, targetRoom, gameId);
        else
            Sync.Instance().joinScene(targetRoom.getId(), new Sync.JoinSceneCallback() {
                @Override
                public void onSuccess(SceneReference sceneReference) {
                    targetSceneRef = sceneReference;
                    roomViewModel.subscribeAttr(sceneReference, targetRoom);
                    doSendApplyPKInvite(roomViewModel, targetSceneRef, targetRoom, gameId);
                }

                @Override
                public void onFail(SyncManagerException exception) {
                    _pkResult.postValue(new Event<>(false));
                }
            });
    }

    private void doSendApplyPKInvite(@NonNull RoomViewModel roomViewModel, @NonNull SceneReference sceneReference, RoomInfo targetRoom, int gameId) {
        PKApplyInfo pkApplyInfo = new PKApplyInfo(roomViewModel.currentRoom.getUserId(), targetRoom.getUserId(), localUser.getName(), PKApplyInfo.APPLYING, gameId,
                roomViewModel.currentRoom.getId(), targetRoom.getId());

        sceneReference.update(GameConstants.PK_APPLY_INFO, pkApplyInfo, new Sync.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
                BaseUtil.logD("success update:" + result.getId() + "->" + result.toString());
                _pkResult.postValue(new Event<>(true));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                _pkResult.postValue(new Event<>(false));
            }
        });
    }

    //</editor-fold>

    //<editor-fold desc="GameInfo">
    private void tryHandleGameInfo(IObject item) {
        GameInfo gameInfo = handleIObject(item, GameInfo.class);
        if (gameInfo != null) {
            if (_gameShareInfo.getValue() == null || !Objects.equals(_gameShareInfo.getValue().toString(), gameInfo.toString()))
                onGameInfoChanged(gameInfo);
        }
    }

    /**
     * 只在当前频道
     * 观众：{@link GameInfo#START} 订阅视频流 ,{@link GameInfo#END} 取消订阅
     */
    private void onGameInfoChanged(@NonNull GameInfo gameInfo) {
        BaseUtil.logD("onGameShareInfoChanged");
        _gameShareInfo.postValue(gameInfo);
    }
    //</editor-fold>

    //<editor-fold desc="GameApplyInfo">
    private void tryHandleGameApplyInfo(IObject item) {
        BaseUtil.logD("tryHandleGameApplyInfo->"+item.toString());
        GameApplyInfo currentGame = handleIObject(item, GameApplyInfo.class);
        if (currentGame != null) {
            if (_currentGame.getValue() == null || !Objects.equals(_currentGame.getValue().toString(), currentGame.toString()))
                onGameApplyInfoChanged(currentGame);
        }
    }

    private void onGameApplyInfoChanged(@NonNull GameApplyInfo currentGame) {
        BaseUtil.logD("onGameApplyInfoChanged:"+currentGame.toString());
        _currentGame.postValue(currentGame);
        if (currentGame.getStatus() == GameInfo.END) {
            exitGame();
            if (currentSceneRef != null)
                currentSceneRef.update(GameConstants.GAME_INFO, new GameInfo(GameInfo.END, screenShareUId, currentGame.getGameId()), null);
        }
    }

    /**
     * 只更新监听的{@link GameConstants#GAME_APPLY_INFO} 字段
     */
    public void requestExitGame() {
        GameApplyInfo gameApplyInfo = _currentGame.getValue();
        if (gameApplyInfo == null) return;

        GameApplyInfo desiredGameApplyInfo = new GameApplyInfo(GameApplyInfo.END, gameApplyInfo.getGameId());

        PKApplyInfo pkApplyInfo = _applyInfo.getValue();

        if (pkApplyInfo != null) {
            boolean startedByMe = Objects.equals(localUser.getUserId(), pkApplyInfo.getUserId());
            if (!startedByMe) {
                if (currentSceneRef != null)
                    currentSceneRef.update(GameConstants.GAME_APPLY_INFO, desiredGameApplyInfo, null);
            }else{
                if (targetSceneRef != null)
                    targetSceneRef.update(GameConstants.GAME_APPLY_INFO, desiredGameApplyInfo, null);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="PKInfo">
    private void tryHandlePKInfo(IObject item) {
        PKInfo pkInfo = handleIObject(item, PKInfo.class);
        if (pkInfo != null) {
            if (this.pkInfo == null || !Objects.equals(pkInfo.toString(), this.pkInfo.toString()))
                onPKInfoChanged(pkInfo);
        }
    }
    /**
     * {@link PKInfo#AGREED} 加入对方频道，拉流 | {@link PKInfo#END} 退出频道
     */
    private void onPKInfoChanged(@NonNull PKInfo pkInfo) {
        BaseUtil.logD("onPKInfoChanged:"+pkInfo.toString());
        this.pkInfo = pkInfo;
        if (pkInfo.getStatus() == PKInfo.AGREED) {
            // 只用来加入频道，只使用 roomId 字段
            // this variable will only for join channel so room name doesn't matter.
            RoomInfo subRoom = new RoomInfo(pkInfo.getRoomId(), "", pkInfo.getUserId());
            joinSubRoom(subRoom);
        } else if (pkInfo.getStatus() == PKInfo.END) {
            leaveSubRoom();
        }
    }

    /**
     * 仅主播调用
     */
    public void endPK() {
        PKInfo pkInfo = new PKInfo(PKInfo.END, "", "");
        PKApplyInfo pkApplyInfo = applyInfo().getValue();
        boolean startedByMe = false;
        if (pkApplyInfo != null) {
            pkApplyInfo = pkApplyInfo.clone();
            pkApplyInfo.setStatus(PKApplyInfo.END);
            startedByMe = Objects.equals(localUser.getUserId(), pkApplyInfo.getUserId());
        }
        if (currentSceneRef != null) {
            currentSceneRef.update(GameConstants.PK_INFO, pkInfo, null);
                if (pkApplyInfo != null && !startedByMe) {
                    currentSceneRef.update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
                }
        }
        if (targetSceneRef != null) {
            if (pkApplyInfo != null && startedByMe)
                targetSceneRef.update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
            BaseUtil.logD("targetSceneRef unsubscribe");
            targetSceneRef.unsubscribe(null);
            targetSceneRef = null;
        }
    }

    //</editor-fold>

    //<editor-fold desc="YuanQi Game">
    public void startGame(@NonNull GameApplyInfo currentGame, @NonNull WebView webView) {
        PKApplyInfo pkApplyInfo = _applyInfo.getValue();
        if (pkApplyInfo == null) return;
        boolean isOrganizer = Objects.equals(_applyInfo.getValue().getRoomId(), currentRoom.getId());
        GameRepo.gameStart(webView, localUser, isOrganizer, Integer.parseInt(pkApplyInfo.getTargetRoomId()));
    }
    /**
     * 监听到修改成功，退出游戏
     */
    public void exitGame() {
        if (GameUtil.currentGame == null) return;
        PKApplyInfo applyInfo = _applyInfo.getValue();
        if (applyInfo != null) {
            GameRepo.endThisGame(Integer.parseInt(applyInfo.getTargetRoomId()));
        }
    }
    //</editor-fold>

    @Nullable
    private <T> T handleIObject(IObject obj, Class<T> clazz) {
        T res = null;
        try {
            res = obj.toObject(clazz);
        } catch (Exception e) {
            e.printStackTrace();
            BaseUtil.logD(e.getMessage());
        }
        return res;
    }

    //</editor-fold>

    //<editor-fold desc="RTC related">
    public void muteLocalVideoStream(boolean mute) {
        isLocalVideoMuted = mute;
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.muteLocalVideoStream(isLocalVideoMuted);
        }
    }

    public void muteLocalAudioStream(boolean mute) {
        isLocalMicMuted = mute;
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.muteLocalAudioStream(isLocalMicMuted);
        }
    }

    public void flipCamera() {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.switchCamera();
        }
    }

    /**
     * 加入当前房间
     */
    @Override
    public void joinRoom(@NonNull LocalUser localUser) {
        new Thread(() -> {
            RtcEngineEx engine = _mEngine.getValue();
            if (engine != null) {
                ChannelMediaOptions options = new ChannelMediaOptions();
                options.autoSubscribeAudio = true;
                options.autoSubscribeVideo = true;
                if (amHost) {
                    engine.enableAudio();
                    engine.enableVideo();
                    engine.startPreview();
                    options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                } else {
                    options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
                }

                engine.joinChannel(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), currentRoom.getId(), Integer.parseInt(localUser.getUserId()), options);
            }
        }).start();
    }

    /**
     * 加入其他主播房间前先退出现在房间
     * 加入成功监听到对方主播上线《==》UI更新
     */
    @Override
    public void joinSubRoom(@NonNull RoomInfo subRoomInfo) {
        leaveSubRoom();

        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            RtcConnection connection = new RtcConnection();
            connection.channelId = subRoomInfo.getId();
            connection.localUid = -Integer.parseInt(localUser.getUserId());

            ChannelMediaOptions options = new ChannelMediaOptions();
//            public abstract int joinChannelEx(String token, RtcConnection connection, ChannelMediaOptions options, IRtcEngineEventHandler eventHandler);
            engine.joinChannelEx(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), connection, options, new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    BaseUtil.logD("onJoinChannelSuccess:"+channel+uid);
                    _subRoomInfo.postValue(subRoomInfo);
                }
            });
        }
    }

    public void leaveSubRoom() {
        RoomInfo tempRoom = _subRoomInfo.getValue();
        if (tempRoom == null) return;
        String roomId = tempRoom.getId();
        BaseUtil.logD("leaveSubRoom:"+roomId);
        _subRoomInfo.postValue(null);
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            RtcConnection connection = new RtcConnection();
            connection.channelId = roomId;
            connection.localUid = -Integer.parseInt(localUser.getUserId());
            engine.leaveChannelEx(connection);
        }
    }

    public void initRTC(@NonNull Context mContext, @NonNull IRtcEngineEventHandler mEventHandler) {
        new Thread(() -> {
            String appID = mContext.getString(R.string.rtc_app_id);
            if (appID.isEmpty() || appID.codePointCount(0, appID.length()) != 32) {
                _viewStatus.postValue(new ViewStatus.Error("APP ID is not valid"));
                _mEngine.postValue(null);
            } else {
                RtcEngineConfig config = new RtcEngineConfig();
                config.mContext = mContext;
                config.mAppId = appID;
                config.mEventHandler = mEventHandler;
                RtcEngineConfig.LogConfig logConfig = new RtcEngineConfig.LogConfig();
                logConfig.filePath = mContext.getExternalCacheDir().getAbsolutePath();
                config.mLogConfig = logConfig;
                config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

                try {
                    _mEngine.postValue((RtcEngineEx) RtcEngineEx.create(config));
                } catch (Exception e) {
                    e.printStackTrace();
                    _viewStatus.postValue(new ViewStatus.Error(e));
                    _mEngine.postValue(null);
                }

                // 监听当前房间数据 <==> 礼物、PK、
                Sync.Instance().joinScene(currentRoom.getId(), new Sync.JoinSceneCallback() {
                    @Override
                    public void onSuccess(SceneReference sceneReference) {
                        onJoinRTMSucceed(sceneReference);
                    }

                    @Override
                    public void onFail(SyncManagerException e) {
                        _viewStatus.postValue(new ViewStatus.Error("加入RTM失败"));
                    }
                });
            }
        }).start();
    }

    public void setupLocalView(@NonNull TextureView view, @NonNull LocalUser localUser) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.setupLocalVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(localUser.getUserId())));
        }
    }

    /**
     * @param view        用来构造 videoCanvas
     * @param roomInfo    isLocalHost => current RoomInfo，!isLocalHost => 对方的 RoomInfo
     * @param isLocalHost 是否是当前房间主播
     */
    public void setupRemoteView(@NonNull TextureView view, @NonNull RoomInfo roomInfo, boolean isLocalHost) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.getUserId()));
            if (isLocalHost) {
                engine.setupRemoteVideo(videoCanvas);
            } else {
                RtcConnection connection = new RtcConnection();
                connection.channelId = roomInfo.getId();
                connection.localUid = -Integer.parseInt(localUser.getUserId());
                engine.setupRemoteVideoEx(videoCanvas, connection);
            }
        }
    }

    /**
     * 设置屏幕共享视图
     * 只有观众调用
     */
    public void setupScreenView(@NonNull TextureView view, int gameUid) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_FIT, gameUid);
            engine.setupRemoteVideo(videoCanvas);
        }
    }
    //</editor-fold>

    private interface GetAttrCallback extends Sync.DataItemCallback {
        @Override
        default void onFail(SyncManagerException exception) {

        }
    }
}
