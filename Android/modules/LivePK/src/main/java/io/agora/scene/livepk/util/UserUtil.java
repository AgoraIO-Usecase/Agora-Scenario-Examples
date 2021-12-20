package io.agora.scene.livepk.util;

import io.agora.scene.livepk.R;

public class UserUtil {

    private static final int[] PROFILE_BG_RES = {
            R.drawable.pk_profile_image_1,
            R.drawable.pk_profile_image_2,
            R.drawable.pk_profile_image_3,
            R.drawable.pk_profile_image_4,
            R.drawable.pk_profile_image_5,
            R.drawable.pk_profile_image_6,
            R.drawable.pk_profile_image_7,
            R.drawable.pk_profile_image_8,
            R.drawable.pk_profile_image_9,
            R.drawable.pk_profile_image_10,
            R.drawable.pk_profile_image_11,
            R.drawable.pk_profile_image_12
    };

    public static int getUserProfileIcon(String userId) {
        try {
            long intUserId = Long.valueOf(userId);
            int size = PROFILE_BG_RES.length;
            int index = (int) (intUserId % size);
            return PROFILE_BG_RES[index];
        } catch (NumberFormatException e) {
            return PROFILE_BG_RES[0];
        }
    }

}
