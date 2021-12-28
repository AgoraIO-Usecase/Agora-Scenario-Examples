package io.agora.scene;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class ModuleInfo {
    public final int nameRes;
    public final int descriptionRes;
    public final int bgImageRes;
    @NonNull
    public String mLaunchClassName;

    public ModuleInfo(int nameRes, int descriptionRes, int bgImageRes, @NonNull String launchClassName) {
        this.nameRes = nameRes;
        this.descriptionRes = descriptionRes;
        this.bgImageRes = bgImageRes;
        this.mLaunchClassName = launchClassName;
    }

}
