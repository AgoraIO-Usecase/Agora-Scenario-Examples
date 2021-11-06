package io.agora.livepk.util;

import android.content.Context;

import io.agora.livepk.R;

public class RandomUtil {
    private static int sLastIndex;

    public static String randomLiveRoomName(Context context) {
        String[] ROOM_NAMES = context.getResources().getStringArray(R.array.random_channel_names);

        int length = ROOM_NAMES.length;
        int thisIndex = sLastIndex;
        while (thisIndex == sLastIndex) {
            thisIndex = (int) (Math.random() * length);
        }

        sLastIndex = thisIndex;
        return ROOM_NAMES[sLastIndex];
    }

}
