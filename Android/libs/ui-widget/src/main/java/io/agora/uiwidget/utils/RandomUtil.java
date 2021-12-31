package io.agora.uiwidget.utils;

import android.content.Context;

import androidx.annotation.DrawableRes;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import io.agora.uiwidget.R;


public class RandomUtil {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private static final int sGeneratedIdRandomStart = randomId(1, 10000);
    private static int sLastIndex;

    private static final int[] ICONS = new int[]{
            R.drawable.random_icon_1,
            R.drawable.random_icon_2,
            R.drawable.random_icon_3,
            R.drawable.random_icon_4,
            R.drawable.random_icon_5,
            R.drawable.random_icon_6,
            R.drawable.random_icon_7,
            R.drawable.random_icon_8,
            R.drawable.random_icon_9,
            R.drawable.random_icon_10,
            R.drawable.random_icon_11,
            R.drawable.random_icon_12
    };

    public static @DrawableRes int randomLiveRoomIcon() {
        int length = ICONS.length;
        int thisIndex = sLastIndex;
        while (thisIndex == sLastIndex) {
            thisIndex = (int) (Math.random() * length);
        }

        sLastIndex = thisIndex;
        return ICONS[sLastIndex];
    }

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

    public static int randomId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return sGeneratedIdRandomStart + result;
            }
        }
    }

    public static int randomId(int start, int end){
        return new Random().nextInt(end - start) + start + 1;
    }
}
