package io.agora.scene.breakoutroom.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomConstant;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 * @author lq
 */
public class MainViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isRTMInit = new MutableLiveData<>();

    @NonNull
    public LiveData<Boolean> isRTMInit() {
        return _isRTMInit;
    }

    public MainViewModel(@NonNull Context context) {
        initSyncManager(context);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Sync.Instance().destroy();
    }

    private void initSyncManager(@NonNull Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", RoomConstant.globalChannel);
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

    public void leaveRoom() {
//        if (_scene.getValue() != null) {
//            // TODO leave channel
//            _scene.setValue(null);
//        }
    }

}
