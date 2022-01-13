package io.agora.scene.onelive.ui.list;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.DividerDecoration;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.onelive.GlobalViewModel;
import io.agora.scene.onelive.R;
import io.agora.scene.onelive.base.BaseNavFragment;
import io.agora.scene.onelive.bean.RoomInfo;
import io.agora.scene.onelive.databinding.OneFragmentRoomListBinding;
import io.agora.scene.onelive.databinding.OneItemRoomListBinding;
import io.agora.scene.onelive.util.Event;
import io.agora.scene.onelive.util.EventObserver;
import io.agora.scene.onelive.util.OneUtil;
import io.agora.scene.onelive.util.ViewStatus;

public class RoomListFragment extends BaseNavFragment<OneFragmentRoomListBinding> implements OnItemClickListener<RoomInfo> {

    ////////////////////////////////////// -- PERMISSION --//////////////////////////////////////////////////////////////
    public static final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    private final BaseUtil.PermissionResultCallback<String[]> callback = new BaseUtil.PermissionResultCallback<String[]>() {
        @Override
        public void onAllPermissionGranted() {
            toNextPage();
        }

        @Override
        public void onPermissionRefused(String[] refusedPermissions) {
            showPermissionAlertDialog();
        }

        @Override
        public void showReasonDialog(String[] refusedPermissions) {
            showPermissionRequestDialog();
        }
    };
    // Must have
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = BaseUtil.registerForActivityResult(RoomListFragment.this, callback);

    ////////////////////////////////////// -- VIEW MODEL --//////////////////////////////////////////////////////////////
    private GlobalViewModel mGlobalModel;
    private RoomListViewModel mViewModel;

    ////////////////////////////////////// -- DATA --//////////////////////////////////////////////////////////////
    private BaseRecyclerViewAdapter<OneItemRoomListBinding, RoomInfo, RoomListHolder> mAdapter;
    private RoomInfo tempRoom;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlobalModel = OneUtil.getAndroidViewModel(this, GlobalViewModel.class);
        mGlobalModel.clearRoomInfo();
        mViewModel = OneUtil.getViewModel(this, RoomListViewModel.class);
        initView();
        initListener();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, RoomListHolder.class);
        mBinding.recyclerViewFgList.setAdapter(mAdapter);
        mBinding.recyclerViewFgList.addItemDecoration(new DividerDecoration(1));
        mBinding.swipeFgList.setProgressViewOffset(true, 0, mBinding.swipeFgList.getProgressViewEndOffset());
        mBinding.swipeFgList.setColorSchemeResources(R.color.one_btn_gradient_start_color, R.color.one_btn_gradient_end_color);
        int backgroundColor = OneUtil.getMaterialBackgroundColor(BaseUtil.getColorInt(requireContext(), R.attr.colorSurface));
        mBinding.swipeFgList.setProgressBackgroundColorSchemeColor(backgroundColor);
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 顶部
            mBinding.appBarFgList.setPadding(0, inset.top, 0, 0);
            // 底部
            CoordinatorLayout.LayoutParams lpBtn = (CoordinatorLayout.LayoutParams) mBinding.btnCreateFgList.getLayoutParams();
            lpBtn.bottomMargin = inset.bottom + ((int) BaseUtil.dp2px(24));
            mBinding.btnCreateFgList.setLayoutParams(lpBtn);
            mBinding.recyclerViewFgList.setPaddingRelative(0, 0, 0, inset.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        // "创建房间"按钮
        mBinding.btnCreateFgList.setOnClickListener((v) -> {
            tempRoom = null;
            BaseUtil.checkPermissionBeforeNextOP(this, requestPermissionLauncher, permissions, callback);
        });
        // 下拉刷新监听
        mBinding.swipeFgList.setOnRefreshListener(() -> mViewModel.fetchRoomList());
        // 状态监听
        mViewModel.viewStatus().observe(getViewLifecycleOwner(), this::onViewStatusChanged);
        // 房间列表数据监听
        mViewModel.roomList().observe(getViewLifecycleOwner(), resList -> {
            onListStatus(resList.isEmpty());
            mAdapter.submitListAndPurge(resList);
        });

    }

    private void onViewStatusChanged(ViewStatus viewStatus) {
        if (viewStatus instanceof ViewStatus.Loading) {
            mBinding.swipeFgList.setRefreshing(true);
        } else if (viewStatus instanceof ViewStatus.Done)
            mBinding.swipeFgList.setRefreshing(false);
        else if (viewStatus instanceof ViewStatus.Error) {
            mBinding.swipeFgList.setRefreshing(false);
            BaseUtil.toast(requireContext(), ((ViewStatus.Error) viewStatus).msg);
        }
    }

    @Override
    public void onItemClick(@NonNull RoomInfo data, @NonNull View view, int position, long viewType) {
        tempRoom = data;
        BaseUtil.checkPermissionBeforeNextOP(this, requestPermissionLauncher, permissions, callback);
    }

    public void onListStatus(boolean empty) {
        mBinding.emptyViewFgList.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void toNextPage() {
        if (tempRoom != null)
            mGlobalModel.roomInfo.setValue(new Event<>(tempRoom));

        findNavController().navigate(R.id.action_roomListFragment_to_roomFragment);
    }

    private void showPermissionAlertDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.one_permission_refused)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void showPermissionRequestDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.one_permission_alert).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> requestPermissionLauncher.launch(permissions))).show();
    }
}
