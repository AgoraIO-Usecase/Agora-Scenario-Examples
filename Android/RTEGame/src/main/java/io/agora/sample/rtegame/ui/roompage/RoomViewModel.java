package io.agora.sample.rtegame.ui.roompage;

import android.content.Context;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.PKApplyInfo;
import io.agora.sample.rtegame.bean.PKInfo;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomApi;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.ViewStatus;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;


/**
 * @author lq
 */
public class RoomViewModel extends ViewModel implements RoomApi {

    public final RoomInfo currentRoom;

    public boolean isLocalVideoMuted = false;
    public boolean isLocalMicMuted = false;


    private final MutableLiveData<RtcEngineEx> _mEngine = new MutableLiveData<>();

    public LiveData<RtcEngineEx> mEngine() {
        return _mEngine;
    }

    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();

    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    private final MutableLiveData<Integer> _hostUID = new MutableLiveData<>();

    public LiveData<Integer> hostUID() {
        return _hostUID;
    }

    // 连麦房间信息
    private final MutableLiveData<RoomInfo> _subRoomInfo = new MutableLiveData<>();

    public LiveData<RoomInfo> subRoomInfo() {
        return _subRoomInfo;
    }

    // 发现 pkApplyInfo 发生改变(可能为自己，可能为他人)
    private final MutableLiveData<PKApplyInfo> _pkApplyInfo = new MutableLiveData<>();

    public LiveData<PKApplyInfo> pkApplyInfo() {
        return _pkApplyInfo;
    }

    // pkInfo 发生改变
    private final MutableLiveData<PKInfo> _pkInfo = new MutableLiveData<>();

    public LiveData<PKInfo> pkInfo() {
        return _pkInfo;
    }

    // pkInfo 发生改变
    private final MutableLiveData<GameInfo> _gameInfo = new MutableLiveData<>();

    public LiveData<GameInfo> gameInfo() {
        return _gameInfo;
    }

    public RoomViewModel(Context context, RoomInfo roomInfo) {
        this.currentRoom = roomInfo;

        // 保证进来时没有PK相关数据
        clearPKApplyInfo(currentRoom.getId());
        clearPKInfo(currentRoom.getId());
        clearGameInfo(currentRoom.getId());


        if (currentRoom.getUserId().equals(GameApplication.getInstance().user.getUserId())) {
            subscribeApplyPKInfo(currentRoom);
        }

        subscribePKInfo();
        subscribeGameInfo();

        initRTC(context, new IRtcEngineEventHandler() {
            @Override
            public void onUserJoined(int uid, int elapsed) {
                _hostUID.postValue(uid);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        new Thread(() -> {
            RtcEngine engine = _mEngine.getValue();
            if (engine != null) {
                engine.leaveChannel();
                RtcEngine.destroy();
            }
        }).start();
    }

    //<editor-fold desc="SyncManager related">

    public void subscribeGameInfo() {
        SyncManager.Instance().getScene(currentRoom.getId()).subscribe(new SyncManager.EventListener() {
            @Override
            public void onCreated(IObject item) {
                BaseUtil.logD("subscribePKInfo#onCreated:" + item.toString());
                tryHandleGameInfo(item);
            }

            @Override
            public void onUpdated(IObject item) {
                BaseUtil.logD("subscribePKInfo#onUpdated:" + item.toString());
                tryHandleGameInfo(item);
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        });
    }

    public void subscribePKInfo() {
        SyncManager.Instance().getScene(currentRoom.getId()).subscribe(new SyncManager.EventListener() {
            @Override
            public void onCreated(IObject item) {
                BaseUtil.logD("subscribePKInfo#onCreated:" + item.toString());
                tryHandlePKInfo(item);
            }

            @Override
            public void onUpdated(IObject item) {
                BaseUtil.logD("subscribePKInfo#onUpdated:" + item.toString());
                tryHandlePKInfo(item);
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        });
    }

    /**
     * PKApplyInfo监听
     * 仅主播调用
     */
    public void subscribeApplyPKInfo(@NonNull RoomInfo targetRoom) {
        SyncManager.Instance().getScene(targetRoom.getId()).subscribe(new SyncManager.EventListener() {
            @Override
            public void onCreated(IObject item) {
                BaseUtil.logD("subScribeApplyPKInfo#onCreated:" + item.toString());
                tryHandleApplyPKInfo(item);
            }

            @Override
            public void onUpdated(IObject item) {
                BaseUtil.logD("subScribeApplyPKInfo#onUpdated:" + item.toString());
                tryHandleApplyPKInfo(item);
            }

            @Override
            public void onDeleted(IObject item) {

            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {

            }
        });
    }

    private void tryHandleGameInfo(IObject item) {
        GameInfo gameInfo = null;
        try {
            gameInfo = item.toObject(GameInfo.class);
        } catch (Exception ignored) {
        }
        if (gameInfo != null)
            _gameInfo.postValue(gameInfo);
    }

    private void tryHandlePKInfo(IObject item) {
        PKInfo pkInfo = null;
        try {
            pkInfo = item.toObject(PKInfo.class);
        } catch (Exception ignored) {
        }
        if (pkInfo != null)
            _pkInfo.postValue(pkInfo);
    }

    private void tryHandleApplyPKInfo(IObject item) {
        PKApplyInfo pkApplyInfo = null;
        try {
            pkApplyInfo = item.toObject(PKApplyInfo.class);
        } catch (Exception ignored) {
        }
        if (pkApplyInfo != null) {
            _pkApplyInfo.postValue(pkApplyInfo);
        }
    }

    public void acceptPK(@NonNull PKApplyInfo pkApplyInfo) {
        pkApplyInfo.setStatus(PKApplyInfo.AGREED);
        SyncManager.Instance().getScene(pkApplyInfo.getTargetRoomId()).update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
    }

    public void cancelPK(@NonNull PKApplyInfo pkApplyInfo) {
        _pkApplyInfo.postValue(null);
        pkApplyInfo.setStatus(PKApplyInfo.REFUSED);
        SyncManager.Instance().getScene(pkApplyInfo.getTargetRoomId()).update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
    }

    /**
     * 仅主播调用
     * 主播开始PK，为接收方接受发起方PK请求后的调用
     * Step 1. 根据当前角色生成 PKInfo
     * Step 2. 更新频道内{@link GameConstants#PK_INFO} 参数
     */
    public void startPK(@NonNull PKApplyInfo pkApplyInfo) {
        PKInfo pkInfo;
        if (pkApplyInfo.getRoomId().equals(currentRoom.getId())) {//      客户端为发起方
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getTargetRoomId(), pkApplyInfo.getTargetUserId());
        }else{//      客户端为接收方
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getRoomId(), pkApplyInfo.getUserId());
        }

        SyncManager.Instance().getScene(pkApplyInfo.getRoomId()).update(GameConstants.PK_INFO, pkInfo, null);
    }

    /**
     * 仅主播调用
     */
    public void endPK(String channelId) {
        SyncManager.Instance().getScene(channelId).update(GameConstants.PK_INFO, null, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void clearPKApplyInfo(String channelId){
        SyncManager.Instance().getScene(channelId).update(GameConstants.PK_APPLY_INFO, null, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }
    public void clearPKInfo(String channelId){
        SyncManager.Instance().getScene(channelId).update(GameConstants.PK_INFO, null, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }
    public void clearGameInfo(String channelId){
        SyncManager.Instance().getScene(channelId).update(GameConstants.GAME_INFO, null, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
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
    public void joinRoom(LocalUser localUser) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            if (localUser.getUserId().equals(currentRoom.getUserId())) {
                engine.enableAudio();
                engine.enableVideo();
                engine.startPreview();
            } else {
                engine.disableAudio();
                engine.disableVideo();
            }
//            joinChannel(String token, String channelName, String optionalInfo, int optionalUid);
            engine.joinChannel(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), currentRoom.getId(), null, Integer.parseInt(localUser.getUserId()));
        }
    }

    /**
     * 加入其他主播房间前先退出现在房间
     * 加入成功监听到对方主播上线《==》UI更新
     */
    @Override
    public void joinSubRoom(@NonNull RoomInfo subRoomInfo, LocalUser localUser) {

        RoomInfo tempRoom = _subRoomInfo.getValue();
        if (tempRoom != null){
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
                public void onUserJoined(int uid, int elapsed) {
                    if (String.valueOf(uid).equals(subRoomInfo.getUserId())) {
                        _subRoomInfo.postValue(subRoomInfo);
                    }
                }
            });
        }
    }

    public void leaveSubRoom(String channelId) {
        _subRoomInfo.setValue(null);
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            RtcConnection connection = new RtcConnection();
            connection.channelId = channelId;
            engine.leaveChannelEx(connection);
        }
    }

    public void initRTC(Context mContext, IRtcEngineEventHandler mEventHandler) {
        String appID = mContext.getString(R.string.rtc_app_id);
        if (appID.length() != 32) {
            _viewStatus.postValue(new ViewStatus.Error("APPID is not valid"));
            _mEngine.postValue(null);
        } else {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = mContext;
            config.mAppId = appID;
            config.mEventHandler = mEventHandler;
            RtcEngineConfig.LogConfig logConfig = new RtcEngineConfig.LogConfig();
            logConfig.filePath = mContext.getExternalCacheDir().getAbsolutePath();
            config.mLogConfig = logConfig;

            try {
                _mEngine.postValue((RtcEngineEx) RtcEngineEx.create(config));
            } catch (Exception e) {
                e.printStackTrace();
                _viewStatus.postValue(new ViewStatus.Error(e));
                _mEngine.postValue(null);
            }
        }
    }

    public void setupLocalView(TextureView view, LocalUser localUser) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            engine.setupLocalVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(localUser.getUserId())));
        }
    }

    /**
     * @param view     用来构造 videoCanvas
     * @param roomInfo isHost => current RoomInfo，!isHost => 对方的 RoomInfo
     * @param isHost   是否是当前房间主播
     */
    public void setupRemoteView(TextureView view, RoomInfo roomInfo, boolean isHost) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.getUserId()));
            if (isHost) {
                engine.setupRemoteVideo(videoCanvas);
            } else {
                RtcConnection connection = new RtcConnection();
                connection.channelId = roomInfo.getId();
                engine.setupRemoteVideoEx(videoCanvas, connection);
            }
        }
    }
    //</editor-fold>
}
