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
import io.agora.sample.rtegame.bean.Gift;
import io.agora.sample.rtegame.bean.GiftInfo;
import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.PKApplyInfo;
import io.agora.sample.rtegame.bean.PKInfo;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomApi;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.GameUtil;
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

    ///////////////////// Audience need to know when host is ready/////////////////////////
    private final MutableLiveData<Integer> _LocalHostId = new MutableLiveData<>();
    @NonNull
    public LiveData<Integer> localHostId() {
        return _LocalHostId;
    }

    /////////////////////OUR presentation only concerns 2 things/////////////////////////
    ////////////1. 当前是否有其他主播////////////////// 2. 是否在游戏 ////////////////////////

    // 连麦房间信息
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


    public RoomViewModel(@NonNull Context context, @NonNull RoomInfo roomInfo) {
        this.currentRoom = roomInfo;

        // 保证进来时没有PK相关数据
        clearPKApplyInfo(currentRoom.getId());
        clearPKInfo(currentRoom.getId());
        clearGameInfo(currentRoom.getId());



        initRTC(context, new IRtcEngineEventHandler() {

            @Override
            public void onUserJoined(int uid, int elapsed) {
                if (String.valueOf(uid).equals(currentRoom.getUserId()))
                    RoomViewModel.this._LocalHostId.postValue(uid);
            }
        });
        SyncManager.Instance().joinScene(GameUtil.getSceneFromRoomInfo(currentRoom), new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                _viewStatus.postValue(new ViewStatus.Error("加入RTM成功"));
                subscribeRTMAttr(currentRoom);
            }

            @Override
            public void onFail(SyncManagerException exception) {

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

    //<editor-fold desc="Gift related">
    public void donateGift(@NonNull GiftInfo gift){
        SyncManager.Instance().getScene(currentRoom.getId()).update(GameConstants.GIFT_INFO, gift, null);
    }
    //</editor-fold>

    //<editor-fold desc="SyncManager related">

    public void subscribeRTMAttr(@NonNull RoomInfo targetRoom) {
        SyncManager.Instance().getScene(targetRoom.getId()).subscribe(new SyncManager.EventListener() {
            @Override
            public void onCreated(IObject item) {
                BaseUtil.logD("subscribeRTMAttr#onCreated:" +item.getId() + item.toString());
                // 是当前房间
                if (targetRoom.getId().equals(currentRoom.getId())) {
                    tryHandlePKInfo(item);
                    tryHandleGameInfo(item);
                    tryHandleGiftInfo(item);
                    // 主播额外监听
                    if (currentRoom.getUserId().equals(GameApplication.getInstance().user.getUserId()))
                        tryHandleApplyPKInfo(item);
                }else{
                    tryHandleApplyPKInfo(item);
                }
            }

            @Override
            public void onUpdated(IObject item) {
                BaseUtil.logD("subscribeRTMAttr#onUpdated:" +item.getId() + item.toString());
                // 是当前房间
                if (targetRoom.getId().equals(currentRoom.getId())) {
                    tryHandlePKInfo(item);
                    tryHandleGameInfo(item);
                    tryHandleGiftInfo(item);
                    // 主播额外监听
                    if (currentRoom.getUserId().equals(GameApplication.getInstance().user.getUserId()))
                        tryHandleApplyPKInfo(item);
                }else{
                    tryHandleApplyPKInfo(item);
                }
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
            onGameChanged(gameInfo);
//            _gameInfo.postValue(gameInfo);
    }

    private void tryHandlePKInfo(IObject item) {
        PKInfo pkInfo = null;
        try {
            pkInfo = item.toObject(PKInfo.class);
        } catch (Exception ignored) {
        }
        if (pkInfo != null) onPKInfoChanged(pkInfo);
//            _pkInfo.postValue(pkInfo);
    }

    private void tryHandleApplyPKInfo(IObject item) {
        PKApplyInfo pkApplyInfo = null;
        try {
            pkApplyInfo = item.toObject(PKApplyInfo.class);
        } catch (Exception ignored) {
        }
        if (pkApplyInfo != null) {
            onPKApplyInfoChanged(pkApplyInfo);
//            _pkApplyInfo.postValue(pkApplyInfo);
        }
    }


    private void tryHandleGiftInfo(IObject item) {
        BaseUtil.logD("tryHandleGiftInfo:"+item.getId()+"->"+item.toString());
        GiftInfo giftInfo = null;
        try {
            giftInfo = item.toObject(GiftInfo.class);
        } catch (Exception ignored) {
        }
        if (giftInfo != null) {
            _gift.postValue(giftInfo);
        }
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
            // 只用来加入频道，只使用 roomId 字段
            // this variable will only for join channel so room name doesn't matter.
            RoomInfo subRoom = new RoomInfo(pkInfo.getRoomId(), "", pkInfo.getUserId());
            joinSubRoom(subRoom, GameApplication.getInstance().user);
        } else if (pkInfo.getStatus() == PKInfo.END) {
            leaveSubRoom(pkInfo.getRoomId());
        }
    }

    /**
     * 仅主播调用
     */
    private void onPKApplyInfoChanged(PKApplyInfo pkApplyInfo) {
        if (pkApplyInfo == null) return;

        switch (pkApplyInfo.getStatus()) {
            case PKApplyInfo.APPLYING: {
//                showPKDialog(pkApplyInfo);
                break;
            }
            case PKApplyInfo.REFUSED: {
                _viewStatus.postValue(new ViewStatus.Error("玩家拒绝"));
                // 发起方提示
//                if (pkApplyInfo.getRoomId().equals(currentRoom.getId())) {
//                    if (currentDialog != null) currentDialog.dismiss();
//                    mMessageAdapter.addItem("玩家拒绝");
//                }
                break;
            }
            case PKApplyInfo.AGREED: {
                startPK(pkApplyInfo);
                break;
            }
            case PKApplyInfo.END: {
                endPK(pkApplyInfo.getTargetRoomId());
                break;
            }
        }
    }

    public void acceptPK(@NonNull PKApplyInfo pkApplyInfo) {
        pkApplyInfo.setStatus(PKApplyInfo.AGREED);
        SyncManager.Instance().getScene(pkApplyInfo.getTargetRoomId()).update(GameConstants.PK_APPLY_INFO, pkApplyInfo, null);
    }

    public void cancelPK(@NonNull PKApplyInfo pkApplyInfo) {
//        _pkApplyInfo.postValue(null);
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
        } else {//      客户端为接收方
            pkInfo = new PKInfo(PKInfo.AGREED, pkApplyInfo.getRoomId(), pkApplyInfo.getUserId());
        }

        SyncManager.Instance().getScene(pkApplyInfo.getRoomId()).update(GameConstants.PK_INFO, pkInfo, null);
    }

    /**
     * 仅主播调用
     */
    public void endPK(@NonNull String channelId) {
        SyncManager.Instance().getScene(channelId).update(GameConstants.PK_INFO, null, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {

            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    public void clearPKApplyInfo(@NonNull String channelId) {
        SyncManager.Instance().getScene(channelId).update(GameConstants.PK_APPLY_INFO, null, null);
    }

    public void clearPKInfo(@NonNull String channelId) {
        SyncManager.Instance().getScene(channelId).update(GameConstants.PK_INFO, null, null);
    }

    public void clearGameInfo(@NonNull String channelId) {
        SyncManager.Instance().getScene(channelId).update(GameConstants.GAME_INFO, null, null);
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
            if (localUser.getUserId().equals(currentRoom.getUserId())) {
                engine.enableAudio();
                engine.enableVideo();
                engine.startPreview();
                options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            } else {
                options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
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
                public void onUserJoined(int uid, int elapsed) {
                    if (String.valueOf(uid).equals(subRoomInfo.getUserId())) {
                        _subRoomInfo.postValue(subRoomInfo);
                    }
                }
            });
        }
    }

    public void leaveSubRoom(@NonNull String channelId) {
        _subRoomInfo.setValue(null);
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            RtcConnection connection = new RtcConnection();
            connection.channelId = channelId;
            engine.leaveChannelEx(connection);
        }
    }

    public void initRTC(@NonNull Context mContext, @NonNull IRtcEngineEventHandler mEventHandler) {
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
                engine.setupRemoteVideoEx(videoCanvas, connection);
            }
        }
    }
    //</editor-fold>
}
