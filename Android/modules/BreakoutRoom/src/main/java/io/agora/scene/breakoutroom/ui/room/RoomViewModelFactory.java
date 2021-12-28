package io.agora.scene.breakoutroom.ui.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.agora.scene.breakoutroom.bean.RoomInfo;

public class RoomViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final Context context;
    private final RoomInfo currentRoom;

    public RoomViewModelFactory(Context context, RoomInfo currentRoom) {
        this.context = context;
        this.currentRoom = currentRoom;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            Constructor<T> constructor = modelClass.getConstructor(Context.class, RoomInfo.class);
            return constructor.newInstance(context, currentRoom);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
    }
}
