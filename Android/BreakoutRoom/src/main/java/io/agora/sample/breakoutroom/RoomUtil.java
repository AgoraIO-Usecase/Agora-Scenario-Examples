package io.agora.sample.breakoutroom;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.circularreveal.cardview.CircularRevealCardView;
import com.google.android.material.textfield.TextInputLayout;

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

    public static boolean isInputValid(TextInputLayout layout){
        EditText editText = layout.getEditText();
        if (editText != null){
            Editable text = editText.getText();

            return text != null && !text.toString().trim().isEmpty();
        }
        return false;
    }
}
