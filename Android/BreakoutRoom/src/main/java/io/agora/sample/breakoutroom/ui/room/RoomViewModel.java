package io.agora.sample.breakoutroom.ui.room;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;

import android.content.Context;
import android.graphics.Color;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
import io.agora.sample.breakoutroom.ui.MainViewModel;
import io.agora.syncmanager.rtm.IObject;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 *
 */
public class RoomViewModel extends ViewModel implements RoomApi {

    public RoomInfo currentRoomInfo;

    public @Nullable
    String currentSubRoom;

    private final MutableLiveData<RtcEngine> _mEngine = new MutableLiveData<>();

    public LiveData<RtcEngine> mEngine() {
        return _mEngine;
    }

    // UI状态
    private final MutableLiveData<ViewStatus> _viewStatus = new MutableLiveData<>();

    public LiveData<ViewStatus> viewStatus() {
        return _viewStatus;
    }

    // 子房间列表
    private final MutableLiveData<List<SubRoomInfo>> _subRoomList = new MutableLiveData<>();

    public LiveData<List<SubRoomInfo>> subRoomList() {
        return _subRoomList;
    }

    public RoomViewModel(RoomInfo currentRoomInfo) {
        this.currentRoomInfo = currentRoomInfo;
        fetchAllSubRooms();
        subscribeSubRooms();
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
    /**
     * @param roomName
     */
    @Override
    public void createSubRoom(@NonNull String roomName) {
        _viewStatus.postValue(new ViewStatus.Loading());
        SubRoomInfo pendingSubRoom = new SubRoomInfo(roomName);
        HashMap<String, Object> map = RoomUtil.convertObjToHashMap(pendingSubRoom, RoomConstant.gson);
        SyncManager.Instance().getScene(currentRoomInfo.getId()).collection(RoomConstant.globalSubRoom).add(map, new SyncManager.DataItemCallback() {
            @Override
            public void onSuccess(IObject result) {
//                List<SubRoomInfo> list = _subRoomList.getValue();
//                if (list == null) {
//                    list = new ArrayList<>();
//                }
//                list.add(pendingSubRoom);

//                _subRoomList.postValue(list);
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
                _subRoomList.postValue(res);
            }

            @Override
            public void onFail(SyncManagerException e) {
                _subRoomList.postValue(new ArrayList<>());
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
                } catch (Exception ignored) { }
            }

            @Override
            public void onUpdated(IObject item) {

            }

            @Override
            public void onDeleted(IObject item) {

                try {
                    deleteSubRoom(item.toObject(SubRoomInfo.class));
                } catch (Exception ignored) { }
            }

            @Override
            public void onSubscribeError(SyncManagerException ex) {
                BaseUtil.logE(ex);
            }
        });
    }

    private void addSubRoom(@NonNull SubRoomInfo subRoomInfo){
        List<SubRoomInfo> res = _subRoomList.getValue();
        if (res == null) res = new ArrayList<>();
        if(!res.contains(subRoomInfo)) {
            res.add(subRoomInfo);
            _subRoomList.postValue(res);
        }
    }

    private void deleteSubRoom(@NonNull SubRoomInfo subRoomInfo){
        List<SubRoomInfo> res = _subRoomList.getValue();
        if (res != null)
            res.remove(subRoomInfo);
        _subRoomList.postValue(res);
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
            engine.joinChannel(context.getString(R.string.rtc_app_token), roomName, null, Integer.parseInt(RoomConstant.userId));
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

    public static CardView getChildVideoCardView(@NonNull Context context, int uid) {
        CardView cardView = new CardView(context);

        // title
        TextView titleText = new TextView(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = BOTTOM;
        lp.bottomMargin = (int) BaseUtil.dp2px(12);
        titleText.setLayoutParams(lp);
        titleText.setGravity(CENTER | BOTTOM);
        titleText.setText(context.getString(R.string.user_name_format, uid));
        titleText.setTextColor(uid == Integer.parseInt(RoomConstant.userId) ? Color.RED : Color.WHITE);


        // container
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextureView textureView = RtcEngine.CreateTextureView(context);

        frameLayout.addView(textureView);
        frameLayout.addView(titleText);


        cardView.setRadius(BaseUtil.dp2px(16));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setTag(uid);

        cardView.addView(frameLayout);
        return cardView;
    }

    public void mute(boolean shouldMute) {
        RtcEngine engine = _mEngine.getValue();
        if (engine != null) {
            engine.muteLocalAudioStream(shouldMute);
        }
    }
}
