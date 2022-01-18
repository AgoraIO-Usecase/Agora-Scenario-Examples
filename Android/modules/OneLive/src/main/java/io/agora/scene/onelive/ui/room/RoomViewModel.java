package io.agora.scene.onelive.ui.room;

import android.content.Context;
import android.view.TextureView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.agora.example.base.BaseUtil;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.internal.RtcEngineImpl;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.onelive.R;
import io.agora.scene.onelive.bean.AgoraGame;
import io.agora.scene.onelive.bean.AppServerResult;
import io.agora.scene.onelive.bean.GameInfo;
import io.agora.scene.onelive.bean.LocalUser;
import io.agora.scene.onelive.bean.RoomInfo;
import io.agora.scene.onelive.repo.GameRepo;
import io.agora.scene.onelive.util.Event;
import io.agora.scene.onelive.util.OneConstants;
import io.agora.scene.onelive.util.OneSyncEventListener;
import io.agora.scene.onelive.util.ViewStatus;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * @author lq
 */
@Keep
public class RoomViewModel extends ViewModel {

    // 用此标识游戏发起端
    public boolean startedByMe = false;

    //<editor-fold desc="Persistent variable">
    // 当前用户是否为主播
    public final boolean amHost;
    // 当前房间信息
    public final RoomInfo currentRoom;
    // 当前在玩游戏信息
    @Nullable
    public AgoraGame currentGame;
    // 当前用户信息
    @NonNull
    public final LocalUser localUser;
    // SyncManager 必须
    @Nullable
    public SceneReference currentSceneRef = null;
    //</editor-fold>


    //<editor-fold desc="Live data">
    // RTC engine 初始化结果
    private final MutableLiveData<RtcEngineEx> _mEngine = new MutableLiveData<>();
    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();
    // 对方主播信息
    private final MutableLiveData<LocalUser> _targetUser = new MutableLiveData<>();
    // 当前游戏信息
    private final MutableLiveData<GameInfo> _gameInfo = new MutableLiveData<>();
    // RTC 音频状态
    public final MutableLiveData<Boolean> isLocalMicMuted = new MutableLiveData<>(false);
    // RTC 音频状态
    public final MutableLiveData<List<AgoraGame>> gameList = new MutableLiveData<>(new ArrayList<>());
    @NonNull
    public MutableLiveData<Event<String>> gameStartUrl = new MutableLiveData<>();
    //</editor-fold>

    //<editor-fold desc="Init and end">
    public RoomViewModel(@NonNull Context context, @NonNull LocalUser localUser, @NonNull RoomInfo roomInfo) {
        this.currentRoom = roomInfo;
        this.localUser = localUser;

        this.amHost = Objects.equals(currentRoom.getUserId(), localUser.getUserId());

        // Consume at the beginning
        Event<String> objectEvent = new Event<>("");
        objectEvent.getContentIfNotHandled();
        gameStartUrl.setValue(objectEvent);

        initRTC(context, new IRtcEngineEventHandler() {
            @Override
            public void onUserJoined(int uid, int elapsed) {
                BaseUtil.logD("onUserJoined:" + uid);
                if (!amHost) return;
                if (_targetUser.getValue() == null)
                    _targetUser.postValue(new LocalUser(String.valueOf(uid)));
            }

            @Override
            public void onUserOffline(int uid, int reason) {
                BaseUtil.logD("onUserOffline:" + uid);
                if (_targetUser.getValue() != null) {
                    if (_targetUser.getValue().getUserId().equals(String.valueOf(uid)))
                        _targetUser.postValue(null);
                }
                if (currentGame != null) {
                    requestEndGame();
                }
            }

            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                BaseUtil.logD("onJoinChannelSuccess:" + uid);
                if (!amHost)
                    // 副主播只能与房主连线
                    _targetUser.postValue(new LocalUser(currentRoom.getUserId()));
            }

        });
    }

    private void onJoinRTMSucceed(@NonNull SceneReference sceneReference) {
        BaseUtil.logD("RTM ready");
        currentSceneRef = sceneReference;
        subscribeAttr();
    }

    private void subscribeAttr() {
        if (currentSceneRef != null) {
            if (amHost)
                currentSceneRef.update(OneConstants.GAME_INFO, new GameInfo(GameInfo.END, "0"), null);
            else
                currentSceneRef.get(OneConstants.GAME_INFO, (GetAttrCallback) this::handleGetGameInfo);
            currentSceneRef.subscribe(OneConstants.GAME_INFO, new OneSyncEventListener(OneConstants.GAME_INFO, this::handleGameInfo));
            currentSceneRef.subscribe(new Sync.EventListener() {
                @Override
                public void onCreated(IObject item) {

                }

                @Override
                public void onUpdated(IObject item) {

                }

                @Override
                public void onDeleted(IObject item) {
                    _viewStatus.postValue(new ViewStatus.Error(""));
                }

                @Override
                public void onSubscribeError(SyncManagerException ex) {

                }
            });
        }
    }

    private void handleGetGameInfo(@Nullable IObject iObject) {
        GameInfo gameInfo = tryHandleIObject(iObject, GameInfo.class);
        if (gameInfo != null) {
            switch (gameInfo.getStatus()) {
                case GameInfo.START: {
                    _gameInfo.postValue(gameInfo);
                    break;
                }
                case GameInfo.END: {
                    currentGame = null;
                    break;
                }
            }
        }
    }

    /**
     * 游戏 未开始：不做任何操作
     * 已开始：加载
     * 结束： 发送网络请求
     */
    private void handleGameInfo(@Nullable IObject iObject) {
        GameInfo gameInfo = tryHandleIObject(iObject, GameInfo.class);
        if (gameInfo != null) {
            switch (gameInfo.getStatus()) {
                case GameInfo.START: {
                    currentGame = new AgoraGame(gameInfo.getGameId(), "");
                    _gameInfo.postValue(gameInfo);
                    break;
                }
                case GameInfo.END: {
                    _gameInfo.postValue(gameInfo);
                    if (currentGame != null) {
                        GameRepo.leaveGame(currentGame.getGameId(), localUser, currentRoom.getId(), amHost ? "1" : "2");
                        currentGame = null;
                    }
                    break;
                }
            }
        }
    }

    @Nullable
    private <T> T tryHandleIObject(@Nullable IObject result, @NonNull Class<T> modelClass) {
        if (result == null) return null;
        T obj = null;
        try {
            obj = result.toObject(modelClass);
        } catch (Exception ignored) {
        }
        return obj;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        new Thread(() -> {
            if (currentGame != null) {
                GameRepo.leaveGame(currentGame.getGameId(), localUser, currentRoom.getId(), amHost ? "1" : "2");
            }
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
        }).start();
    }
    //</editor-fold>

    @NonNull
    public LiveData<RtcEngineEx> mEngine() {
        return _mEngine;
    }

    @NonNull
    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    @NonNull
    public LiveData<LocalUser> targetUser() {
        return _targetUser;
    }

    @NonNull
    public LiveData<GameInfo> gameInfo() {
        return _gameInfo;
    }

    public void requestStartGame(@NonNull AgoraGame agoraGame) {
        if (currentSceneRef != null) {
            startedByMe = true;
            GameInfo gameInfo = new GameInfo(GameInfo.START, agoraGame.getGameId());
            currentSceneRef.update(OneConstants.GAME_INFO, gameInfo, null);
        }
    }

    public void requestEndGame() {
        if (currentSceneRef != null && currentGame != null) {
            currentSceneRef.update(OneConstants.GAME_INFO, new GameInfo(GameInfo.END, currentGame.getGameId()), null);
        }
    }

    public void initRTM() {
        new Thread(() -> Sync.Instance().joinScene(currentRoom.getId(), new Sync.JoinSceneCallback() {
            @Override
            public void onSuccess(SceneReference sceneReference) {
                BaseUtil.logD("加入频道成功");
                onJoinRTMSucceed(sceneReference);
            }

            @Override
            public void onFail(SyncManagerException e) {
                BaseUtil.logD("加入频道失败");
                _viewStatus.postValue(new ViewStatus.Error("加入RTM失败"));
            }
        })).start();
    }
    //<editor-fold desc="APP Server">
    public void startGame(@NonNull String gameId) {
        GameRepo.getJoinUrl(gameId, localUser, currentRoom.getId(), amHost ? "1" : "2", new Callback<AppServerResult<String>>() {
            @Override
            public void onResponse(Call<AppServerResult<String>> call, Response<AppServerResult<String>> response) {
                AppServerResult<String> body = response.body();
                if (body != null)
                    gameStartUrl.postValue(new Event<>(body.getResult()));
            }

            @Override
            public void onFailure(Call<AppServerResult<String>> call, Throwable t) {
            }
        });
    }
    public void fetchGameList() {
        GameRepo.getGameList("1", gameList);
    }
    //</editor-fold>

    //<editor-fold desc="RTC related">

    public void enableMic(boolean enable) {
        isLocalMicMuted.setValue(!enable);
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.muteLocalAudioStream(!enable);
        }
    }

    public void changeRole(int oldRole, int newRole){
        if (currentGame != null) {
            GameRepo.changeRole(currentGame.getGameId(), localUser, currentRoom.getId(), oldRole, newRole);
        }
    }


    public void flipCamera() {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.switchCamera();
        }
    }

    public void joinRoom(@NonNull RtcEngineEx engine) {
        new Thread(() -> {
            BaseUtil.logD("joinRoom");
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;

            engine.joinChannel(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), currentRoom.getId(), Integer.parseInt(localUser.getUserId()), options);
        }).start();
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
                logConfig.level = Constants.LogLevel.LOG_LEVEL_ERROR.ordinal();
                File cacheDir = mContext.getExternalCacheDir();
                boolean isCacheDirExist = cacheDir.exists();
                if (!isCacheDirExist) {
                    isCacheDirExist = cacheDir.mkdir();
                }
                if (isCacheDirExist)
                    logConfig.filePath = cacheDir.getAbsolutePath();
                config.mLogConfig = logConfig;
                config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

                try {
                    RtcEngineEx engineEx = (RtcEngineEx) RtcEngineEx.create(config);
                    _mEngine.postValue(engineEx);
                    joinRoom(engineEx);
                    initRTM();
                } catch (Exception e) {
                    e.printStackTrace();
                    _viewStatus.postValue(new ViewStatus.Error(e));
                    _mEngine.postValue(null);
                }
            }
        }).start();
    }

    public void setupLocalView(@NonNull TextureView view) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.enableAudio();
            engine.enableVideo();
            engine.startPreview();
            engine.setupLocalVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(localUser.getUserId())));
        }
    }

    /**
     * @param view 用来构造 videoCanvas
     */
    public void setupRemoteView(@NonNull TextureView view, int uid) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, uid);
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
