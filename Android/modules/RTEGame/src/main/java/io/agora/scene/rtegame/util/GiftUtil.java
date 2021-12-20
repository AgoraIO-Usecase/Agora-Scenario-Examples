package io.agora.scene.rtegame.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.scene.rtegame.R;
import io.agora.scene.rtegame.bean.GiftInfo;

public class GiftUtil {
    @NonNull
    public static String getGiftResNameById(@NonNull Context context, int id){
        return context.getResources().getStringArray(R.array.game_gift_name_list_eng)[id];
    }

    @NonNull
    public static String getGiftTitleById(@NonNull Context context, int id){
        return context.getResources().getStringArray(R.array.game_gift_name_list)[id];
    }


    public static int getGifByGiftId(int id){
        switch (id){
            case 0: return R.drawable.game_gift_anim_bell;
            case 1: return R.drawable.game_gift_anim_icecream;
            case 2: return R.drawable.game_gift_anim_wine;
            case 3: return R.drawable.game_gift_anim_cake;
            case 4: return R.drawable.game_gift_anim_ring;
            case 5: return R.drawable.game_gift_anim_watch;
            case 6: return R.drawable.game_gift_anim_diamond;
            default: return R.drawable.game_gift_anim_rocket;
        }
    }

    public static int getIconByGiftId(int id){
        switch (id){
            case 0: return R.drawable.game_gift_01_bell;
            case 1: return R.drawable.game_gift_02_icecream;
            case 2: return R.drawable.game_gift_03_wine;
            case 3: return R.drawable.game_gift_04_cake;
            case 4: return R.drawable.game_gift_05_ring;
            case 5: return R.drawable.game_gift_06_watch;
            case 6: return R.drawable.game_gift_07_diamond;
            default: return R.drawable.game_gift_08_rocket;
        }
    }

    public static int getGiftIdFromGiftInfo(@NonNull Context context, @NonNull GiftInfo giftInfo){
        String[] nameEngList = context.getResources().getStringArray(R.array.game_gift_name_list_eng);
        int index = -1;
        for (int i = 0; i < nameEngList.length; i++) {
            if (nameEngList[i].equals(giftInfo.getGifName())){
                index = i;
                break;
            }
        }
        return index;
    }

    @Nullable
    public static String getGiftDesc(@NonNull Context context, @NonNull GiftInfo giftInfo){
        int index = getGiftIdFromGiftInfo(context, giftInfo);
        if (index != -1){
            String[] nameList = context.getResources().getStringArray(R.array.game_gift_name_list);
            String[] emojiList = context.getResources().getStringArray(R.array.game_gift_name_list_emoji);
            return context.getString(R.string.game_gift_received, "User-"+ giftInfo.getUserId(), nameList[index], emojiList[index]);
        }
        return null;
    }
}
