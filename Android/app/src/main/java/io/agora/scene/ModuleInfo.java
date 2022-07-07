package io.agora.scene;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class ModuleInfo {
    public final int type;

    public final int titleRes;

    public final int nameRes;
    public final int bgImageRes;
    @NonNull
    public final String launchClassName;

    public ModuleInfo(int titleRes) {
        this.type = ModuleType.title;
        this.titleRes = titleRes;
        this.nameRes = 0;
        this.bgImageRes = 0;
        this.launchClassName = "";
    }

    public ModuleInfo(int nameRes, int bgImageRes, @NonNull String launchClassName) {
        this.type = ModuleType.content;
        this.titleRes = 0;
        this.nameRes = nameRes;
        this.bgImageRes = bgImageRes;
        this.launchClassName = launchClassName;
    }


    public @interface ModuleType{
        int title = 0;
        int content = 1;
    }
}
