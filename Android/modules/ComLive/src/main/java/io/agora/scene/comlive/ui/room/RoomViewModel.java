package io.agora.scene.comlive.ui.room;

import android.content.Context;
import android.view.TextureView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.bean.AgoraGame;
import io.agora.scene.comlive.bean.AppServerResult;
import io.agora.scene.comlive.bean.GameInfo;
import io.agora.scene.comlive.bean.GiftInfo;
import io.agora.scene.comlive.bean.LocalUser;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.repo.GameRepo;
import io.agora.scene.comlive.util.ComLiveConstants;
import io.agora.scene.comlive.util.Event;
import io.agora.scene.comlive.util.GamSyncEventListener;
import io.agora.scene.comlive.util.ViewStatus;
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
    @NonNull
    private final RoomInfo currentRoom;
    private final boolean amHost;
    @NonNull
    public LocalUser localUser;
    @Nullable
    public SceneReference currentSceneRef;

    @NonNull
    public MutableLiveData<AgoraGame> currentGame = new MutableLiveData<>();
    @NonNull
    public MutableLiveData<Boolean> isMicEnabled = new MutableLiveData<>(true);
    @NonNull
    public MutableLiveData<Boolean> isCameraEnabled = new MutableLiveData<>(true);
    @NonNull
    public MutableLiveData<Event<GiftInfo>> gift = new MutableLiveData<>();
    @NonNull
    public MutableLiveData<GameInfo> gameInfo = new MutableLiveData<>();
    @NonNull
    public MutableLiveData<RtcEngineEx> rtcEngine = new MutableLiveData<>();
    @NonNull
    public MutableLiveData<ViewStatus> viewStatus = new MutableLiveData<>();
    @NonNull
    public MutableLiveData<List<AgoraGame>> gameList = new MutableLiveData<>();
    @NonNull
    public MutableLiveData<Event<String>> gameStartUrl = new MutableLiveData<>();

    public RoomViewModel(@NonNull Context context, @NonNull RoomInfo currentRoom) {
        this.currentRoom = currentRoom;
        assert GlobalViewModel.localUser != null;
        localUser = GlobalViewModel.localUser;
        // Consume at the beginning
        Event<String> objectEvent = new Event<>("");
        objectEvent.getContentIfNotHandled();
        gameStartUrl.setValue(objectEvent);

        amHost = Objects.equals(currentRoom.getUserId(), localUser.getUserId());
        initSDK(context, new IRtcEngineEventHandler() {
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // destroy RTC
        RtcEngine engine = rtcEngine.getValue();
        if (engine != null) {
            engine.leaveChannel();
            RtcEngine.destroy();
        }

        if (currentSceneRef != null) {
            if (amHost)
                currentSceneRef.delete(null);
            else currentSceneRef.unsubscribe(null);
        }
    }

    public void initSDK(@NonNull Context mContext, @NonNull IRtcEngineEventHandler mEventHandler) {
        String appID = mContext.getString(R.string.rtc_app_id);
        if (appID.isEmpty() || appID.codePointCount(0, appID.length()) != 32) {
            viewStatus.postValue(new ViewStatus.Message("APP ID is not valid"));
            rtcEngine.postValue(null);
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
                RtcEngineEx engine = (RtcEngineEx) RtcEngineEx.create(config);
                configRTC(engine);
                rtcEngine.postValue(engine);
                // 监听当前房间数据 <==> 礼物、PK、
                Sync.Instance().joinScene(currentRoom.getId(), new Sync.JoinSceneCallback() {
                    @Override
                    public void onSuccess(SceneReference sceneReference) {
                        onJoinRTMSucceed(sceneReference);
                    }

                    @Override
                    public void onFail(SyncManagerException e) {
                        e.printStackTrace();
                        viewStatus.postValue(new ViewStatus.Error("主播已下播💔"));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                viewStatus.postValue(new ViewStatus.Error(e));
                rtcEngine.postValue(null);
            }
        }
    }


    private void onJoinRTMSucceed(@NonNull SceneReference sceneReference) {
        BaseUtil.logD("onJoinRTMSucceed");
        currentSceneRef = sceneReference;
        viewStatus.postValue(new ViewStatus.Message(localUser.getName() + " 加入频道成功"));
        if (currentSceneRef != null) {
            currentSceneRef.subscribe(new Sync.EventListener() {
                @Override
                public void onCreated(IObject item) {

                }

                @Override
                public void onUpdated(IObject item) {

                }

                @Override
                public void onDeleted(IObject item) {
                    viewStatus.postValue(new ViewStatus.Error("主播已下播💔"));
                }

                @Override
                public void onSubscribeError(SyncManagerException ex) {

                }
            });
            currentSceneRef.get(ComLiveConstants.GIFT_INFO, (GetAttrCallback) this::tryHandleGetGiftInfo);
            currentSceneRef.subscribe(ComLiveConstants.GIFT_INFO, new GamSyncEventListener(ComLiveConstants.GIFT_INFO, this::tryHandleGiftInfo));
            currentSceneRef.subscribe(ComLiveConstants.GAME_INFO, new GamSyncEventListener(ComLiveConstants.GAME_INFO, this::tryHandleGameInfo));
            if (amHost)
                currentSceneRef.update(ComLiveConstants.GAME_INFO, new GameInfo(GameInfo.END, "0", "0"), null);
            else
                currentSceneRef.get(ComLiveConstants.GAME_INFO, (GetAttrCallback) this::tryHandleGameInfo);
        }
    }

    /**
     * 加入房间先获取当前 Gift
     */
    private void tryHandleGetGiftInfo(@Nullable IObject item) {
        GiftInfo giftInfo = handleIObject(item, GiftInfo.class);
        if (giftInfo != null) {
            Event<GiftInfo> giftInfoEvent = new Event<>(giftInfo);
            giftInfoEvent.getContentIfNotHandled(); // consume this time
            gift.postValue(giftInfoEvent);
        }
    }

    private void tryHandleGiftInfo(IObject item) {
        GiftInfo giftInfo = handleIObject(item, GiftInfo.class);
        if (giftInfo != null) {
            gift.postValue(new Event<>(giftInfo));
        }
    }

    private void tryHandleGameInfo(@Nullable IObject item) {
        GameInfo gameInfo = handleIObject(item, GameInfo.class);
        if (gameInfo != null) {
            if (gameInfo.getStatus() != GameInfo.END)
                this.currentGame.postValue(new AgoraGame(gameInfo.getGameId(), ""));
            this.gameInfo.postValue(gameInfo);
        }
    }

    @Nullable
    private <T> T handleIObject(@Nullable IObject obj, @NonNull Class<T> clazz) {
        if (obj == null) return null;

        T res = null;
        try {
            res = obj.toObject(clazz);
        } catch (Exception e) {
            e.printStackTrace();
            BaseUtil.logD(e.getMessage());
        }
        return res;
    }

    public void donateGift(@NonNull GiftInfo giftInfo) {
        if (currentSceneRef != null)
            currentSceneRef.update(ComLiveConstants.GIFT_INFO, giftInfo, null);

//        if (ComLiveUtil.currentGame != null) {
//            GameRepo.sendGift(localUser, Integer.parseInt(currentRoom.getId()), amHost ? 1 : 2, giftInfo);
//        }
    }

    public void requestExitGame() {
        if (amHost) {
            if (currentGame.getValue() != null && currentSceneRef != null)
                currentSceneRef.update(ComLiveConstants.GAME_INFO, new GameInfo(GameInfo.END, "0", currentGame.getValue().getGameId()), null);
        } else {
            exitGame();
        }
    }

    /**
     * 1. 发送请求退出游戏
     * 2. 清除游戏信息
     */
    public void exitGame() {
        if (currentGame.getValue() != null) {
            GameRepo.leaveGame(currentGame.getValue().getGameId(), localUser, currentRoom.getId(), amHost ? "1" : "3");
            currentGame.postValue(null);
        }
    }

    /**
     * 主播选择游戏
     */
    public void requestStartGame(@NonNull String gameId) {
        if (currentSceneRef != null)
            currentSceneRef.update(ComLiveConstants.GAME_INFO, new GameInfo(GameInfo.START, "0", gameId), null);
    }

    public void startGame(@NonNull String gameId) {
        GameRepo.getJoinUrl(gameId, localUser, currentRoom.getId(), amHost ? "1" : "3", new Callback<AppServerResult<String>>() {
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
        GameRepo.getGameList("2", gameList);
    }

    //<editor-fold desc="RTC related">
    public void enableMic(boolean enable) {
        isMicEnabled.postValue(enable);
        RtcEngineEx engine = rtcEngine.getValue();
        if (engine != null) {
            engine.muteLocalAudioStream(!enable);
        }
    }

    /**
     * 2022.01.17 11:00
     * 负责人表示游戏内 JS 对麦克风的启停只影响其他玩家，不影响主播
     * @param enable true 启用；false 禁用
     */
    public void jsEnableMic(boolean enable){
        if (amHost) return;
        BaseUtil.logD(Thread.currentThread().getName() + "enableMic " + enable);
        isMicEnabled.postValue(enable);
        RtcEngineEx engine = rtcEngine.getValue();
        if (engine != null) {
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = true;
            options.publishAudioTrack = enable;
            options.publishCameraTrack = false;
            options.clientRoleType = enable ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
            int ret = engine.updateChannelMediaOptions(options);
            engine.enableLocalAudio(enable);
            BaseUtil.logD(Thread.currentThread().getName() + "updateChannelMediaOptions " + ret);
        }
    }

    public void enableCamera(boolean enable) {
        isCameraEnabled.postValue(enable);
        RtcEngineEx engine = rtcEngine.getValue();
        if (engine != null) {
            engine.muteLocalVideoStream(!enable);
        }
    }

    public void flipCamera() {
        RtcEngineEx engine = this.rtcEngine.getValue();
        if (engine != null) {
            engine.switchCamera();
        }
    }


    public void changeRole(int oldRole, int newRole) {
        AgoraGame game = currentGame.getValue();
        if (game != null) {
            GameRepo.changeRole(game.getGameId(), localUser, currentRoom.getId(), oldRole, newRole);
        }
    }

    public void joinRoom(@NonNull RtcEngine engine) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        options.publishAudioTrack = amHost;
        options.publishCameraTrack = amHost;
        options.clientRoleType = amHost ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        engine.joinChannel(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), currentRoom.getId(), Integer.parseInt(localUser.getUserId()), options);
    }

    public void setupLocalView(@NonNull TextureView view) {
        RtcEngine engine = rtcEngine.getValue();
        if (engine != null) {
            engine.startPreview();
            engine.setupLocalVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(localUser.getUserId())));
        }
    }

    public void setupRemoteView(@NonNull TextureView view) {
        RtcEngine engine = rtcEngine.getValue();
        if (engine != null) {
            engine.setupRemoteVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(currentRoom.getUserId())));
        }
    }

    private void configRTC(@NonNull RtcEngineEx engine) {
        engine.enableAudio();
        engine.enableVideo();
    }
//</editor-fold>


    private interface GetAttrCallback extends Sync.DataItemCallback {
        @Override
        default void onFail(SyncManagerException exception) {

        }
    }
}
