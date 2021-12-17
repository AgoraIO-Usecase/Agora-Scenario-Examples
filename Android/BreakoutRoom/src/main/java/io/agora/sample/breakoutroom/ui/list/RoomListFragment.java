package io.agora.sample.breakoutroom.ui.list;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.navigation.Navigation;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.DividerDecoration;
import io.agora.example.base.OnItemClickListener;
import io.agora.sample.breakoutroom.BuildConfig;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomConstant;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.databinding.FragmentRoomListBinding;
import io.agora.sample.breakoutroom.databinding.ItemRoomListBinding;
import io.agora.sample.breakoutroom.ui.MainViewModel;
import io.agora.sample.breakoutroom.ui.room.RoomFragment;
import io.agora.syncmanager.rtm.SyncManager;

public class RoomListFragment extends BaseFragment<FragmentRoomListBinding> implements OnItemClickListener<RoomInfo> {

    public static final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    private MainViewModel mainViewModel;
    private RoomListViewModel mViewModel;

    private BaseRecyclerViewAdapter<ItemRoomListBinding, RoomInfo, ListViewHolder> listAdapter;

    private RoomInfo tempRoom;

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> {
        List<String> permissionsRefused = new ArrayList<>();
        for (String s : res.keySet()) {
            if (Boolean.TRUE != res.get(s))
                permissionsRefused.add(s);
        }
        if (!permissionsRefused.isEmpty()) {
            showPermissionAlertDialog();
        } else {
            goToRoomPage();
        }
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mViewModel = new ViewModelProvider(getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(RoomListViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom) + (int) BaseUtil.dp2px(12);

            mBinding.appBarFgRoomList.setPadding(0, inset.top, 0, 0);

            CoordinatorLayout.LayoutParams lpFab = (CoordinatorLayout.LayoutParams) mBinding.fabFgList.getLayoutParams();
            lpFab.bottomMargin = desiredBottom;

            return WindowInsetsCompat.CONSUMED;
        });

        initView();
        initListener();
    }

    private void initView() {
        // config Dialog
        RoomUtil.configInputDialog(mBinding.viewInputFgList.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgList.inputLayoutViewInput);
        mBinding.viewInputFgList.titleViewInput.setText(R.string.fab_add_room);

        // config RecyclerView
        listAdapter = new BaseRecyclerViewAdapter<>(null, this, ListViewHolder.class);
        mBinding.recyclerViewFgList.setAdapter(listAdapter);
        mBinding.recyclerViewFgList.addItemDecoration(new DividerDecoration(2));
        mBinding.swipeFgList.setProgressViewOffset(true, 0, mBinding.swipeFgList.getProgressViewEndOffset());
    }

    @Override
    public void onItemClick(@NonNull RoomInfo data, @NonNull View view, int position, long viewType) {
        mViewModel.joinRoom(data);
    }

    private void initListener() {
        // 清除所有房间
        if (BuildConfig.DEBUG) {
            mBinding.toolbarFgList.setOnLongClickListener(v -> {
                SyncManager.Instance().getScene(RoomConstant.globalChannel).delete(null);
                return true;
            });
        }

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
            } else if (viewStatus instanceof ViewStatus.Done)
                dismissLoading();
            else if (viewStatus instanceof ViewStatus.Error) {
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
                listAdapter.submitListWithDiffCallback(new RoomListDiffCallback(listAdapter.dataList, resList));
                if (resList.isEmpty()) {
                    onEmptyStatus();
                } else {
                    onContentStatus();
                }
            }
        });

        // 加入房间或者创建房间成功
        mViewModel.pendingRoomInfo().observe(getViewLifecycleOwner(), roomInfo -> {
            if (roomInfo == null) return;

            tempRoom = roomInfo;
            if (mBinding.scrimFgList.getVisibility() == View.VISIBLE)
                mBinding.scrimFgList.performClick();
            mBinding.fabFgList.setExpanded(false);
            mViewModel.clearPendingRoomInfo();


            checkPermissionBeforeGoNextPage();

        });
    }

    private void clearFocus(View v) {
        mBinding.viewInputFgList.inputLayoutViewInput.clearFocus();
        BaseUtil.hideKeyboard(requireActivity().getWindow(), mBinding.viewInputFgList.inputLayoutViewInput);
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
                String bgdId = requireContext().getString(R.string.portrait_format, (new Random().nextInt(14) + 1));
                mViewModel.createRoom(new RoomInfo(inputText, RoomConstant.userId, bgdId));
            }
        } else {
            RoomUtil.showNameIllegalError(inputLayout, R.string.please_input_name);
        }
    }


    private void goToRoomPage() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(RoomFragment.currentRoom, tempRoom);
        Navigation.findNavController(mBinding.getRoot()).navigate(R.id.action_roomListFragment_to_roomFragment, bundle);
    }

    private void checkPermissionBeforeGoNextPage() {
        // 小于 M 无需控制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            goToRoomPage();
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
            goToRoomPage();
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
        else showPermissionAlertDialog(permissions);
    }

    private void showPermissionAlertDialog() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.permission_refused)
                .setPositiveButton(android.R.string.ok, null).show();
    }


    private void showPermissionAlertDialog(@NonNull String[] permissions) {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.permission_alert).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> requestPermissionLauncher.launch(permissions))).show();
    }

    @StringRes
    private int isValidRoomName(@NonNull String name) {

        Pattern pattern = Pattern.compile(requireContext().getString(R.string.room_name_regex));
        if (!pattern.matcher(name).matches())
            return R.string.room_name_restrict;
        return 0;
    }

    private void onLoadingStatus(boolean on) {
        mBinding.swipeFgList.setRefreshing(on);
        mBinding.emptyImageFgList.setVisibility(on ? View.GONE : View.VISIBLE);
    }

    private void onEmptyStatus() {
        onLoadingStatus(false);
        int colorAccent = ContextCompat.getColor(requireContext(), R.color.colorAccent);
        mBinding.emptyViewFgList.setVisibility(View.VISIBLE);
        mBinding.emptyImageFgList.setImageTintList(ColorStateList.valueOf(colorAccent));
        mBinding.btnRefreshFgList.setTextColor(colorAccent);
        mBinding.btnRefreshFgList.setText(R.string.empty_click_to_refresh);
    }

    private void onErrorStatus() {
        onLoadingStatus(false);
        mBinding.emptyViewFgList.setVisibility(View.VISIBLE);
        mBinding.emptyImageFgList.setImageTintList(ColorStateList.valueOf(Color.RED));
        mBinding.btnRefreshFgList.setTextColor(Color.RED);
        mBinding.btnRefreshFgList.setText(R.string.error_click_to_refresh);
    }

    private void onContentStatus() {
        mBinding.swipeFgList.setRefreshing(false);
        mBinding.emptyImageFgList.setVisibility(View.GONE);
    }
}