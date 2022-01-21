package io.agora.scene.rtegame.ui.room.invite;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ThemeUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseBottomSheetDialogFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.example.base.OnItemClickListener;
import io.agora.scene.rtegame.GlobalViewModel;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.databinding.GameDialogHostListBinding;
import io.agora.scene.rtegame.databinding.GameItemDialogHostBinding;
import io.agora.scene.rtegame.ui.list.RoomListViewModel;
import io.agora.scene.rtegame.ui.room.RoomViewModel;
import io.agora.scene.rtegame.util.EventObserver;
import io.agora.scene.rtegame.util.GameUtil;
import io.agora.scene.rtegame.util.ViewStatus;

public class HostListDialog extends BaseBottomSheetDialogFragment<GameDialogHostListBinding> implements OnItemClickListener<RoomInfo> {
    public static final String TAG = "HostListDialog";

    @NonNull
    public String desiredGameId = "";

    private RoomViewModel roomViewModel;
    // 用于获取可PK主播
    private RoomListViewModel roomListViewModel;

    private BaseRecyclerViewAdapter<GameItemDialogHostBinding, RoomInfo, ItemHostHolder> mAdapter;

    public HostListDialog() {
    }

    public HostListDialog(@NonNull String desiredGameId) {
        this.desiredGameId = desiredGameId;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (desiredGameId.isEmpty()){
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
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, 0);
            mBinding.recyclerViewDialogHostList.setPaddingRelative(0,0,0,inset.bottom);

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.getRoot().getLayoutParams();
            layoutParams.topMargin = inset.top;
            mBinding.getRoot().setLayoutParams(layoutParams);

            FrameLayout.LayoutParams lp4Empty = (FrameLayout.LayoutParams) mBinding.emptyDialogHostList.getLayoutParams();
            lp4Empty.bottomMargin = inset.bottom;
            mBinding.emptyDialogHostList.setLayoutParams(lp4Empty);

            return WindowInsetsCompat.CONSUMED;
        });

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(requireDialog().getWindow().getDecorView());
        if (controller!=null) {
            boolean isNightMode = GameUtil.isNightMode(getResources().getConfiguration());
            controller.setAppearanceLightStatusBars(!isNightMode);
            controller.setAppearanceLightNavigationBars(!isNightMode);
        }

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
            if (tempRoomInfoList.get(i).getUserId().equals(GlobalViewModel.localUser.getUserId())){
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
