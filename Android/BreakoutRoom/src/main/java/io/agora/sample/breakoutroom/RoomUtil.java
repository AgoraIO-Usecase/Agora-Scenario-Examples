package io.agora.sample.breakoutroom;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.google.android.material.circularreveal.cardview.CircularRevealCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

import io.agora.example.base.BaseUtil;

public class RoomUtil {

    public static void configInputDialog(CircularRevealCardView v) {
        v.setRadius(BaseUtil.dp2px(12));
        v.setClipToPadding(false);
        v.setElevation(BaseUtil.dp2px(6));
    }

    public static void configTextInputLayout(TextInputLayout layout) {
        EditText editText = layout.getEditText();
        if (editText != null)
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

    public static <T,K,V> HashMap<K,V> convertObjToHashMap(T obj, Gson gson){
        String jsonString = gson.toJson(obj);
        return gson.fromJson(jsonString, new TypeToken<HashMap<K,V>>(){}.getType());
    }


    @DrawableRes
    public static int getDrawableByName(@Nullable String name){
        int i = 1;
        try {
            if (name != null)
                i = Integer.parseInt(name.toLowerCase().substring(8,10));
        } catch (NumberFormatException ignored) { }
        switch (i){
            case 1: return R.drawable.portrait01;
            case 2: return R.drawable.portrait02;
            case 3: return R.drawable.portrait03;
            case 4: return R.drawable.portrait04;
            case 5: return R.drawable.portrait05;
            case 6: return R.drawable.portrait06;
            case 7: return R.drawable.portrait07;
            case 8: return R.drawable.portrait08;
            case 9: return R.drawable.portrait09;
            case 10: return R.drawable.portrait10;
            case 11: return R.drawable.portrait11;
            case 12: return R.drawable.portrait12;
            case 13: return R.drawable.portrait13;
            default: return R.drawable.portrait14;
        }
    }
}
