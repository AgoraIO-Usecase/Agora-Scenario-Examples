package io.agora.sample.rtegame.ui.roompage;

import android.content.Context;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.internal.RtcEngineImpl;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomApi;
import io.agora.sample.rtegame.util.ViewStatus;


/**
 * @author lq
 */
public class RoomViewModel extends ViewModel implements RoomApi {

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
    public LiveData<Integer> hostUID(){ return _hostUID;}

    private final MutableLiveData<RoomInfo> _subRoomInfo = new MutableLiveData<>();
    public LiveData<RoomInfo> subRoomInfo(){ return _subRoomInfo;}


    public RoomViewModel(Context context) {
        initRTC(context, new IRtcEngineEventHandler() {
            @Override
            public void onUserJoined(int uid, int elapsed) {
                    _hostUID.postValue(uid);
            }
        });
//        initRTC(context, new IRtcEngineEventHandler() {
//            @Override
//            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
//                BaseUtil.logD("onJoinChannelSuccess:"+channel);
//            }
//            @Override
//            public void onError(int err) {
//                BaseUtil.logE(RtcEngine.getErrorDescription(err));
//            }
//            /**
//             * {@see <a href="https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler.html?platform=Android#a31b2974a574ec45e62bb768e17d1f49e">}
//             */
//            @Override
//            public void onConnectionStateChanged(int state, int reason) {
//                BaseUtil.logD("onConnectionStateChanged:" + state + "," + reason);
//            }
//            @Override
//            public void onUserJoined(int uid, int elapsed) {
//                BaseUtil.logD("onUserJoined:" + uid);
//                Set<Integer> set = _rtcUserSet.getValue();
//                if (set == null) set = new LinkedHashSet<>();
//                set.add(uid);
//                _rtcUserSet.postValue(set);
//            }
//
//            @Override
//            public void onUserOffline(int uid, int reason) {
//                Set<Integer> set = _rtcUserSet.getValue();
//                if (set != null && set.remove(uid)){
//                    _rtcUserSet.postValue(set);
//                }
//            }
//        });
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

    @Override
    public void joinRoom(@NonNull RoomInfo roomInfo, LocalUser localUser){
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            if (localUser.getUserId().equals(roomInfo.getUserId())) {
                engine.enableAudio();
                engine.enableVideo();
                engine.startPreview();
            }else{
                engine.disableAudio();
                engine.disableVideo();
            }
//            joinChannel(String token, String channelName, String optionalInfo, int optionalUid);
            engine.joinChannel(((RtcEngineImpl) engine).getContext().getString(R.string.rtc_app_token), roomInfo.getId(), null, Integer.parseInt(localUser.getUserId()));
        }
    }

    /**
     * 加入其他主播房间前先退出现在房间
     * 加入成功监听到对方主播上线《==》UI更新
     */
    @Override
    public void joinSubRoom(@NonNull RoomInfo subRoomInfo, LocalUser localUser) {
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
                public void onUserJoined(int uid, int elapsed) {
                    if (String.valueOf(uid).equals(subRoomInfo.getUserId())) {
                        if (_subRoomInfo.getValue() == null)
                            _subRoomInfo.postValue(subRoomInfo);
                    }
                }
            });
        }
    }

    public void leaveSubRoom(){
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null){
            RoomInfo info = _subRoomInfo.getValue();
            if (info != null){
                RtcConnection connection = new RtcConnection();
                connection.channelId = info.getId();

                engine.leaveChannelEx(connection);
            }
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
     *
     * @param view 用来构造 videoCanvas
     * @param roomInfo isHost => current RoomInfo，!isHost => 对方的 RoomInfo
     * @param isHost 是否是当前房间主播
     */
    public void setupRemoteView(TextureView view, RoomInfo roomInfo, boolean isHost) {
        RtcEngineEx engine = _mEngine.getValue();
        if (engine != null) {
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(roomInfo.getUserId()));
            if (isHost){
                engine.setupRemoteVideo(videoCanvas);
            }else {
                RtcConnection connection = new RtcConnection();
                connection.channelId = roomInfo.getId();
                engine.setupRemoteVideoEx(videoCanvas, connection);
            }
        }
    }
}
