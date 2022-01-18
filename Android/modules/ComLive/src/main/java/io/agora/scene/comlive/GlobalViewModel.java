package io.agora.scene.comlive;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;

import io.agora.scene.comlive.bean.LocalUser;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.util.ComLiveConstants;
import io.agora.scene.comlive.util.ComLiveUtil;
import io.agora.scene.comlive.util.Event;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

@Keep
public class GlobalViewModel extends AndroidViewModel {
    @Nullable
    public static LocalUser localUser;

    private final MutableLiveData<Event<Boolean>> _isRTMInit = new MutableLiveData<>();

    @NonNull
    public LiveData<Event<Boolean>> isRTMInit() {
        return _isRTMInit;
    }

    public final MutableLiveData<Event<RoomInfo>> roomInfo = new MutableLiveData<>();

    public GlobalViewModel(@NonNull Application application) {
        super(application);
        GlobalViewModel.localUser = checkLocalOrGenerate(application.getApplicationContext());
        initSyncManager(application.getApplicationContext());
    }
    /**
     * 本地存在==> 本地生成
     * 本地不存在==> 随机生成
     */
    @NonNull
    public static LocalUser checkLocalOrGenerate(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences("sp_rte_game", Context.MODE_PRIVATE);
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

    public void createRoom(@NonNull RoomInfo room) {
        Sync.Instance().createScene(ComLiveUtil.getSceneFromRoomInfo(room), new Sync.Callback() {

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
    private void initSyncManager(@NonNull Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", ComLiveConstants.globalChannel);
        Sync.Instance().init(context, map, new Sync.Callback() {
            @Override
            public void onSuccess() {
                _isRTMInit.postValue(new Event<>(true));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                _isRTMInit.postValue(new Event<>(false));
            }
        });
    }

    //</editor-fold>

}
