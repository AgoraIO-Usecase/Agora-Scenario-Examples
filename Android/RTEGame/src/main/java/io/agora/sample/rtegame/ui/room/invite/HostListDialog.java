package io.agora.sample.rtegame.ui.room.invite;

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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.OnItemClickListener;
import io.agora.sample.rtegame.GameApplication;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.DialogHostListBinding;
import io.agora.sample.rtegame.databinding.ItemDialogHostBinding;
import io.agora.sample.rtegame.ui.list.RoomListViewModel;
import io.agora.sample.rtegame.ui.room.RoomViewModel;
import io.agora.sample.rtegame.util.EventObserver;
import io.agora.sample.rtegame.util.GameUtil;
import io.agora.sample.rtegame.util.ViewStatus;

public class HostListDialog extends BaseBottomSheetDialogFragment<DialogHostListBinding> implements OnItemClickListener<RoomInfo> {
    public static final String TAG = "HostListDialog";

    public int desiredGameId = -1;

    private RoomViewModel roomViewModel;
    // 用于获取可PK主播
    private RoomListViewModel roomListViewModel;

    private BaseRecyclerViewAdapter<ItemDialogHostBinding, RoomInfo, ItemHostHolder> mAdapter;

    public HostListDialog() {
    }

    public HostListDialog(int desiredGameId) {
        this.desiredGameId = desiredGameId;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (desiredGameId == -1){
            dismiss();
            return;
        }
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);

        roomViewModel = GameUtil.getViewModel(requireParentFragment(), RoomViewModel.class);
        roomListViewModel = GameUtil.getViewModel(this, RoomListViewModel.class);

        initView();
        initListener();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, this, ItemHostHolder.class);
        GameUtil.setBottomDialogBackground(mBinding.getRoot());
        GameUtil.setBottomDialogBackground(mBinding.appBarDialogHostList);
        mBinding.recyclerViewDialogHostList.setAdapter(mAdapter);
        mBinding.appBarDialogHostList.setLiftable(true);
        mBinding.appBarDialogHostList.setLifted(false);
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mBinding.btnRefreshDialogHostList.setOnClickListener((v) -> roomListViewModel.fetchRoomList());
        mBinding.appBarDialogHostList.setOnClickListener(v -> mBinding.recyclerViewDialogHostList.smoothScrollToPosition(0));
        mBinding.recyclerViewDialogHostList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                mBinding.appBarDialogHostList.setLifted(recyclerView.canScrollVertically(-1));
            }
        });

        roomListViewModel.viewStatus().observe(getViewLifecycleOwner(), this::onViewStatusChanged);
        roomListViewModel.roomList().observe(getViewLifecycleOwner(), this::onFetchedRoomList);

        roomViewModel.pkResult().observe(getViewLifecycleOwner(), new EventObserver<>(data -> {
            if (data == Boolean.TRUE)
                dismiss();
            else if (data == Boolean.FALSE)
                BaseUtil.toast(requireContext(), "PK error.");
        }));
    }

    /**
     * 获取房间列表成功
     */
    private void onFetchedRoomList(List<RoomInfo> rooms) {
        List<RoomInfo> tempRoomInfoList = new ArrayList<>(rooms);
        // 排除自己
        for (int i = 0; i < tempRoomInfoList.size();) {
            if (tempRoomInfoList.get(i).getUserId().equals(GameApplication.getInstance().user.getUserId())){
                tempRoomInfoList.remove(i);
            }else i++;
        }
        // TODO TEST
//        if (new Random().nextBoolean())
//        for (int i = 0; i < 20; i++) {
//            tempRoomInfoList.add(new RoomInfo("room_test:"+i, ""+i));
//        }else tempRoomInfoList.clear();
        mAdapter.submitListAndPurge(tempRoomInfoList);
        mBinding.recyclerViewDialogHostList.setVisibility(tempRoomInfoList.isEmpty() ? GONE : VISIBLE);
        mBinding.emptyDialogHostList.setVisibility(tempRoomInfoList.isEmpty() ? VISIBLE : GONE);
    }

    /**
     * View状态改变
     */
    private void onViewStatusChanged(ViewStatus viewStatus) {
        if (viewStatus instanceof ViewStatus.Loading) {
            mBinding.recyclerViewDialogHostList.setVisibility(GONE);
            mBinding.loadingDialogHostList.setVisibility(VISIBLE);
        }else if (viewStatus instanceof ViewStatus.Done)
            mBinding.loadingDialogHostList.setVisibility(GONE);
    }

    /**
     * 游戏PK邀请
     */
    @Override
    public void onItemClick(@NonNull RoomInfo targetRoom, @NonNull View view, int position, long viewType) {
        if (view instanceof MaterialButton){
            roomViewModel.sendApplyPKInvite(roomViewModel, targetRoom, desiredGameId);
        }
    }
}
