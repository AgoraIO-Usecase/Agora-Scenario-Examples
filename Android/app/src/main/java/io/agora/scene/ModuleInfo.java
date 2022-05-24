package io.agora.scene;

import android.app.Activity;

public class ModuleInfo {
    public int nameRes;
    public int descriptionRes;
    public int bgImageRes;
    public Class<? extends Activity> targetActivity;

    public ModuleInfo(int nameRes, int descriptionRes, int bgImageRes, Class<? extends Activity> targetActivity) {
        this.nameRes = nameRes;
        this.descriptionRes = descriptionRes;
        this.bgImageRes = bgImageRes;
        this.targetActivity = targetActivity;
    }

}
