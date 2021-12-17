package io.agora.syncmanager.rtm;

import androidx.annotation.NonNull;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public interface IObject {

    <T> T toObject(@NonNull Class<T> valueType);

    String getId();
}
