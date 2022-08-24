package io.agora.scene;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ModulesConfig {

    @NonNull
    public static volatile ModulesConfig instance = new ModulesConfig();

    public final List<ModuleInfo> moduleInfo = new ArrayList<>();

    private ModulesConfig() {
        moduleInfo.add(new ModuleInfo(R.string.app_type_social_entertainment));
        // Single Live Host
        moduleInfo.add(new ModuleInfo(
                R.string.app_live_single_host_name,
                R.drawable.app_banner_video_call,
                "io.agora.scene.singlehostlive.RoomListActivity"
        ));
        // Video call
        moduleInfo.add(new ModuleInfo(
                R.string.app_video_call_name,
                R.drawable.app_banner_voice_call,
                "io.agora.scene.videocall.RoomListActivity"
        ));
        // Voice
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_voice_name,
                R.drawable.app_banner_chat_room,
                "io.agora.scene.voice.RoomListActivity"
        ));
        // Live PK
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_live_pk_name,
                R.drawable.app_banner_livepk,
                "io.agora.scene.pklive.RoomListActivity"
        ));
        // Multi Call
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_multi_call,
                R.drawable.app_banner_multi_call,
                "io.agora.scene.multicall.RoomListActivity"
        ));
        // Interactive Blog
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_interactive_blog,
                R.drawable.app_banner_interactor_blog,
                "io.agora.scene.interactiveblog.RoomListActivity"
        ));

        moduleInfo.add(new ModuleInfo(R.string.app_type_education));
        // BreakoutRoom
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_breakout_room_name,
                R.drawable.app_banner_breakout_room,
                "io.agora.scene.breakoutroom.ui.MainActivity"
        ));
    }


}
