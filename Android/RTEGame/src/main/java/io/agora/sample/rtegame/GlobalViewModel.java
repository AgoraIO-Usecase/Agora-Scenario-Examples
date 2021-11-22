package io.agora.sample.rtegame;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceDataStore;

import androidx.annotation.NonNull;
import androidx.datastore.core.DataStore;
import androidx.datastore.core.DataStoreFactory;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.prefs.PreferencesFactory;

import io.agora.example.BaseApplication;
import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomCreateApi;
import io.agora.sample.rtegame.util.Event;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

public class GlobalViewModel extends ViewModel implements RoomCreateApi {

    private final MutableLiveData<LocalUser> _user = new MutableLiveData<>();
    public LiveData<LocalUser> user(){
        return _user;
    }
    public LocalUser getLocalUser(){
        return _user.getValue();
    }

    private final MutableLiveData<Boolean> _isRTMInit = new MutableLiveData<>();
    public LiveData<Boolean> isRTMInit(){
        return _isRTMInit;
    }

    public final MutableLiveData<Event<RoomInfo>> roomInfo = new MutableLiveData<>();

    public GlobalViewModel() {
        LocalUser localUser = checkLocalOrGenerate();
        _user.setValue(localUser);
    }

    private LocalUser checkLocalOrGenerate() {
        SharedPreferences sp = BaseApplication.getInstance().getSharedPreferences("sp_rte_game", Context.MODE_PRIVATE);
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

    public void clearRoomInfo(){
        Event<RoomInfo> roomInfoEvent = new Event<>(null);
        roomInfoEvent.getContentIfNotHandled();
        roomInfo.setValue(roomInfoEvent);
    }

    @Override
    public void createRoom(@NonNull RoomInfo room) {
        SyncManager.Instance().joinScene(GameUtil.getSceneFromRoomInfo(room), new SyncManager.Callback() {
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
    public void initSyncManager(Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", GameConstants.globalChannel);
        BaseUtil.logD("initSyncManager");
        SyncManager.Instance().init(context, map);
        // FIXME
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _isRTMInit.postValue(true);
        }).start();
    }

    public void destroySyncManager(){
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
