package io.agora.scene.breakoutroom;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;

import com.google.android.material.circularreveal.cardview.CircularRevealCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

import io.agora.example.base.BaseUtil;

public class RoomUtil {

    public static void configInputDialog(@NonNull CircularRevealCardView v) {
        v.setRadius(BaseUtil.dp2px(12));
        v.setClipToPadding(false);
        v.setElevation(BaseUtil.dp2px(6));
    }

    public static void configTextInputLayout(@NonNull TextInputLayout layout) {
        EditText editText = layout.getEditText();
        if (editText != null) {
            // clear error
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (layout.isErrorEnabled())
                        layout.setErrorEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }
    }

    public static void showNameIllegalError(@NonNull TextInputLayout inputLayout, @StringRes int stringRes){
        if (inputLayout.isErrorEnabled())
            BaseUtil.shakeViewAndVibrateToAlert(inputLayout);
        else
            inputLayout.setError(inputLayout.getContext().getString(stringRes));
    }

    @NonNull
    public static <T,K,V> HashMap<K,V> convertObjToHashMap(T obj, @NonNull Gson gson){
        String jsonString = gson.toJson(obj);
        return gson.fromJson(jsonString, new TypeToken<HashMap<K,V>>(){}.getType());
    }


    @DrawableRes
    public static int getDrawableByName(@Nullable String name){
        int i = 1;
        try {
            if (name != null)
                i = Integer.parseInt(name.toLowerCase().substring(8,10));
        } catch (Exception ignored) { }
        switch (i){
            case 1: return R.drawable.room_portrait01;
            case 2: return R.drawable.room_portrait02;
            case 3: return R.drawable.room_portrait03;
            case 4: return R.drawable.room_portrait04;
            case 5: return R.drawable.room_portrait05;
            case 6: return R.drawable.room_portrait06;
            case 7: return R.drawable.room_portrait07;
            case 8: return R.drawable.room_portrait08;
            case 9: return R.drawable.room_portrait09;
            case 10: return R.drawable.room_portrait10;
            case 11: return R.drawable.room_portrait11;
            case 12: return R.drawable.room_portrait12;
            case 13: return R.drawable.room_portrait13;
            default: return R.drawable.room_portrait14;
        }
    }

    @NonNull
    public static CardView getChildVideoCardView(@NonNull Context context,boolean isLocal, int uid) {
        CardView cardView = new CardView(context);

        // title
        TextView titleText = new TextView(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = BOTTOM;
        lp.bottomMargin = (int) BaseUtil.dp2px(12);
        titleText.setLayoutParams(lp);
        titleText.setGravity(CENTER | BOTTOM);
        titleText.setText(context.getString(R.string.room_user_name_format, uid));
        titleText.setTextColor(uid == Integer.parseInt(RoomConstant.userId) ? Color.RED : Color.WHITE);


        // container
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextureView textureView = new TextureView(context);

        frameLayout.addView(textureView);
        frameLayout.addView(titleText);


        cardView.setRadius(BaseUtil.dp2px(16));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setTag(isLocal ? -uid : uid);

        cardView.addView(frameLayout);
        return cardView;
    }

}
