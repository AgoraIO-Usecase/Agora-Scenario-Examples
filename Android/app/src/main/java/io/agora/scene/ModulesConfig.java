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
//        moduleInfo.add(new ModuleInfo(
//                R.string.app_live_single_host_name,
//                R.string.app_live_single_host_description,
//                R.drawable.app_banner_live_single_host,
//                "io.agora.sample.singlehostlive.RoomListActivity"
//        ));
        // Live PK
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_live_pk_name,
                R.string.app_live_pk_description,
                R.drawable.app_banner_livepk,
                "io.agora.scene.livepk.activity.LivePKListActivity"
        ));
        // BreakoutRoom
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_breakout_room_name,
                R.string.app_breakout_room_description,
                R.drawable.app_banner_breakout_room,
                "io.agora.scene.breakoutroom.ui.MainActivity"
        ));
        // RTEGame
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_live_game_name,
                R.string.app_live_game_description,
                R.drawable.app_banner_live_game,
                "io.agora.scene.rtegame.MainActivity"
        ));
        // OneLive
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_one_live_name,
                R.string.app_one_live_description,
                R.drawable.app_banner_live_shopping,
                "io.agora.scene.onelive.MainActivity"
        ));
        // ComLive
        this.moduleInfo.add(new ModuleInfo(
                R.string.app_com_live_name,
                R.string.app_com_live_description,
                R.drawable.app_banner_live_shopping,
                "io.agora.scene.comlive.MainActivity"
        ));
    }


}
