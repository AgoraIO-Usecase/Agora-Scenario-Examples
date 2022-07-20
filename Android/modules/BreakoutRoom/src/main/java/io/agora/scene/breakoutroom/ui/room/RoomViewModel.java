package io.agora.scene.breakoutroom.ui.room;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.agora.example.base.BaseUtil;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.internal.RtcEngineImpl;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomConstant;
import io.agora.scene.breakoutroom.RoomUtil;
import io.agora.scene.breakoutroom.ViewStatus;
import io.agora.scene.breakoutroom.bean.RoomInfo;
import io.agora.scene.breakoutroom.bean.SubRoomInfo;
import io.agora.scene.breakoutroom.repo.RoomApi;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SceneReference;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 *
 */
public class RoomViewModel extends ViewModel implements RoomApi {

    @NonNull
    public RoomInfo currentRoomInfo;
    @Nullable
    public String currentSubRoom;
    private SceneReference currentSceneRef = null;

    private final RtcEngineEx rtcEngineEx;

    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();

    @NonNull
    public MutableLiveData<Boolean> isMicEnabled = new MutableLiveData<>(true);
    @NonNull
    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    // RTC User list
    private final MutableLiveData<Set<Integer>> _rtcUserSet = new MutableLiveData<>();

    @NonNull
    public LiveData<Set<Integer>> rtcUserList() {
        return _rtcUserSet;
    }

    // 子房间列表
    private final MutableLiveData<List<SubRoomInfo>> _subRoomList = new MutableLiveData<>();

    @NonNull
    public LiveData<List<SubRoomInfo>> subRoomList() {
        return _subRoomList;
    }

    @NonNull
    private final IRtcEngineEventHandler roomEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            BaseUtil.logD("onJoinChannelSuccess:" + channel);
            Boolean enabled = isMicEnabled.getValue();
            enableMic(enabled != Boolean.FALSE);
//                setupLocalView((ViewGroup) mBinding.dynamicViewFgRoom.flexContainer.getChildAt(0));
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
            if (set != null && set.remove(uid)) {
                _rtcUserSet.postValue(set);
            }
        }
    };

    public RoomViewModel(@NonNull RoomInfo currentRoomInfo, @NonNull RtcEngineEx rtcEngineEx) {
        BaseUtil.logD("RoomViewModel-> init:" + currentRoomInfo);
        this.currentRoomInfo = currentRoomInfo;
        this.rtcEngineEx = rtcEngineEx;

        configRTC();
        configRTM();

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        rtcEngineEx.removeHandler(roomEventHandler);
        rtcEngineEx.leaveChannel();
        if (currentSceneRef != null)
            currentSceneRef.unsubscribe(null);
    }

    private void configRTC() {
        rtcEngineEx.enableAudio();
        rtcEngineEx.enableVideo();
        rtcEngineEx.startPreview();
        rtcEngineEx.addHandler(roomEventHandler);
        joinRTCRoom(currentRoomInfo.getId());
    }

    private void configRTM() {
        Sync.Instance().joinScene(currentRoomInfo.getId(), new Sync.JoinSceneCallback() {
            @Override
            public void onSuccess(SceneReference sceneReference) {
                currentSceneRef = sceneReference;
                fetchAllSubRooms();
                subscribeSubRooms();
            }

            @Override
            public void onFail(SyncManagerException exception) {

            }
        });
    }

    //<editor-fold desc="SyncManager related">

    /**
     * Just make a add-OP, the result will be in the callback of {@link this#subscribeSubRooms()}
     */
    @Override
    public void createSubRoom(@NonNull String roomName) {
        if (currentSceneRef != null) {
            _viewStatus.postValue(new ViewStatus.Loading(false));
            SubRoomInfo pendingSubRoom = new SubRoomInfo(roomName);
            HashMap<String, Object> map = RoomUtil.convertObjToHashMap(pendingSubRoom, RoomConstant.gson);
            currentSceneRef.collection(RoomConstant.globalSubRoom).add(map, new Sync.DataItemCallback() {
                @Override
                public void onSuccess(IObject result) {
                    pendingSubRoom.setId(result.getId());
                }

                @Override
                public void onFail(SyncManagerException e) {
                    _viewStatus.postValue(new ViewStatus.Error(e));
                }
            });
        }
    }

    @Override
    public void fetchAllSubRooms() {
        if (currentSceneRef != null)
            currentSceneRef.collection(RoomConstant.globalSubRoom).get(new Sync.DataListCallback() {
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
                        if (subRoomInfo != null && !subRoomInfo.getSubRoom().equals(subRoomInfo.getCreateTime()))
                            res.add(subRoomInfo);
                    }
                    Collections.sort(res);
                    BaseUtil.logD("res:"+res.size());
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

    /**
     * Subscribe the SubRoom collection
     * Every time it added or deleted notify the view
     */
    @Override
    public void subscribeSubRooms() {
        if (currentSceneRef != null)
            currentSceneRef.collection(RoomConstant.globalSubRoom).subscribe(new Sync.EventListener() {
                @Override
                public void onCreated(IObject item) {
                    try {
                        SubRoomInfo subRoomInfo = item.toObject(SubRoomInfo.class);
                        onSubRoomAdded(subRoomInfo);
                        _viewStatus.postValue(new ViewStatus.Done());
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onUpdated(IObject item) {
                    try {
                        SubRoomInfo subRoomInfo = item.toObject(SubRoomInfo.class);
                        onSubRoomAdded(subRoomInfo);
                        _viewStatus.postValue(new ViewStatus.Done());
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onDeleted(IObject item) {
                    try {
                        onSubRoomDeleted(item.getId());
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onSubscribeError(SyncManagerException ex) {
                    BaseUtil.logE(ex);
                }
            });
    }

    private void onSubRoomAdded(@NonNull SubRoomInfo subRoomInfo) {
        List<SubRoomInfo> res = _subRoomList.getValue();
        if (res == null) res = new ArrayList<>();
        if (!res.contains(subRoomInfo)) {
            res.add(subRoomInfo);
            Collections.sort(res);
            _subRoomList.postValue(res);
        }
    }

    private void onSubRoomDeleted(String id) {
        List<SubRoomInfo> res = _subRoomList.getValue();
        if(res != null){
            Iterator<SubRoomInfo> iterator = res.iterator();
            while (iterator.hasNext()){
                SubRoomInfo next = iterator.next();
                if(id.equals(next.getId())){
                    iterator.remove();
                    break;
                }
            }
            Collections.sort(res);
            _subRoomList.postValue(res);
        }
    }
    //</editor-fold>

    //<editor-fold desc="RTC related">

    @Override
    public void joinRTCRoom(@NonNull String roomName) {
        if (roomName.equals(currentRoomInfo.getId()))
            currentSubRoom = null;
        else
            currentSubRoom = roomName;

        Context context = ((RtcEngineImpl) rtcEngineEx).getContext();
//            engine.joinChannel(context.getString(R.string.rtc_app_token), currentRoomInfo.getUserId() + roomName, null, Integer.parseInt(RoomConstant.userId));
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        rtcEngineEx.joinChannel(context.getString(R.string.rtc_app_token), currentRoomInfo.getUserId() + roomName, Integer.parseInt(RoomConstant.userId), options);
    }

    public void joinSubRoom(@NonNull String subRoomName) {
        rtcEngineEx.leaveChannel();
        joinRTCRoom(subRoomName);
    }

    public void setupLocalView(@NonNull ViewGroup cardView) {
        View view = ((ViewGroup) cardView.getChildAt(0)).getChildAt(0);
        rtcEngineEx.setupLocalVideo(new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, Integer.parseInt(RoomConstant.userId)));
    }

    public void setupRemoteView(@NonNull ViewGroup cardView, int uid) {
        View view = ((ViewGroup) cardView.getChildAt(0)).getChildAt(0);
        VideoCanvas videoCanvas = new VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, uid);
        rtcEngineEx.setupRemoteVideo(videoCanvas);
    }

    public void onUserLeft(int uid) {
//        RtcEngine engine = _mEngine.getValue();
//        if (engine != null) {
//            engine.setupRemoteVideo(new VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, uid));
//        }
    }
    //</editor-fold>

    public void enableMic(boolean enable) {
        isMicEnabled.postValue(enable);
        rtcEngineEx.muteLocalAudioStream(!enable);
    }
}
