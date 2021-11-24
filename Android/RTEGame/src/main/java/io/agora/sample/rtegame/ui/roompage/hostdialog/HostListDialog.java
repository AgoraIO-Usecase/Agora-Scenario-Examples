package io.agora.sample.rtegame.ui.roompage.hostdialog;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.OnItemClickListener;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.DialogHostListBinding;
import io.agora.sample.rtegame.databinding.ItemDialogHostBinding;
import io.agora.sample.rtegame.ui.listpage.RoomListViewModel;
import io.agora.sample.rtegame.ui.roompage.RoomViewModel;
import io.agora.sample.rtegame.util.EventObserver;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.util.ViewStatus;

public class HostListDialog extends BaseBottomSheetDialogFragment<DialogHostListBinding> implements OnItemClickListener<RoomInfo> {
    public static final String TAG = "HostListDialog";

    // 用于更新直播间状态
    private RoomViewModel roomViewModel;
    // 用于发起PK
    private HostListViewModel hostListViewModel;
    // 用于获取可PK主播
    private RoomListViewModel roomListViewModel;

    private BaseRecyclerViewAdapter<ItemDialogHostBinding, RoomInfo, ItemHostHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);

        roomViewModel = GameUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        hostListViewModel = GameUtil.getViewModel(this, HostListViewModel.class);
        roomListViewModel = GameUtil.getViewModel(this, RoomListViewModel.class);

        initView();
        initListener();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, ItemHostHolder.class);
        mBinding.recyclerViewDialogHostList.setAdapter(mAdapter);
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
        GameUtil.setBottomDialogBackground(mBinding.appBarDialogHostList);
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mBinding.btnRefreshDialogHostList.setOnClickListener((v) -> roomListViewModel.fetchRoomList());

        roomListViewModel.viewStatus().observe(getViewLifecycleOwner(), this::onViewStatusChanged);
        roomListViewModel.roomList().observe(getViewLifecycleOwner(), this::onFetchedRoomList);
        hostListViewModel.pkResult().observe(getViewLifecycleOwner(), new EventObserver<>(data -> {
            if (data == Boolean.TRUE)
                dismiss();
            else if (data == Boolean.FALSE)
                BaseUtil.toast(requireContext(), "PK error.");
        }));
        mBinding.appBarDialogHostList.setOnClickListener(v -> mBinding.recyclerViewDialogHostList.smoothScrollToPosition(0));
    }

    /**
     * 获取房间列表成功
     */
    private void onFetchedRoomList(List<RoomInfo> rooms) {
        // 排除自己
        for (int i = 0; i < rooms.size();) {
            if (rooms.get(i).getUserId().equals(GameApplication.getInstance().user.getUserId())){
                rooms.remove(i);
            }else i++;
        }
        // TODO TEST
//        for (int i = 0; i < 20; i++) {
//            rooms.add(new RoomInfo("room_test:"+i, ""+i));
//        }
        mAdapter.submitListAndPurge(rooms);
        mBinding.emptyDialogHostList.setVisibility(rooms.isEmpty() ? VISIBLE : GONE);
    }

    /**
     * View状态改变成功
     */
    private void onViewStatusChanged(ViewStatus viewStatus) {
        if (viewStatus instanceof ViewStatus.Loading)
            mBinding.loadingDialogHostList.setVisibility(VISIBLE);
        else if (viewStatus instanceof ViewStatus.Done)
            mBinding.loadingDialogHostList.setVisibility(GONE);
    }

    /**
     * 游戏PK邀请
     */
    @Override
    public void onItemClick(@NonNull RoomInfo targetRoom, View view, int position, long viewType) {
        if (view instanceof MaterialButton){
            hostListViewModel.sendPKInvite(roomViewModel, targetRoom, 1);
        }
    }
}
