package io.agora.scene;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ModulesConfig {

    @NonNull
    public static volatile ModulesConfig instance = new ModulesConfig();

    public final List<ModuleInfo> moduleInfo = new ArrayList<>();

    private ModulesConfig() {
        // Single Live Host
        moduleInfo.add(new ModuleInfo(
                R.string.app_live_single_host_name,
                R.string.app_live_single_host_description,
                R.drawable.app_banner_live_single_host,
                "io.agora.scene.singlehostlive.RoomListActivity"
        ));
        // Live PK
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_live_pk_name,
                R.string.app_live_pk_description,
                R.drawable.app_banner_livepk,
                "io.agora.scene.pklive.RoomListActivity"
        ));
        // Live PK by CDN
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_live_pk_cdn_name,
                R.string.app_live_pk_cdn_description,
                R.drawable.app_banner_livepk,
                "io.agora.scene.pklivebycdn.RoomListActivity"
        ));
        // BreakoutRoom
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_breakout_room_name,
                R.string.app_breakout_room_description,
                R.drawable.app_banner_breakout_room,
                "io.agora.scene.breakoutroom.ui.MainActivity"
        ));
        // Club
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_club_name,
                R.string.app_club_description,
                R.drawable.app_banner_breakout_room,
                "io.agora.scene.club.MainActivity"
        ));
        // Voice
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_voice_name,
                R.string.app_voice_description,
                R.drawable.app_banner_breakout_room,
                "io.agora.scene.voice.RoomListActivity"
        ));
    }


}
