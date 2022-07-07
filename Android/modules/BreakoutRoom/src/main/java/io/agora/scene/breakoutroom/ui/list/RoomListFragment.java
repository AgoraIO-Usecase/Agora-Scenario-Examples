package io.agora.scene.breakoutroom.ui.list;

import android.Manifest;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;
import java.util.regex.Pattern;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.DividerDecoration;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomConstant;
import io.agora.scene.breakoutroom.RoomUtil;
import io.agora.scene.breakoutroom.ViewStatus;
import io.agora.scene.breakoutroom.base.BaseNavFragment;
import io.agora.scene.breakoutroom.bean.RoomInfo;
import io.agora.scene.breakoutroom.databinding.RoomFragmentRoomListBinding;
import io.agora.scene.breakoutroom.databinding.RoomItemRoomListBinding;
import io.agora.scene.breakoutroom.ui.room.RoomFragment;

public class RoomListFragment extends BaseNavFragment<RoomFragmentRoomListBinding> implements OnItemClickListener<RoomInfo> {

    public static final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    private RoomListViewModel mViewModel;

    private BaseRecyclerViewAdapter<RoomItemRoomListBinding, RoomInfo, ListViewHolder> listAdapter;

    private RoomInfo tempRoom;

    private final BaseUtil.PermissionResultCallback<String[]> permissionResultCallback = new BaseUtil.PermissionResultCallback<String[]>() {
        @Override
        public void onAllPermissionGranted() {
            goToRoomPage();
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

    private final ActivityResultLauncher<String[]> launcher = BaseUtil.registerForActivityResult(this, permissionResultCallback);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(RoomListViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom) + (int) BaseUtil.dp2px(12);

            mBinding.appBarFgRoomList.setPadding(0, inset.top, 0, 0);

            CoordinatorLayout.LayoutParams lpFab = (CoordinatorLayout.LayoutParams) mBinding.fabFgList.getLayoutParams();
            lpFab.bottomMargin = desiredBottom;
            mBinding.fabFgList.setLayoutParams(lpFab);

            return WindowInsetsCompat.CONSUMED;
        });

        initView();
        initListener();
    }

    private void initView() {
        // config Dialog
        RoomUtil.configInputDialog(mBinding.viewInputFgList.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgList.inputLayoutViewInput);
        mBinding.viewInputFgList.titleViewInput.setText(R.string.room_fab_add_room);

        // config RecyclerView
        listAdapter = new BaseRecyclerViewAdapter<>(null, this, ListViewHolder.class);
        mBinding.recyclerViewFgList.setAdapter(listAdapter);
        mBinding.recyclerViewFgList.addItemDecoration(new DividerDecoration(2));
        mBinding.swipeFgList.setProgressViewOffset(true, 0, mBinding.swipeFgList.getProgressViewEndOffset());
    }

    @Override
    public void onItemClick(@NonNull RoomInfo data, @NonNull View view, int position, long viewType) {
        this.tempRoom = listAdapter.getItemData(position);
        BaseUtil.checkPermissionBeforeNextOP(this, launcher, permissions, permissionResultCallback);
    }

    private void initListener() {
        // Inner Dialog stuff
        mBinding.viewInputFgList.getRoot().setOnClickListener(this::clearFocus);
        mBinding.viewInputFgList.btnConfirmViewLayout.setOnClickListener(this::createRoom);

        // Show Dialog stuff
        mBinding.fabFgList.setOnClickListener(v -> mBinding.fabFgList.setExpanded(true));
        mBinding.scrimFgList.setOnClickListener(v -> mBinding.fabFgList.setExpanded(false));

        // Empty View stuff
        mBinding.btnRefreshFgList.setOnClickListener((v) -> mViewModel.fetchRoomList());
        // swipe
        mBinding.swipeFgList.setOnRefreshListener(() -> mViewModel.fetchRoomList());


        mViewModel.viewStatus().observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Loading) {
                if (((ViewStatus.Loading) viewStatus).showLoading)
                    showLoading();
                else
                    onLoadingStatus(true);
            } else if (viewStatus instanceof ViewStatus.Done) {
                mBinding.fabFgList.setExpanded(false);
                dismissLoading();
            }else if (viewStatus instanceof ViewStatus.Error) {
                dismissLoading();
                BaseUtil.toast(requireContext(), ((ViewStatus.Error) viewStatus).msg);
            }
        });

        // 房间列表数据监听
        mViewModel.roomList().observe(getViewLifecycleOwner(), resList -> {
            if (resList == null) {
                if (listAdapter.dataList.isEmpty()) {
                    onErrorStatus();
                }
            } else {
                listAdapter.submitListAndPurge(resList);
                if (resList.isEmpty()) {
                    onEmptyStatus();
                } else {
                    onContentStatus();
                }
            }
        });
    }

    private void clearFocus(View v) {
        mBinding.viewInputFgList.inputLayoutViewInput.clearFocus();
        BaseUtil.hideKeyboardCompat(mBinding.viewInputFgList.inputLayoutViewInput);
    }

    private void createRoom(View v) {
        // 获取用户输入文字
        TextInputLayout inputLayout = mBinding.viewInputFgList.inputLayoutViewInput;
        Editable text = mBinding.viewInputFgList.editTextViewInput.getText();
        String inputText = null;
        if (text != null) inputText = text.toString().trim();

        if (inputText != null && !inputText.isEmpty()) {
            int error = isValidRoomName(inputText);
            if (error != 0) {
                RoomUtil.showNameIllegalError(inputLayout, error);
            } else {
                String bgdId = requireContext().getString(R.string.room_portrait_format, (new Random().nextInt(14) + 1));
                mViewModel.createRoom(new RoomInfo(inputText, RoomConstant.userId, bgdId));
            }
        } else {
            RoomUtil.showNameIllegalError(inputLayout, R.string.room_please_input_name);
        }
    }


    private void goToRoomPage() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(RoomFragment.roomKey, tempRoom);
        findNavController().navigate(R.id.action_roomListFragment_to_roomFragment, bundle);
    }

    private void showPermissionRequestDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.room_permission_alert)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> launcher.launch(permissions)))
                .show();
    }

    private void showPermissionAlertDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.room_permission_refused)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    @StringRes
    private int isValidRoomName(@NonNull String name) {
        Pattern pattern = Pattern.compile(requireContext().getString(R.string.room_room_name_regex));
        if (!pattern.matcher(name).matches())
            return R.string.room_room_name_restrict;
        return 0;
    }

    private void onLoadingStatus(boolean on) {
        mBinding.swipeFgList.setRefreshing(on);
        mBinding.emptyImageFgList.setVisibility(on ? View.GONE : View.VISIBLE);
    }

    private void onEmptyStatus() {
        onLoadingStatus(false);
        int colorAccent = ContextCompat.getColor(requireContext(), R.color.room_colorAccent);
        mBinding.emptyViewFgList.setVisibility(View.VISIBLE);
        mBinding.emptyImageFgList.setImageTintList(ColorStateList.valueOf(colorAccent));
        mBinding.btnRefreshFgList.setTextColor(colorAccent);
        mBinding.btnRefreshFgList.setText(R.string.room_empty_click_to_refresh);
    }

    private void onErrorStatus() {
        onLoadingStatus(false);
        mBinding.emptyViewFgList.setVisibility(View.VISIBLE);
        mBinding.emptyImageFgList.setImageTintList(ColorStateList.valueOf(Color.RED));
        mBinding.btnRefreshFgList.setTextColor(Color.RED);
        mBinding.btnRefreshFgList.setText(R.string.room_error_click_to_refresh);
    }

    private void onContentStatus() {
        mBinding.swipeFgList.setRefreshing(false);
        mBinding.emptyViewFgList.setVisibility(View.GONE);
    }
}