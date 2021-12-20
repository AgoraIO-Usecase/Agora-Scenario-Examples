package io.agora.scene.rtegame.ui.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.syncmanager.rtm.SceneReference;

public class RoomViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final Context context;
    private final RoomInfo roomInfo;

    public RoomViewModelFactory(@NonNull Context context, @NonNull RoomInfo currentRoom) {
        this.context = context;
        this.roomInfo = currentRoom;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            Constructor<T> constructor = modelClass.getConstructor(Context.class, RoomInfo.class);
            return constructor.newInstance(context, roomInfo);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
    }
}
