package io.agora.scene.rtegame.util;

import androidx.annotation.Nullable;

public class Event<T> {
    private boolean hasBeenHandled = false;

    private final T content;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T getContentIfNotHandled(){
        if (hasBeenHandled) return null;
        else {
            hasBeenHandled = true;
            return content;
        }
    }

    public T peekContent(){
        return content;
    }

    public boolean hasBeenHandled(){
        return hasBeenHandled;
    }
}
