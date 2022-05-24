package io.agora.scene.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.example.base.BaseActivity;
import io.agora.scene.ModuleInfo;
import io.agora.scene.ModulesConfig;
import io.agora.scene.R;
import io.agora.scene.databinding.HomeActivityBinding;
import io.agora.scene.databinding.HomeItemBinding;

/**
 * 首页
 *
 */
public class HomeActivity extends BaseActivity<HomeActivityBinding>{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTitleBar();
        initModulesRecyclerView();
    }

    private void initTitleBar() {
        mBinding.titleBar.setTitleName(getString(R.string.home_title), Color.BLACK);
        mBinding.titleBar.setBackIcon(false, View.NO_ID, null);
        mBinding.titleBar.setUserIcon(false, View.NO_ID, null);
    }

    private void initModulesRecyclerView(){
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerView.setAdapter(new RecyclerView.Adapter<ModuleItemViewHolder>() {
            @NonNull
            @Override
            public ModuleItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ModuleItemViewHolder(HomeItemBinding.inflate(LayoutInflater.from(parent.getContext())));
            }

            @Override
            public void onBindViewHolder(@NonNull ModuleItemViewHolder holder, int position) {
                ModuleInfo moduleInfo = ModulesConfig.getInstance().moduleInfos.get(position);
                holder.binding.homeItemBg.setImageResource(moduleInfo.bgImageRes);
                holder.binding.homeItemName.setText(moduleInfo.nameRes);
                holder.binding.homeItemDescription.setText(moduleInfo.descriptionRes);
                holder.binding.getRoot().setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, moduleInfo.targetActivity)));
            }

            @Override
            public int getItemCount() {
                return ModulesConfig.getInstance().moduleInfos.size();
            }
        });
    }

    private static final class ModuleItemViewHolder extends RecyclerView.ViewHolder{

        private final io.agora.scene.databinding.HomeItemBinding binding;

        public ModuleItemViewHolder(HomeItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }
}
