package io.agora.scene.comlive.util;

import androidx.lifecycle.Observer;

public class EventObserver<T> implements Observer<Event<T>> {
    private final OnChangedListener<T> listener;

    public EventObserver(OnChangedListener<T> listener) {
        this.listener = listener;
    }

    @Override
    public void onChanged(Event<T> event) {
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
