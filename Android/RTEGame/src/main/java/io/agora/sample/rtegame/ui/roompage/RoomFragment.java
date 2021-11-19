package io.agora.sample.rtegame.ui.roompage;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.GlobalViewModel;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.databinding.FragmentRoomBinding;
import io.agora.sample.rtegame.databinding.ItemRoomMessageBinding;
import io.agora.sample.rtegame.util.GameUtil;

public class RoomFragment extends BaseFragment<FragmentRoomBinding> {

    private GlobalViewModel mGlobalModel;
    private RoomViewModel mViewModel;

    private NavController navController;
    private BaseRecyclerViewAdapter<ItemRoomMessageBinding, String, MessageHolder> mMessageAdapter;

    private RoomInfo currentRoom;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlobalModel = new ViewModelProvider(requireActivity()).get(GlobalViewModel.class);
        navController = NavHostFragment.findNavController(this);
        currentRoom = mGlobalModel.roomInfo.getValue();
        if (currentRoom == null) {
            navController.navigate(R.id.action_roomFragment_to_roomCreateFragment);
        } else {
            mViewModel = new ViewModelProvider(getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(RoomViewModel.class);
            initView();
            initListener();
        }
    }

    private void initView() {
        mBinding.avatarHostFgRoom.setImageResource(GameUtil.getAvatarFromUserId(currentRoom.getUserId()));
        mBinding.nameHostFgRoom.setText(currentRoom.getTempUserName());

        // TEST
        List<String> mList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder("Message:");
            for (int j = 0; j < i; j++) {
                sb.append(i);
            }
            mList.add(sb.toString());
        }
        mMessageAdapter = new BaseRecyclerViewAdapter<>(mList, MessageHolder.class);
        mBinding.recyclerViewFgRoom.setAdapter(mMessageAdapter);
    }

    private void initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            int desiredBottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, inset.bottom);
            int desiredBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            // 整体
            mBinding.containerOverlayFgRoom.setPadding(inset.left, inset.top, inset.right, inset.bottom);
            // 输入框
            mBinding.inputLayoutFgRoom.setVisibility(insets.isVisible(WindowInsetsCompat.Type.ime())?View.VISIBLE:View.GONE);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mBinding.inputLayoutFgRoom.getLayoutParams();
            layoutParams.bottomMargin = desiredBottom;
            mBinding.inputLayoutFgRoom.setLayoutParams(layoutParams);

            return WindowInsetsCompat.CONSUMED;
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mGlobalModel.roomInfo.removeObservers(getViewLifecycleOwner());
                mGlobalModel.roomInfo.setValue(null);
                BaseUtil.logD("Back");
                navController.popBackStack();
            }
        });
        mBinding.btnExitFgRoom.setOnClickListener((v) -> requireActivity().onBackPressed());

        mBinding.inputFgRoom.setOnClickListener(v -> BaseUtil.showKeyboard(requireActivity().getWindow(), mBinding.editTextFgRoom));
    }


}
