package io.agora.sample.breakoutroom.ui.list;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import io.agora.example.base.BaseRecyclerViewAdapter;
import io.agora.sample.breakoutroom.R;
import io.agora.sample.breakoutroom.bean.SceneInfo;
import io.agora.sample.breakoutroom.databinding.ItemSceneListBinding;

public class ListViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<ItemSceneListBinding, SceneInfo> {

    public ListViewHolder(@NonNull ItemSceneListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(@Nullable SceneInfo sceneInfo, int selectedIndex) {
        if (sceneInfo != null) {
//        mBinding.bgdItemRoomList.setImageResource();
            mBinding.titleItemRoomList.setText(sceneInfo.getSceneId());
            mBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Navigation.findNavController(v).navigate(R.id.action_sceneListFragment_to_roomFragment);
                }
            });
        }
    }
}
