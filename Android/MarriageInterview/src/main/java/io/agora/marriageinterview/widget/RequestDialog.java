package io.agora.marriageinterview.widget;

import android.os.Bundle;
import android.view.Gravity;
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
import io.agora.marriageinterview.databinding.MerryDialogRequestBinding;

/**
 * 申请连麦
 *
 * @author chenhengfei@agora.io
 */
public class RequestDialog extends DataBindBaseDialog<MerryDialogRequestBinding> implements View.OnClickListener {
    private static final String TAG = RequestDialog.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Window win = getDialog().getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        win.setAttributes(params);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Dialog_Bottom);
    }

    @Override
    public void iniBundle(@NonNull Bundle bundle) {
    }

    @Override
    public int getLayoutId() {
        return R.layout.merry_dialog_request;
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

    }

    public void show(@NonNull FragmentManager manager) {
        Bundle intent = new Bundle();
        setArguments(intent);
        super.show(manager, TAG);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btCancel) {
            dismiss();
        } else if (v.getId() == R.id.btConfirm) {
            doRequest();
        }
    }

    private void doRequest() {
        mDataBinding.btConfirm.setEnabled(false);
//        DataRepositroy.Instance(requireContext())
//                .inviteSeat(mMember)
//                .observeOn(AndroidSchedulers.mainThread())
//                .compose(mLifecycleProvider.bindToLifecycle())
//                .subscribe(new DataCompletableObserver(requireContext()) {
//                    @Override
//                    public void handleError(@NonNull BaseError e) {
//                        mDataBinding.btConfirm.setEnabled(true);
//                        ToastUtile.toastShort(requireContext(), e.getMessage());
//                    }
//
//                    @Override
//                    public void handleSuccess() {
//                        mDataBinding.btConfirm.setEnabled(true);
//                        dismiss();
//                    }
//                });
    }
}
