package io.agora.livepk.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import io.agora.livepk.R;

public class UserUtil {

    private static final String SP_KEY_USER_NAME = "AppUserName";
    private static final String SP_KEY_USER_ID = "AppUserId";

    private static final int[] PROFILE_BG_RES = {
            R.drawable.profile_image_1,
            R.drawable.profile_image_2,
            R.drawable.profile_image_3,
            R.drawable.profile_image_4,
            R.drawable.profile_image_5,
            R.drawable.profile_image_6,
            R.drawable.profile_image_7,
            R.drawable.profile_image_8,
            R.drawable.profile_image_9,
            R.drawable.profile_image_10,
            R.drawable.profile_image_11,
            R.drawable.profile_image_12
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

    @NonNull
    public static String getLocalUserId() {
        String uuid = PreferenceUtil.get(SP_KEY_USER_ID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            PreferenceUtil.put(SP_KEY_USER_ID, uuid);
        }
        return uuid;
    }

    public static String getLocalUserName(Context context){
        String userName = PreferenceUtil.get(SP_KEY_USER_NAME, "");
        if (TextUtils.isEmpty(userName)) {
            userName = randomUserName(context);
            PreferenceUtil.put(SP_KEY_USER_NAME, userName);
        }
        return userName;
    }

    public static void setLocalUserName(String userName){
        PreferenceUtil.put(SP_KEY_USER_NAME, userName);
    }

    public static String randomUserName(Context context) {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale.getLanguage().equals("en")) {
            return String.format(defaultLocale, "%s %s",
                    getRandomName(context), getRandomSurname(context));
        }
        return String.format(defaultLocale, "%s%s",
                getRandomSurname(context), getRandomName(context));
    }

    private static String getRandomSurname(Context context) {
        Random random = new Random(System.currentTimeMillis());
        String[] surnames = context.getResources().getStringArray(R.array.random_surnames);
        return surnames[random.nextInt(surnames.length - 1)];
    }

    private static String getRandomName(Context context) {
        Random random = new Random(System.currentTimeMillis());
        String[] names = context.getResources().getStringArray(R.array.random_names);
        return names[random.nextInt(names.length - 1)];
    }
}
