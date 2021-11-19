package io.agora.sample.rtegame;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.agora.sample.rtegame.bean.LocalUser;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.repo.RoomCreateApi;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.SyncManager;
import io.agora.syncmanager.rtm.SyncManagerException;

public class GlobalViewModel extends ViewModel implements RoomCreateApi {

    private final MutableLiveData<LocalUser> _user = new MutableLiveData<>();
    public LiveData<LocalUser> user(){
        return _user;
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
}
