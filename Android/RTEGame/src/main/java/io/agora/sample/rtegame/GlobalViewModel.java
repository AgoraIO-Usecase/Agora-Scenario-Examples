package io.agora.sample.rtegame;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomCreateApi;
import io.agora.sample.rtegame.util.GameConstants;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

public class GlobalViewModel extends ViewModel implements RoomCreateApi {

    private final MutableLiveData<LocalUser> _user = new MutableLiveData<>();
    public LiveData<LocalUser> user(){
        return _user;
    }


    private final MutableLiveData<Boolean> _isRTMInit = new MutableLiveData<>();
    public LiveData<Boolean> isRTMInit(){
        return _isRTMInit;
    }

    public final MutableLiveData<RoomInfo> roomInfo = new MutableLiveData<>();

    public GlobalViewModel() {
        _user.setValue(new LocalUser());
    }

    @Override
    public LiveData<RoomInfo> createRoom(@NonNull RoomInfo room) {
        SyncManager.Instance().joinScene(GameUtil.getSceneFromRoomInfo(room), new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                roomInfo.postValue(room);
            }

            @Override
            public void onFail(SyncManagerException exception) {
                roomInfo.postValue(null);
            }
        });
        return roomInfo;
    }

    public LiveData<Boolean> initSyncManager(Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", GameConstants.globalChannel);
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
        return _isRTMInit;
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

}
