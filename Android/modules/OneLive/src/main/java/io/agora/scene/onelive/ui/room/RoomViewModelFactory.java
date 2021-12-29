package io.agora.scene.onelive.ui.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.agora.scene.onelive.bean.LocalUser;
import io.agora.scene.onelive.bean.RoomInfo;

public class RoomViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final Context context;
    private final LocalUser localUser;
    private final RoomInfo roomInfo;

    public RoomViewModelFactory(@NonNull Context context, @NonNull LocalUser localUser, @NonNull RoomInfo currentRoom) {
        this.context = context;
        this.localUser = localUser;
        this.roomInfo = currentRoom;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            Constructor<T> constructor = modelClass.getConstructor(Context.class, LocalUser.class, RoomInfo.class);
            return constructor.newInstance(context, localUser, roomInfo);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
    }
}
