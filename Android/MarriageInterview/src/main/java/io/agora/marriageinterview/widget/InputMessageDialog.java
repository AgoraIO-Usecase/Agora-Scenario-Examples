package io.agora.marriageinterview.widget;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.agora.data.BaseError;
import com.agora.data.manager.RTMManager;
import com.agora.data.observer.DataCompletableObserver;

import io.agora.baselibrary.base.DataBindBottomSheetDialogFragment;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.databinding.MerryDialogInputMessageBinding;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 消息输入界面
 *
 * @author chenhengfei@agora.io
 */
public class InputMessageDialog extends DataBindBottomSheetDialogFragment<MerryDialogInputMessageBinding> implements View.OnClickListener {
    private static final String TAG = InputMessageDialog.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Dialog_Input);
    }

    @Override
    public void iniBundle(@NonNull Bundle bundle) {
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_dialog_input_message;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {
        mDataBinding.btSend.setOnClickListener(this);
    }

    @Override
    public void iniData() {

    }

    public void show(@NonNull FragmentManager manager, ISendMessageCallback mISendMessageCallback) {
        this.mISendMessageCallback = mISendMessageCallback;

        Bundle intent = new Bundle();
        setArguments(intent);
        super.show(manager, TAG);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btSend) {
            sendMessage();
        }
    }

    private void sendMessage() {
        String message = mDataBinding.etInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }

        RTMManager.Instance(requireContext())
                .sendChannelMessage(message)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(requireContext()) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        dismiss();
                    }

                    @Override
                    public void handleSuccess() {
                        mISendMessageCallback.onSendMessage(message);
                        dismiss();
                    }
                });
    }

    private ISendMessageCallback mISendMessageCallback;

    public interface ISendMessageCallback {
        void onSendMessage(String message);
    }
}
