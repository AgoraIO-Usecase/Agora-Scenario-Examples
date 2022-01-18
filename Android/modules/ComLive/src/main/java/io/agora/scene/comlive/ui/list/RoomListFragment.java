package io.agora.scene.comlive.ui.list;

import static java.lang.Boolean.TRUE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.DividerDecoration;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.comlive.GlobalViewModel;
import io.agora.scene.comlive.R;
import io.agora.scene.comlive.base.BaseNavFragment;
import io.agora.scene.comlive.bean.RoomInfo;
import io.agora.scene.comlive.databinding.ComLiveFragmentRoomListBinding;
import io.agora.scene.comlive.databinding.ComLiveItemRoomListBinding;
import io.agora.scene.comlive.util.Event;
import io.agora.scene.comlive.util.ComLiveUtil;
import io.agora.scene.comlive.util.ViewStatus;

public class RoomListFragment extends BaseNavFragment<ComLiveFragmentRoomListBinding> implements OnItemClickListener<RoomInfo> {

    ////////////////////////////////////// -- PERMISSION --//////////////////////////////////////////////////////////////
    public static final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> {
        List<String> permissionsRefused = new ArrayList<>();
        for (String s : res.keySet()) {
            if (TRUE != res.get(s))
                permissionsRefused.add(s);
        }
        if (!permissionsRefused.isEmpty()) {
            showPermissionAlertDialog();
        } else {
            toNextPage();
        }
    });
    ////////////////////////////////////// -- VIEW MODEL --//////////////////////////////////////////////////////////////
    private GlobalViewModel mGlobalModel;
    private RoomListViewModel mViewModel;

    ////////////////////////////////////// -- DATA --//////////////////////////////////////////////////////////////
    private BaseRecyclerViewAdapter<ComLiveItemRoomListBinding, RoomInfo, RoomListHolder> mAdapter;
    private RoomInfo tempRoom;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = ComLiveUtil.getViewModel(this, RoomListViewModel.class);

        mGlobalModel = ComLiveUtil.getAndroidViewModel(this, GlobalViewModel.class);
        mGlobalModel.clearRoomInfo();
        initView();
        initListener();
    }

    private void initView() {
        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4;
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, RoomListHolder.class);
        mBinding.recyclerViewFgList.setAdapter(mAdapter);
        mBinding.recyclerViewFgList.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        mBinding.recyclerViewFgList.addItemDecoration(new DividerDecoration(spanCount));
        mBinding.swipeFgList.setProgressViewOffset(true, 0, mBinding.swipeFgList.getProgressViewEndOffset());
        mBinding.swipeFgList.setColorSchemeResources(R.color.com_live_btn_gradient_start_color, R.color.com_live_btn_gradient_end_color);
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

            mBinding.recyclerViewFgList.setPaddingRelative(0,0,0,inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // "创建房间"按钮
        mBinding.btnCreateFgList.setOnClickListener((v) -> checkPermissionBeforeToNextPage(null));
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
        checkPermissionBeforeToNextPage(data);
    }

    public void onListStatus(boolean empty) {
        mBinding.emptyViewFgList.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /**
     * 摄像头、录音权限检查
     */
    private void checkPermissionBeforeToNextPage(@Nullable RoomInfo data) {
        tempRoom = data;

        // 小于 M 无需控制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toNextPage();
            return;
        }

        // 检查权限是否通过
        boolean needRequest = false;

        for (String permission : permissions) {
            if (requireContext().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (!needRequest) {
            toNextPage();
            return;
        }

        boolean requestDirectly = true;
        for (String requiredPermission : permissions)
            if (shouldShowRequestPermissionRationale(requiredPermission)) {
                requestDirectly = false;
                break;
            }
        // 直接申请
        if (requestDirectly) requestPermissionLauncher.launch(permissions);
            // 显示申请理由
        else showPermissionRequestDialog();
    }

    private void toNextPage() {
        if (tempRoom != null)
            mGlobalModel.roomInfo.setValue(new Event<>(tempRoom));

        findNavController().navigate(R.id.action_roomListFragment_to_roomFragment);
    }

    private void showPermissionAlertDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.com_live_permission_refused)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void showPermissionRequestDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.com_live_permission_alert).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> requestPermissionLauncher.launch(permissions))).show();
    }
}
