package io.agora.livepk.widget;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import java.util.Random;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.livepk.R;
import io.agora.livepk.databinding.DialogCreateRoomBinding;

/**
 * 创建房间
 *
 * @author chenhengfei@agora.io
 */
public class CreateRoomDialog extends DataBindBaseDialog<DialogCreateRoomBinding> implements View.OnClickListener {
    private static final String TAG = CreateRoomDialog.class.getSimpleName();

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

    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_create_room;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {
        mDataBinding.ivRefresh.setOnClickListener(this);
        mDataBinding.btCreate.setOnClickListener(this);
        mDataBinding.ivClose.setOnClickListener(this);
    }

    @Override
    public void iniData() {
        refreshName();
    }

    public void show(@NonNull FragmentManager manager, ICreateCallback mICreateCallback) {
        this.mICreateCallback = mICreateCallback;
        super.show(manager, TAG);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivClose) {
            dismiss();
        } else if (id == R.id.ivRefresh) {
            refreshName();
        } else if (id == R.id.btCreate) {
            create();
        }
    }

    public static String radomName() {
        return "Room " + String.valueOf(new Random().nextInt(999999));
    }

    private void refreshName() {
        mDataBinding.etInput.setText(radomName());
    }

    private void create() {
        String roomName = mDataBinding.etInput.getText().toString();
        if (TextUtils.isEmpty(roomName)) {
            return;
        }
        if(mICreateCallback != null){
            mICreateCallback.onRoomCreate(roomName);
        }
        dismiss();
    }

    private ICreateCallback mICreateCallback;

    public interface ICreateCallback {
        void onRoomCreate(@NonNull String roomName);
    }
}
