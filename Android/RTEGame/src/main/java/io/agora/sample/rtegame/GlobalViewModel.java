package io.agora.sample.rtegame;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomCreateApi;
import io.agora.sample.rtegame.util.Event;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

public class GlobalViewModel extends ViewModel implements RoomCreateApi {

    private final MutableLiveData<LocalUser> _user = new MutableLiveData<>();

    @NonNull
    public LiveData<LocalUser> user() {
        return _user;
    }

    private final MutableLiveData<Boolean> _isRTMInit = new MutableLiveData<>();

    @NonNull
    public LiveData<Boolean> isRTMInit() {
        return _isRTMInit;
    }

    public final MutableLiveData<Event<RoomInfo>> roomInfo = new MutableLiveData<>();

    public GlobalViewModel() {
        LocalUser localUser = checkLocalOrGenerate();
        GameApplication.getInstance().user = localUser;
        _user.setValue(localUser);
    }

    /**
     * 本地存在==> 本地生成
     * 本地不存在==> 随机生成
     */
    private LocalUser checkLocalOrGenerate() {
        SharedPreferences sp = GameApplication.getInstance().getSharedPreferences("sp_rte_game", Context.MODE_PRIVATE);
        String userId = sp.getString("id", "-1");

        boolean isValidUser = true;

        try {
            int i = Integer.parseInt(userId);
            if (i == -1) isValidUser = false;
        } catch (NumberFormatException e) {
            isValidUser = false;
        }

        LocalUser localUser;
        if (isValidUser)
            localUser = new LocalUser(userId);
        else
            localUser = new LocalUser();
        sp.edit().putString("id", localUser.getUserId()).apply();

        return localUser;
    }

    public void clearRoomInfo() {
        Event<RoomInfo> roomInfoEvent = new Event<>(null);
        roomInfoEvent.getContentIfNotHandled();
        roomInfo.setValue(roomInfoEvent);
    }

    @Override
    public void createRoom(@NonNull RoomInfo room) {
        Sync.Instance().createScene(GameUtil.getSceneFromRoomInfo(room), new Sync.Callback() {

            @Override
            public void onSuccess() {
                roomInfo.postValue(new Event<>(room));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                roomInfo.postValue(new Event<>(null));
            }
        });
    }

    //<editor-fold desc="SyncManager">
    public void initSyncManager(@NonNull Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", GameConstants.globalChannel);
        Sync.Instance().init(context, map, new Sync.Callback() {
            @Override
            public void onSuccess() {
                _isRTMInit.postValue(true);
            }

            @Override
            public void onFail(SyncManagerException exception) {
                _isRTMInit.postValue(false);
            }
        });
    }

    public void destroySyncManager() {
//        SyncManager.Instance()
    }

    public void leaveRoom() {
//        if (_scene.getValue() != null) {
//            // TODO leave channel
//            _scene.setValue(null);
//        }
    }
    //</editor-fold>

}
