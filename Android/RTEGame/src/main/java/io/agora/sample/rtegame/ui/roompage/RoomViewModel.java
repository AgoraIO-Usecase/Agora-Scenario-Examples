package io.agora.sample.rtegame.ui.roompage;

import android.content.Context;
import android.view.TextureView;
import android.webkit.WebView;

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
import io.agora.rtc2.video.VideoCanvas;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.GameInfo;
import io.agora.sample.rtegame.bean.GiftInfo;
import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.PKApplyInfo;
import io.agora.sample.rtegame.bean.PKInfo;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.GameRepo;
import io.agora.sample.rtegame.repo.RoomApi;
import io.agora.sample.rtegame.util.Event;
import io.agora.sample.rtegame.util.GamSyncEventListener;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.util.ViewStatus;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;


/**
 * @author lq
 */
public class RoomViewModel extends ViewModel implements RoomApi {

    //<editor-fold desc="Persistent variable">
    public final RoomInfo currentRoom;
    public final boolean amHost;
    @Nullable
    public SceneReference currentSceneRef = null;
    @Nullable
    private SceneReference targetSceneRef = null;

    public boolean isLocalVideoMuted = false;
    public boolean isLocalMicMuted = false;
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
    private final MutableLiveData<GiftInfo> _gift = new MutableLiveData<>();

    @NonNull
    public LiveData<GiftInfo> gift() {
        return _gift;
    }

    // 连麦房间信息
    private final MutableLiveData<RoomInfo> _subRoomInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<RoomInfo> subRoomInfo() {
        return _subRoomInfo;
    }

    // 连麦房间信息
    private final MutableLiveData<GameInfo> _gameInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<GameInfo> gameInfo() {
        return _gameInfo;
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
    public RoomViewModel(@NonNull Context context, @NonNull RoomInfo roomInfo, @Nullable SceneReference sceneReference) {
        this.currentRoom = roomInfo;
        this.amHost = Objects.equals(currentRoom.getUserId(), GameApplication.getInstance().user.getUserId());

        initRTC(context, new IRtcEngineEventHandler() {

            @Override
            public void onUserJoined(int uid, int elapsed) {
                if (String.valueOf(uid).equals(currentRoom.getUserId()))
                    RoomViewModel.this._LocalHostId.postValue(uid);
            }
        });
        if (sceneReference == null)
            // 监听当前房间数据 <==> 礼物、PK、
            Sync.Instance().joinScene(GameUtil.getSceneFromRoomInfo(currentRoom), new Sync.JoinSceneCallback() {
                @Override
                public void onSuccess(SceneReference sceneReference) {
                    onJoinRTMSucceed(sceneReference);
                }

                @Override
                public void onFail(SyncManagerException e) {
                    _viewStatus.postValue(new ViewStatus.Error("加入RTM失败"));
                }
            });
        else onJoinRTMSucceed(sceneReference);
    }

    private void onJoinRTMSucceed(@NonNull SceneReference sceneReference) {
        currentSceneRef = sceneReference;
        _viewStatus.postValue(new ViewStatus.Error("加入RTM成功"));
        if (currentSceneRef != null) {
            subscribeAttr(currentSceneRef, currentRoom);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
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

    //<editor-fold desc="SyncManager related">

    public void subscribeAttr(@NonNull SceneReference sceneRef, @NonNull RoomInfo targetRoom) {
        if (Objects.equals(targetRoom.getId(), currentRoom.getId())) {
            BaseUtil.logD("subscribe current room attr");
            sceneRef.subscribe(GameConstants.GIFT_INFO, new GamSyncEventListener(GameConstants.GIFT_INFO, this::tryHandleGiftInfo));
            sceneRef.subscribe(GameConstants.PK_INFO, new GamSyncEventListener(GameConstants.PK_INFO, this::tryHandlePKInfo));
            sceneRef.subscribe(GameConstants.GAME_INFO, new GamSyncEventListener(GameConstants.GAME_INFO, this::tryHandleGameInfo));
            if (amHost)
                sceneRef.subscribe(GameConstants.PK_APPLY_INFO, new GamSyncEventListener(GameConstants.PK_APPLY_INFO, this::tryHandleApplyPKInfo));
        } else {
            BaseUtil.logD("subscribe other room attr");
            sceneRef.subscribe(GameConstants.PK_APPLY_INFO, new GamSyncEventListener(GameConstants.PK_APPLY_INFO, this::tryHandleApplyPKInfo));
        }
    }

    //<editor-fold desc="Gift related">

    public void donateGift(@NonNull GiftInfo gift) {
        if (currentSceneRef != null)
            currentSceneRef.update(GameConstants.GIFT_INFO, gift, null);
    }


    private void tryHandleGiftInfo(IObject item) {
        GiftInfo giftInfo = handleIObject(item, GiftInfo.class);
        if (giftInfo != null) {
            _gift.postValue(giftInfo);
        }
    }
    //</editor-fold>

    //<editor-fold desc="PKApplyInfo related">
    private void tryHandleApplyPKInfo(IObject item) {
        PKApplyInfo pkApplyInfo = handleIObject(item, PKApplyInfo.class);
        if (pkApplyInfo != null) {
            onPKApplyInfoChanged(pkApplyInfo);
        }
    }


    /**
     * 仅主播调用
     */
    private void onPKApplyInfoChanged(@NonNull PKApplyInfo pkApplyInfo) {
        _applyInfo.postValue(pkApplyInfo);
        switch (pkApplyInfo.getStatus()) {
            case PKApplyInfo.APPLYING:{
                if (targetSceneRef == null && Objects.equals(pkApplyInfo.getTargetRoomId(), currentRoom.getId()))
                    Sync.Instance().joinScene(GameUtil.getSceneFromRoomInfo(new RoomInfo(pkApplyInfo.getRoomId(), "", pkApplyInfo.getUserId())), new Sync.JoinSceneCallback() {
                        @Override
                        public void onSuccess(SceneReference sceneReference) {
                            if (targetSceneRef == null)
                                targetSceneRef = sceneReference;
                        }

                        @Override
                        public void onFail(SyncManagerException exception) {

                        }
                    });
                break;
            }
            case PKApplyInfo.AGREED: {
                startPK(pkApplyInfo);
                break;
            }
            case PKApplyInfo.REFUSED:
            case PKApplyInfo.END: {
                if (targetSceneRef != null) targetSceneRef.unsubscribe(null);
                break;
            }
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
    public void sendPKInvite(@NonNull RoomViewModel roomViewModel, @NonNull RoomInfo targetRoom, int gameId) {
        if (targetSceneRef != null)
            doSendPKInvite(roomViewModel, targetSceneRef, targetRoom, gameId);
        else
            Sync.Instance().joinScene(GameUtil.getSceneFromRoomInfo(targetRoom), new Sync.JoinSceneCallback() {
                @Override
                public void onSuccess(SceneReference sceneReference) {
                    targetSceneRef = sceneReference;
                    roomViewModel.subscribeAttr(sceneReference, targetRoom);
                    doSendPKInvite(roomViewModel, targetSceneRef, targetRoom, gameId);
                }

                @Override
                public void onFail(SyncManagerException exception) {
                    _pkResult.postValue(new Event<>(false));
                }
            });
    }

    private void doSendPKInvite(@NonNull RoomViewModel roomViewModel, @NonNull SceneReference sceneReference, RoomInfo targetRoom, int gameId) {
        PKApplyInfo pkApplyInfo = new PKApplyInfo(roomViewModel.currentRoom.getUserId(), targetRoom.getUserId(), GameApplication.getInstance().user.getName(), PKApplyInfo.APPLYING, gameId,
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

    private void tryHandleGameInfo(IObject item) {
        GameInfo gameInfo = handleIObject(item, GameInfo.class);
        if (gameInfo != null)
            onGameChanged(gameInfo);
    }

    private void tryHandlePKInfo(IObject item) {
        PKInfo pkInfo = handleIObject(item, PKInfo.class);
        if (pkInfo != null) onPKInfoChanged(pkInfo);
//            _pkInfo.postValue(pkInfo);
    }

    @Nullable
    private <T> T handleIObject(IObject obj, Class<T> clazz) {
        T res = null;
        try {
            res = obj.toObject(clazz);
        } catch (Exception ignored) {
        }
        return res;
    }

    /**
     * 只在当前频道
     * 观众：{@link GameInfo#PLAYING} 订阅视频流 ,{@link GameInfo#END} 取消订阅
     * 主播：@{@link GameInfo#IDLE} 加载WebView， {@link GameInfo#END} 卸载 WebView
     */
    private void onGameChanged(@NonNull GameInfo gameInfo) {
        _gameInfo.postValue(gameInfo);
        if (gameInfo.getStatus() == GameInfo.END){
            exitGame();
        }
    }

    /**
     * {@link PKInfo#AGREED} 加入对方频道，拉流 | {@link PKInfo#END} 退出频道
     */
    private void onPKInfoChanged(@NonNull PKInfo pkInfo) {
        BaseUtil.logD("onPKInfoChanged:"+pkInfo.getStatus());
        if (pkInfo.getStatus() == PKInfo.AGREED) {
            // 只用来加入频道，只使用 roomId 字段
            // this variable will only for join channel so room name doesn't matter.
            RoomInfo subRoom = new RoomInfo(pkInfo.getRoomId(), "", pkInfo.getUserId());
            joinSubRoom(subRoom, GameApplication.getInstance().user);
        } else if (pkInfo.getStatus() == PKInfo.END) {
            leaveSubRoom(pkInfo.getRoomId());
        }
    }

    public void acceptPK(@NonNull PKApplyInfo pkApplyInfo) {
        pkApplyInfo.setStatus(PKApplyInfo.AGREED);
        if (currentSceneRef != null)
            currentSceneRef.update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
    }

    public void cancelPK(@NonNull PKApplyInfo pkApplyInfo) {
//        _pkApplyInfo.postValue(null);
        pkApplyInfo.setStatus(PKApplyInfo.REFUSED);
        if (currentSceneRef != null)
            currentSceneRef.update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
    }

    public void startGame(@NonNull GameInfo gameInfo, @NonNull WebView webView) {
        PKApplyInfo pkApplyInfo = _applyInfo.getValue();
        if (pkApplyInfo == null) return;
        GameUtil.currentGame = GameRepo.getGameDetail(gameInfo.getGameId());
        boolean isOrganizer = Objects.equals(_applyInfo.getValue().getRoomId(), currentRoom.getId());
        GameRepo.gameStart(webView, GameApplication.getInstance().user, isOrganizer, Integer.parseInt(pkApplyInfo.getRoomId()));
    }

    public void requestExitGame(){
        GameInfo gameInfo = new GameInfo(GameInfo.END, 0, 0);
        if (currentSceneRef != null) currentSceneRef.update(GameConstants.GAME_INFO, gameInfo, null);
        if (targetSceneRef != null) targetSceneRef.update(GameConstants.GAME_INFO, gameInfo, null);
    }

    public void exitGame() {
        if (GameUtil.currentGame == null) return;
        PKApplyInfo applyInfo = _applyInfo.getValue();
        if (applyInfo != null) {
            GameRepo.endThisGame(Integer.parseInt(applyInfo.getRoomId()));
        }
    }

    /**
     * 仅主播调用
     * 主播开始PK，为接收方接受发起方PK请求后的调用
     * Step 1. 根据当前角色生成 PKInfo
     * Step 2. 更新频道内{@link GameConstants#PK_INFO} 参数
     */
    public void startPK(@NonNull PKApplyInfo pkApplyInfo) {
        PKInfo pkInfo;
        if (pkApplyInfo.getRoomId().equals(currentRoom.getId())) {//      客户端为发起方,当前房间内所有人需要知道对方的 roomId 和 UserId
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getTargetRoomId(), pkApplyInfo.getTargetUserId());
        } else {//      客户端为接收方,当前房间内所有人需要知道发起方的 roomId 和 UserId
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getRoomId(), pkApplyInfo.getUserId());
        }
        GameInfo gameInfo = new GameInfo(GameInfo.IDLE, pkApplyInfo.getGameId(), 0);
        if (currentSceneRef != null) {
            currentSceneRef.update(GameConstants.PK_INFO, pkInfo, null);
            currentSceneRef.update(GameConstants.GAME_INFO, gameInfo, null);
        }
    }

    /**
     * 仅主播调用
     */
    public void endPK() {
        PKInfo pkInfo = new PKInfo(PKInfo.END, "", "");
        if (currentSceneRef != null) currentSceneRef.update(GameConstants.PK_INFO, pkInfo, null);
        if (targetSceneRef != null) targetSceneRef.update(GameConstants.PK_INFO, pkInfo, null);
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
//            joinChannel(String token, String channelName, String optionalInfo, int optionalUid);
//            engine.joinChannel(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), currentRoom.getId(), null, Integer.parseInt(localUser.getUserId()));
        }
    }

    /**
     * 加入其他主播房间前先退出现在房间
     * 加入成功监听到对方主播上线《==》UI更新
     */
    @Override
    public void joinSubRoom(@NonNull RoomInfo subRoomInfo, @NonNull LocalUser localUser) {

        RoomInfo tempRoom = _subRoomInfo.getValue();
        if (tempRoom != null) {
            leaveSubRoom(tempRoom.getId());
        }

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
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    BaseUtil.logD("onJoinChannelSuccess:" + uid);
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    _subRoomInfo.postValue(subRoomInfo);
                    BaseUtil.logD("uid:" + uid);
                }
            });
        }
    }

    public void leaveSubRoom(@NonNull String channelId) {
        _subRoomInfo.postValue(null);
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            RtcConnection connection = new RtcConnection();
            connection.channelId = channelId;
            engine.leaveChannelEx(connection);
        }
    }

    public void initRTC(@NonNull Context mContext, @NonNull IRtcEngineEventHandler mEventHandler) {
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
        }
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
            BaseUtil.logD(roomInfo.toString());
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.getUserId()));
            if (isLocalHost) {
                engine.setupRemoteVideo(videoCanvas);
            } else {
                RtcConnection connection = new RtcConnection();
                connection.channelId = roomInfo.getId();
                connection.localUid = -Integer.parseInt(GameApplication.getInstance().user.getUserId());
                engine.setupRemoteVideoEx(videoCanvas, connection);
            }
        }
    }
    //</editor-fold>
}
