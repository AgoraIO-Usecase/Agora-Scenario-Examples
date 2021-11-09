package io.agora.sample.breakoutroom.ui.list;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputLayout;

import io.agora.example.base.BaseFragment;
import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.example.base.BaseUtil;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.RoomUtil;
import io.agora.sample.breakoutroom.bean.SceneInfo;
import io.agora.sample.breakoutroom.databinding.FragmentRoomListBinding;
import io.agora.sample.breakoutroom.databinding.ItemSceneListBinding;

public class RoomListFragment extends BaseFragment<FragmentRoomListBinding> {

    private ListViewModel mViewModel;

    private BaseRecyclerViewAdapter<ItemSceneListBinding, SceneInfo, ListViewHolder> listAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ListViewModel.class);

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
    }

    private void initView() {
        // config Dialog
        RoomUtil.configInputDialog(mBinding.viewInputFgList.getRoot());
        RoomUtil.configTextInputLayout(mBinding.viewInputFgList.inputLayoutViewInput);
        mBinding.viewInputFgList.titleViewInput.setText(R.string.fab_add_room);

        // config RecyclerView
        listAdapter = new BaseRecyclerViewAdapter<>(null, ListViewHolder.class);
        mBinding.recyclerViewFgList.setAdapter(listAdapter);

    }

    private void initListener() {
        // Inner Dialog stuff
        mBinding.viewInputFgList.getRoot().setOnClickListener(this::clearFocus);
        mBinding.viewInputFgList.btnConfirmViewLayout.setOnClickListener(this::createRoom);

        // Show Dialog stuff
        mBinding.fabFgList.setOnClickListener(v -> mBinding.fabFgList.setExpanded(true));
        mBinding.scrimFgList.setOnClickListener(v -> mBinding.fabFgList.setExpanded(false));

        // Empty View stuff
        mBinding.btnRefreshFgList.setOnClickListener(this::fetchData);

        mViewModel.sceneInfoList().observe(getViewLifecycleOwner(), sceneInfoList -> {
            listAdapter.setDataList(sceneInfoList);
            dismissLoading();
            mBinding.emptyViewFgList.setVisibility(sceneInfoList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void fetchData(View v) {

    }

    private void clearFocus(View v) {
        mBinding.viewInputFgList.inputLayoutViewInput.clearFocus();
        BaseUtil.hideKeyboard(requireActivity().getWindow(), mBinding.viewInputFgList.inputLayoutViewInput);
    }

    private void createRoom(View v) {
        TextInputLayout inputLayout = mBinding.viewInputFgList.inputLayoutViewInput;
        if (RoomUtil.isInputValid(inputLayout)) {
            mBinding.fabFgList.setExpanded(false);
            Navigation.findNavController(v).navigate(R.id.action_sceneListFragment_to_roomFragment);
        } else {
            if(inputLayout.isErrorEnabled())
                BaseUtil.shakeViewAndVibrateToAlert(inputLayout);
            else
                inputLayout.setError(getString(R.string.room_name_invalid));
        }
    }
}
