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
import com.agora.data.model.Member;
import com.agora.data.observer.DataCompletableObserver;

import java.util.List;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.marriageinterview.R;
import io.agora.marriageinterview.adapter.RoomMemberListAdapter;
import io.agora.marriageinterview.data.DataRepositroy;
import io.agora.marriageinterview.databinding.MerryDialogMemberListBinding;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 房间成员列表
 *
 * @author chenhengfei@agora.io
 */
public class MemberListDialog extends DataBindBaseDialog<MerryDialogMemberListBinding> implements OnItemClickListener<Member> {
    private static final String TAG = MemberListDialog.class.getSimpleName();

    private RoomMemberListAdapter mAdapter;

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
        return R.layout.merry_dialog_member_list;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {

    }

    @Override
    public void iniData() {
        boolean showInvite = RoomManager.Instance(requireContext()).isOwner();
        Member member = RoomManager.Instance(requireContext()).getMine();
        mAdapter = new RoomMemberListAdapter(null, this, showInvite, member, mIConnectStatusProvider);
        mDataBinding.rvList.setAdapter(mAdapter);

        loadData();
    }

    public void loadData() {
        List<Member> members = RoomManager.Instance(requireContext()).getMembers();
        mAdapter.setDatas(members);
    }

    public void show(@NonNull FragmentManager manager, IConnectStatusProvider mIConnectStatusProvider) {
        this.mIConnectStatusProvider = mIConnectStatusProvider;
        Bundle intent = new Bundle();
        setArguments(intent);
        super.show(manager, TAG);
    }

    @Override
    public void onItemClick(@NonNull Member data, View view, int position, long id) {
        if (view.getId() == R.id.tvInvite) {
            doInvite(view, position, data);
        }
    }

    private void doInvite(View view, int index, Member data) {
        if (mIConnectStatusProvider.hasLeftMember() && mIConnectStatusProvider.hasRightMember()) {
            return;
        }

        view.setEnabled(false);
        DataRepositroy.Instance(requireContext())
                .inviteConnect(data, Action.ACTION.Invite)
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
                        view.setEnabled(true);
                    }
                });
    }

    private IConnectStatusProvider mIConnectStatusProvider;

    public interface IConnectStatusProvider {
        boolean isMemberConnected(Member member);

        boolean hasLeftMember();

        boolean hasRightMember();
    }
}
