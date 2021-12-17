package io.agora.sample.breakoutroom.ui.room;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.agora.example.base.BaseUtil;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.RtcEngineConfig;
import io.agora.rtc.internal.RtcEngineImpl;
import io.agora.rtc.video.VideoCanvas;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomConstant;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.bean.SubRoomInfo;
import io.agora.sample.breakoutroom.repo.RoomApi;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 *
 */
public class RoomViewModel extends ViewModel implements RoomApi {

    public RoomInfo currentRoomInfo;
    public @Nullable String currentSubRoom;

    private boolean isMuted = false;

    private final MutableLiveData<RtcEngine> _mEngine = new MutableLiveData<>();
    public LiveData<RtcEngine> mEngine() {
        return _mEngine;
    }

    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();
    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    // RTC User list
    private final MutableLiveData<Set<Integer>> _rtcUserSet = new MutableLiveData<>();
    public LiveData<Set<Integer>> rtcUserList(){ return _rtcUserSet; }

    // 子房间列表
    private final MutableLiveData<List<SubRoomInfo>> _subRoomList = new MutableLiveData<>();
    public LiveData<List<SubRoomInfo>> subRoomList() {
        return _subRoomList;
    }

    public RoomViewModel(Context context, RoomInfo currentRoomInfo) {
        BaseUtil.logD("RoomViewModel-> init:" + currentRoomInfo.toString());
        this.currentRoomInfo = currentRoomInfo;
        fetchAllSubRooms();
        subscribeSubRooms();
        initRTC(context, new IRtcEngineEventHandler() {
            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                BaseUtil.logD("onJoinChannelSuccess:"+channel);
                handleMuteStuff();
//                setupLocalView((ViewGroup) mBinding.dynamicViewFgRoom.flexContainer.getChildAt(0));
            }
            @Override
            public void onError(int err) {
                BaseUtil.logE(RtcEngine.getErrorDescription(err));
            }
            /**
             * {@see <a href="https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler.html?platform=Android#a31b2974a574ec45e62bb768e17d1f49e">}
             */
            @Override
            public void onConnectionStateChanged(int state, int reason) {
                BaseUtil.logD("onConnectionStateChanged:" + state + "," + reason);
            }
            @Override
            public void onUserJoined(int uid, int elapsed) {
                Set<Integer> set = _rtcUserSet.getValue();
                if (set == null) set = new LinkedHashSet<>();
                set.add(uid);
                _rtcUserSet.postValue(set);
            }

            @Override
            public void onUserOffline(int uid, int reason) {
                Set<Integer> set = _rtcUserSet.getValue();
                if (set != null && set.remove(uid)){
                    _rtcUserSet.postValue(set);
                }
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

    @Override
    public void createSubRoom(@NonNull String roomName) {
        _viewStatus.postValue(new ViewStatus.Loading(false));
        SubRoomInfo pendingSubRoom = new SubRoomInfo(roomName);
        HashMap<String, Object> map = RoomUtil.convertObjToHashMap(pendingSubRoom, RoomConstant.gson);
        SyncManager.Instance().getScene(currentRoomInfo.getId()).collection(RoomConstant.globalSubRoom).add(map, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
            }

            @Override
            public void onFail(SyncManagerException e) {
                _viewStatus.postValue(new ViewStatus.Error(e));
            }
        });
    }

    @Override
    public void fetchAllSubRooms() {
        SyncManager.Instance().getScene(currentRoomInfo.getId()).collection(RoomConstant.globalSubRoom).get(new SyncManager.DataListCallback() {
            @Override
            public void onSuccess(List<IObject> result) {

                List<SubRoomInfo> res = new ArrayList<>();
                SubRoomInfo subRoomInfo;

                for (IObject iObject : result) {
                    try {
                        subRoomInfo = iObject.toObject(SubRoomInfo.class);
                    } catch (Exception e) {
                        subRoomInfo = null;
                        BaseUtil.logE(e);
                    }
                    if (subRoomInfo != null && subRoomInfo.getSubRoom() != subRoomInfo.getCreateTime())
                        res.add(subRoomInfo);
                }
                Collections.sort(res);
                _subRoomList.postValue(res);
            }

            @Override
            public void onFail(SyncManagerException e) {
                _subRoomList.postValue(new ArrayList<>());
                // TODO optimized it
                if (!Objects.equals(e.getMessage(), "empty attributes")) {
                    _viewStatus.postValue(new ViewStatus.Error(e));
                }
            }
        });
    }

    @Override
    public void subscribeSubRooms() {
        SyncManager.Instance().getScene(currentRoomInfo.getId()).collection(RoomConstant.globalSubRoom).subcribe(new SyncManager.EventListener() {
            @Override
            public void onCreated(IObject item) {
                try {
                    SubRoomInfo subRoomInfo = item.toObject(SubRoomInfo.class);
                    addSubRoom(subRoomInfo);
                    _viewStatus.postValue(new ViewStatus.Done());
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onUpdated(IObject item) {

            }

            @Override
            public void onDeleted(IObject item) {
                try {
                    deleteSubRoom(item.toObject(SubRoomInfo.class));
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {
                BaseUtil.logE(ex);
            }
        });
    }

    private void addSubRoom(@NonNull SubRoomInfo subRoomInfo) {
        List<SubRoomInfo> res = _subRoomList.getValue();
        if (res == null) res = new ArrayList<>();
        if (!res.contains(subRoomInfo)) {
            res.add(subRoomInfo);
            Collections.sort(res);
            _subRoomList.postValue(res);
        }
    }

    private void deleteSubRoom(@NonNull SubRoomInfo subRoomInfo) {
        List<SubRoomInfo> res = _subRoomList.getValue();
        if (res != null && res.remove(subRoomInfo)) {
            Collections.sort(res);
            _subRoomList.postValue(res);
        }
    }

    //<editor-fold desc="RTC related">
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
                RtcEngine engine = RtcEngine.create(config);
                engine.enableAudio();
                engine.enableVideo();
                _mEngine.postValue(engine);
            } catch (Exception e) {
                e.printStackTrace();
                _viewStatus.postValue(new ViewStatus.Error(e));
                _mEngine.postValue(null);
            }
        }
    }

    public void joinRoom(@NonNull String roomName) {
        if (roomName.equals(currentRoomInfo.getId()))
            currentSubRoom = null;
        else
            currentSubRoom = roomName;

        RtcEngine engine = _mEngine.getValue();
        if (engine != null) {
            Context context = ((RtcEngineImpl) engine).getContext();
            engine.joinChannel(context.getString(R.string.rtc_app_token), currentRoomInfo.getUserId() + roomName, null, Integer.parseInt(RoomConstant.userId));
        }
    }

    public void joinSubRoom(@NonNull String subRoomName) {
        RtcEngine engine = _mEngine.getValue();
        if (engine != null) {
            engine.leaveChannel();
            joinRoom(subRoomName);
        }
    }

    public void setupLocalView(ViewGroup cardView) {
        View view = ((ViewGroup) cardView.getChildAt(0)).getChildAt(0);
        RtcEngine engine = _mEngine.getValue();
        if (engine != null) {
            engine.setupLocalVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(RoomConstant.userId)));
        }
    }

    public void setupRemoteView(ViewGroup cardView, int uid) {
        View view = ((ViewGroup) cardView.getChildAt(0)).getChildAt(0);
        RtcEngine engine = _mEngine.getValue();
        if (engine != null) {
            VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, uid);
            engine.setupRemoteVideo(videoCanvas);
        }
    }

    public void onUserLeft(int uid) {
//        RtcEngine engine = _mEngine.getValue();
//        if (engine != null) {
//            engine.setupRemoteVideo(new VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, uid));
//        }
    }
    //</editor-fold>

    public void mute(boolean shouldMute) {
        isMuted = shouldMute;
        handleMuteStuff();
    }

    private void handleMuteStuff(){
        RtcEngine engine = _mEngine.getValue();
        if (engine != null) {
            engine.muteLocalAudioStream(isMuted);
        }
    }
}
