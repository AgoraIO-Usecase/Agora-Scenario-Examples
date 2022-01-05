package io.agora.scene.onelive.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

public class EventObserver<T> implements Observer<Event<T>> {
    private final OnChangedListener<T> listener;

    public EventObserver(@NonNull OnChangedListener<T> listener) {
        this.listener = listener;
    }

    @Override
    public void onChanged(@Nullable Event<T> event) {
        if (event != null) {
            T contentIfNotHandled = event.getContentIfNotHandled();
            if (contentIfNotHandled != null)
                listener.onChanged(contentIfNotHandled);
        }
    }

    public interface OnChangedListener<T> {
        void onChanged(T data);
    }
}
