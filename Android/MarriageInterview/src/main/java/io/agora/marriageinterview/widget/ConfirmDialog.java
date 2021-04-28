package io.agora.marriageinterview.widget;

import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryDialogConfirmBinding;

/**
 * 确认窗口
 *
 * @author chenhengfei@agora.io
 */
public class ConfirmDialog extends DataBindBaseDialog<MerryDialogConfirmBinding> implements View.OnClickListener {
    private static final String TAG = ConfirmDialog.class.getSimpleName();

    private OnConfirmCallback mOnConfirmCallback;

    private static final String TAG_TITLE = "title";
    private static final String TAG_MESSAGE = "message";

    private String title;
    private String message;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Window win = getDialog().getWindow();
        WindowManager windowManager = win.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams params = win.getAttributes();
        params.width = display.getWidth() * 4 / 5;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        win.setAttributes(params);
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Dialog_Nomal);
    }

    @Override
    public void iniBundle(@NonNull Bundle bundle) {
        title = bundle.getString(TAG_TITLE);
        message = bundle.getString(TAG_MESSAGE);
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_dialog_confirm;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {
        mDataBinding.btCancel.setOnClickListener(this);
        mDataBinding.btConfirm.setOnClickListener(this);
    }

    @Override
    public void iniData() {
        mDataBinding.tvTitle.setText(title);
        mDataBinding.tvMessage.setText(message);
    }

    public void show(@NonNull FragmentManager manager, String title, String message, OnConfirmCallback mOnConfirmCallback) {
        this.mOnConfirmCallback = mOnConfirmCallback;

        Bundle intent = new Bundle();
        intent.putString(TAG_TITLE, title);
        intent.putString(TAG_MESSAGE, message);
        setArguments(intent);
        super.show(manager, TAG);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btCancel) {
            mDataBinding.btCancel.setEnabled(false);
            mOnConfirmCallback.onClickCancel();
            mDataBinding.btCancel.setEnabled(true);
            dismiss();
        } else if (v.getId() == R.id.btConfirm) {
            mDataBinding.btConfirm.setEnabled(false);
            mOnConfirmCallback.onClickConfirm();
            mDataBinding.btConfirm.setEnabled(true);
            dismiss();
        }
    }

    public interface OnConfirmCallback {
        void onClickCancel();

        void onClickConfirm();
    }
}
