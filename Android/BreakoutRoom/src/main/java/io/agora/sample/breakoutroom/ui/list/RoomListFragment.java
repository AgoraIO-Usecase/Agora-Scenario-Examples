package io.agora.sample.breakoutroom.ui.list;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.DividerDecoration;
import io.agora.example.base.OnItemClickListener;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomConstant;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.ViewStatus;
import io.agora.sample.breakoutroom.bean.RoomInfo;
import io.agora.sample.breakoutroom.databinding.FragmentRoomListBinding;
import io.agora.sample.breakoutroom.databinding.ItemSceneListBinding;
import io.agora.sample.breakoutroom.ui.MainViewModel;

public class RoomListFragment extends BaseFragment<FragmentRoomListBinding> implements OnItemClickListener<RoomInfo> {

    private MainViewModel mainViewModel;
    private RoomListViewModel mViewModel;

    private BaseRecyclerViewAdapter<ItemSceneListBinding, RoomInfo, ListViewHolder> listAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mViewModel = new ViewModelProvider(getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(RoomListViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom);

            v.setPadding(inset.left, inset.top, inset.right, desiredBottom);
            // FIXME find a better way to achieve this.
            ((CoordinatorLayout.LayoutParams) mBinding.scrimFgList.getLayoutParams()).setMargins(-inset.left, -inset.top - 1, -inset.right, -desiredBottom);
            return WindowInsetsCompat.CONSUMED;
        });

        initView();
        initListener();
        mViewModel.fetchRoomList();
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

    }

    @Override
    public void onItemClick(@NonNull RoomInfo data, View view, int position, long viewType) {
        mViewModel.joinRoom(data);
    }

    private void initListener() {
        // Inner Dialog stuff
        mBinding.viewInputFgList.getRoot().setOnClickListener(this::clearFocus);
        mBinding.viewInputFgList.btnConfirmViewLayout.setOnClickListener(this::createRoom);

        // Show Dialog stuff
        mBinding.fabFgList.setOnClickListener(v -> mBinding.fabFgList.setExpanded(true));
        mBinding.scrimFgList.setOnClickListener(v -> mBinding.fabFgList.setExpanded(false));

        // Empty View stuff
        mBinding.btnRefreshFgList.setOnClickListener(this::fetchRoomList);


        mViewModel.viewStatus().observe(getViewLifecycleOwner(), viewStatus -> {
            if (viewStatus instanceof ViewStatus.Loading)
                showLoading();
            else if (viewStatus instanceof ViewStatus.Done)
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
                listAdapter.setDataList(resList);
                if (resList.isEmpty()) {
                    onEmptyStatus();
                } else {
                    mBinding.emptyViewFgList.setVisibility(View.GONE);
                }
            }
        });

        // 加入房间或者创建房间成功
        mViewModel.pendingRoomInfo().observe(getViewLifecycleOwner(), roomInfo -> {
            if (roomInfo == null) return;

            mainViewModel.currentRoom = roomInfo;
            if (mBinding.scrimFgList.getVisibility() == View.VISIBLE)
                mBinding.scrimFgList.performClick();
            goToRoomPage();

            mViewModel.clearPendingRoomInfo();
        });
    }

    private void fetchRoomList(View v) {
        mViewModel.fetchRoomList();
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
            String bgdId = requireContext().getString(R.string.portrait_format, (new Random(System.currentTimeMillis()).nextInt(13) + 1));
            mViewModel.createRoom(new RoomInfo(inputText, RoomConstant.userId, bgdId));
        } else {
            if (inputLayout.isErrorEnabled())
                BaseUtil.shakeViewAndVibrateToAlert(inputLayout);
            else
                inputLayout.setError(getString(R.string.room_name_invalid));
        }
    }

    private void goToRoomPage() {
        mBinding.fabFgList.setExpanded(false);
        Navigation.findNavController(mBinding.getRoot()).navigate(R.id.action_roomListFragment_to_roomFragment);
    }

    private void onEmptyStatus() {
        int colorAccent = ContextCompat.getColor(requireContext(), R.color.colorAccent);
        mBinding.emptyViewFgList.setVisibility(View.VISIBLE);
        mBinding.emptyImageFgList.setImageTintList(ColorStateList.valueOf(colorAccent));
        mBinding.btnRefreshFgList.setTextColor(colorAccent);
    }

    private void onErrorStatus() {
        mBinding.emptyViewFgList.setVisibility(View.VISIBLE);
        mBinding.emptyImageFgList.setImageTintList(ColorStateList.valueOf(Color.RED));
        mBinding.btnRefreshFgList.setTextColor(Color.RED);
    }
}
