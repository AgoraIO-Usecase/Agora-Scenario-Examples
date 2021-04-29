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

import com.agora.data.BaseError;
import com.agora.data.manager.RoomManager;
import com.agora.data.model.Action;
import com.agora.data.model.RequestMember;
import com.agora.data.observer.DataCompletableObserver;
import com.agora.data.observer.DataObserver;

import java.util.List;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.adapter.RequestConnectListAdapter;
import io.agora.marriageinterview.data.DataRepositroy;
import io.agora.marriageinterview.databinding.MerryDialogRequestConnectListBinding;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 申请连接列表
 *
 * @author chenhengfei@agora.io
 */
public class RequestConnectListDialog extends DataBindBaseDialog<MerryDialogRequestConnectListBinding> implements OnItemClickListener<RequestMember> {
    private static final String TAG = RequestConnectListDialog.class.getSimpleName();

    private RequestConnectListAdapter mAdapter;

    private IConnectStatusProvider mIConnectStatusProvider;

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
        return R.layout.merry_dialog_request_connect_list;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {

    }

    @Override
    public void iniData() {
        mAdapter = new RequestConnectListAdapter(null, this);
        mDataBinding.rvList.setAdapter(mAdapter);

        loadData();
    }

    public void loadData() {
        DataRepositroy.Instance(requireContext())
                .getRequestList()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataObserver<List<RequestMember>>(requireContext()) {

                    @Override
                    public void handleError(@NonNull BaseError e) {

                    }

                    @Override
                    public void handleSuccess(@NonNull List<RequestMember> members) {
                        mAdapter.setDatas(members);
                    }
                });
    }

    public void show(@NonNull FragmentManager manager, IConnectStatusProvider mIConnectStatusProvider) {
        this.mIConnectStatusProvider = mIConnectStatusProvider;
        Bundle intent = new Bundle();
        setArguments(intent);
        super.show(manager, TAG);
    }

    @Override
    public void onItemClick(@NonNull RequestMember data, View view, int position, long id) {
        if (view.getId() == R.id.btRefuse) {
            clickRefuse(view, position, data);
        } else if (view.getId() == R.id.btAgree) {
            clickAgree(view, position, data);
        }
    }

    private void clickRefuse(View view, int index, RequestMember data) {
        view.setEnabled(false);
        RoomManager.Instance(requireContext())
                .refuseRequest(data.getMember(), data.getAction())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(requireContext()) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        ToastUtile.toastShort(requireContext(), e.getMessage());
                        view.setEnabled(true);
                    }

                    @Override
                    public void handleSuccess() {
                        mAdapter.deleteItem(index);
                        view.setEnabled(true);
                    }
                });
    }

    private void clickAgree(View view, int index, RequestMember data) {
        Action.ACTION action = data.getAction();
        if (action == Action.ACTION.RequestLeft) {
            if (mIConnectStatusProvider.hasLeftMember()) {
                return;
            }
        } else if (action == Action.ACTION.RequestRight) {
            if (mIConnectStatusProvider.hasRightMember()) {
                return;
            }
        }

        view.setEnabled(false);
        RoomManager.Instance(requireContext())
                .agreeRequest(data.getMember(), data.getAction())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(requireContext()) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        ToastUtile.toastShort(requireContext(), e.getMessage());
                        view.setEnabled(true);
                    }

                    @Override
                    public void handleSuccess() {
                        mAdapter.deleteItem(index);
                        view.setEnabled(true);
                    }
                });
    }

    public interface IConnectStatusProvider {
        boolean hasLeftMember();

        boolean hasRightMember();
    }
}
