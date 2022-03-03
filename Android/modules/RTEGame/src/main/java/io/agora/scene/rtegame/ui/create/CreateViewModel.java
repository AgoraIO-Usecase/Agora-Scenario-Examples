package io.agora.scene.rtegame.ui.create;

import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.rtegame.GlobalViewModel;
import io.agora.scene.rtegame.bean.LocalUser;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.util.Event;
import io.agora.scene.rtegame.util.GameUtil;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

public class CreateViewModel extends ViewModel {

    @Nullable
    private final RtcEngineEx rtcEngineEx = GlobalViewModel.rtcEngine;

    private final MutableLiveData<Event<Boolean>> _isRoomCreateSuccess = new MutableLiveData<>();

    @NonNull
    public LiveData<Event<Boolean>> isRoomCreateSuccess() {
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
            Sync.Instance().createScene(GameUtil.getSceneFromRoomInfo(pendingRoom), new Sync.Callback() {
                @Override
                public void onSuccess() {
                    GlobalViewModel.currentRoom = pendingRoom;
                    _isRoomCreateSuccess.postValue(new Event<>(true));
                }

                @Override
                public void onFail(SyncManagerException exception) {
                    _isRoomCreateSuccess.setValue(new Event<>(false));
                }
            });
        } else {
            _isRoomCreateSuccess.setValue(new Event<>(false));
        }
    }
}
