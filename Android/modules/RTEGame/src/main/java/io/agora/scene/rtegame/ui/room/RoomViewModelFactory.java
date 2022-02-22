package io.agora.scene.rtegame.ui.room;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.agora.rtc2.RtcEngineEx;
import io.agora.scene.rtegame.bean.LocalUser;
import io.agora.scene.rtegame.bean.RoomInfo;

public class RoomViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final RoomInfo roomInfo;
    private final LocalUser localUser;
    private final RtcEngineEx rtcEngineEx;

    public RoomViewModelFactory(@NonNull RoomInfo currentRoom, @NonNull LocalUser localUser, @NonNull RtcEngineEx rtcEngineEx) {
        this.roomInfo = currentRoom;
        this.localUser = localUser;
        this.rtcEngineEx = rtcEngineEx;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            Constructor<T> constructor = modelClass.getConstructor(roomInfo.getClass(),localUser.getClass(), RtcEngineEx.class);
            return constructor.newInstance(roomInfo, localUser, rtcEngineEx);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
    }
}
