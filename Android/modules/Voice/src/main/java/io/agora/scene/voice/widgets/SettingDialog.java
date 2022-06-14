package io.agora.scene.voice.widgets;

import android.content.Context;

import androidx.annotation.NonNull;

import io.agora.scene.voice.R;
import io.agora.scene.voice.RtcManager;
import io.agora.uiwidget.function.LiveToolsDialog;

public class SettingDialog extends LiveToolsDialog {
    public static final ToolItem ITEM_MONITOR = new ToolItem(R.string.voice_setting_monitor, R.drawable.voice_setting_ic_monitor);
    public static final ToolItem ITEM_BACKGROUND = new ToolItem(R.string.voice_setting_background, R.drawable.voice_setting_ic_background);
    public static final ToolItem ITEM_BACKGROUND_MUSIC = new ToolItem(R.string.voice_setting_background_music, R.drawable.voice_setting_ic_music);
    public static final ToolItem ITEM_STATISTICS = new ToolItem(R.string.voice_setting_statistics, R.drawable.voice_setting_ic_data);

    public static LiveToolsDialog createDialog(Context context, RtcManager rtcManager, Runnable showBackgroundDialog, Runnable showBGMusicDialog){
        return new SettingDialog(context)
                .addToolItem(TOOL_ITEM_SPEAKER, false, (view, item) -> rtcManager.enableLocalAudio(item.activated))
                .addToolItem(ITEM_MONITOR, false, (view, item) -> rtcManager.enableEarMonitoring(item.activated))
                .addToolItem(ITEM_BACKGROUND, false, (view, item) -> {
                    if(showBackgroundDialog != null){
                        showBackgroundDialog.run();
                    }
                })
                .addToolItem(ITEM_BACKGROUND_MUSIC, false, (view, item) -> {
                    if(showBGMusicDialog != null){
                        showBGMusicDialog.run();
                    }
                })
                //.addToolItem(ITEM_STATISTICS, false, listener)
                ;
    }

    private SettingDialog(@NonNull Context context) {
        super(context);
    }

}
