package io.agora.scene.comlive.ui.create;

import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.bean.LocalUser;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.util.ComLiveUtil;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

public class CreateViewModel extends ViewModel {

    @Nullable
    private final RtcEngineEx rtcEngineEx = GlobalViewModel.rtcEngine;


    private final MutableLiveData<Boolean> _isRoomCreateSuccess = new MutableLiveData<>();

    @NonNull
    public LiveData<Boolean> isRoomCreateSuccess() {
        return _isRoomCreateSuccess;
    }


    public void startPreview(@NonNull SurfaceView surfaceView) {
        if (rtcEngineEx != null) {
            rtcEngineEx.startPreview();
            rtcEngineEx.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN));
        }
    }

    public void stopPreview() {
        if (rtcEngineEx != null)
            rtcEngineEx.stopPreview();
    }

    public void createRoom(@NonNull String roomName) {
        LocalUser localUser = GlobalViewModel.localUser;
        if (localUser != null) {
            RoomInfo pendingRoom = new RoomInfo(roomName, localUser.getUserId());
            Sync.Instance().createScene(ComLiveUtil.getSceneFromRoomInfo(pendingRoom), new Sync.Callback() {
                @Override
                public void onSuccess() {
                    GlobalViewModel.currentRoom = pendingRoom;
                    _isRoomCreateSuccess.postValue(true);
                }

                @Override
                public void onFail(SyncManagerException exception) {
                    _isRoomCreateSuccess.setValue(false);
                }
            });
        } else {
            _isRoomCreateSuccess.setValue(false);
        }
    }
}
