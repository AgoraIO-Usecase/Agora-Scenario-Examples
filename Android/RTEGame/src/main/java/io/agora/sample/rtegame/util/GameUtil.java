package io.agora.sample.rtegame.util;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Random;

import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.syncmanager.rtm.Scene;

public class GameUtil {
    private static final String[] nameList = {
            "一马当先",
            "二姓之好",
            "三生有幸",
            "四分五裂",
            "五光十色",
            "六神无主",
            "七上八下",
            "八面玲珑",
            "九霄云外",
            "十全十美",
    };
    @DrawableRes
    public static int getBgdFromRoomBgdId(String bgdId){
        return R.mipmap.ic_launcher;
    }
    @DrawableRes
    public static int getAvatarFromUserId(String userId){
        return R.mipmap.ic_launcher;
    }

    public static Scene getSceneFromRoomInfo(@NonNull RoomInfo roomInfo){
        Scene scene = new Scene();
        scene.setId(roomInfo.getRoomId());
        scene.setUserId(roomInfo.getUserId());

        HashMap<String, String> map = new HashMap<>();
        map.put("backgroundId", roomInfo.getBackgroundId());
        scene.setProperty(map);
        return scene;
    }

    public static String getRandomRoomName(){
        return getRandomRoomName(new Random().nextInt(10));
    }
    public static String getRandomRoomName(int number){
        return nameList[number % 10];
    }

}
