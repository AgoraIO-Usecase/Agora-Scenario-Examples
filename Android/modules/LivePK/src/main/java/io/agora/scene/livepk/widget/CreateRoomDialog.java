package io.agora.scene.livepk.widget;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import java.util.Random;

import io.agora.example.base.BaseDialogFragment;
import io.agora.scene.livepk.R;
import io.agora.scene.livepk.databinding.PkDialogCreateRoomBinding;

/**
 * 创建房间
 *
 * @author chenhengfei@agora.io
 */
public class CreateRoomDialog extends BaseDialogFragment<PkDialogCreateRoomBinding> implements View.OnClickListener {
    private static final String TAG = CreateRoomDialog.class.getSimpleName();

    @Override
    public void onBeforeCreateView() {
        Window win = getDialog().getWindow();
        WindowManager windowManager = win.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams params = win.getAttributes();
        params.width = display.getWidth() * 4 / 5;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        win.setAttributes(params);
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.pk_Dialog_Nomal);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.ivRefresh.setOnClickListener(this);
        mBinding.btCreate.setOnClickListener(this);
        mBinding.ivClose.setOnClickListener(this);
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
        mBinding.etInput.setText(radomName());
    }

    private void create() {
        String roomName = mBinding.etInput.getText().toString();
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
