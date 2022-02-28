package io.agora.scene.breakoutroom.ui.room;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.agora.rtc2.RtcEngineEx;
import io.agora.scene.breakoutroom.bean.RoomInfo;

public class RoomViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    @NonNull
    private final RoomInfo currentRoom;
    @NonNull
    private final RtcEngineEx rtcEngineEx;

    public RoomViewModelFactory(@NonNull RoomInfo currentRoom, @NonNull RtcEngineEx rtcEngineEx) {
        this.currentRoom = currentRoom;
        this.rtcEngineEx = rtcEngineEx;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            Constructor<T> constructor = modelClass.getConstructor(RoomInfo.class, RtcEngineEx.class);
            return constructor.newInstance(currentRoom, rtcEngineEx);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
    }
}
