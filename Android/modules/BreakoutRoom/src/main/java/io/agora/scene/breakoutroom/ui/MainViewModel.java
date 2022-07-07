package io.agora.scene.breakoutroom.ui;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomConstant;
import io.agora.syncmanager.rtm.Sync;

/**
 * @author lq
 */
public class MainViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isRTMInit = new MutableLiveData<>();
    public LiveData<Boolean> isRTMInit(){
        return _isRTMInit;
    }

    public void initSyncManager(Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", RoomConstant.globalChannel);
        Sync.Instance().init(context, map, null);
        // FIXME
        new Thread(() -> {
            try {
                Thread.sleep(3000);
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

}
