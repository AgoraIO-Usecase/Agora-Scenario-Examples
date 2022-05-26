package io.agora.scene.pklive.util;

import java.util.List;

public interface DataListCallback<T> {
    void onSuccess(List<T> data);
}
