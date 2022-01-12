package io.agora.scene.comlive.bean;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class AppServerResult<T> {
    @NonNull
    private final T result;

    public AppServerResult(@NonNull T result) {
        this.result = result;
    }

    @NonNull
    public T getResult() {
        return result;
    }
}
