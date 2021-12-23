package io.agora.scene;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.example.base.BaseActivity;
import io.agora.example.base.BaseUtil;
import io.agora.scene.databinding.AppHomeActivityBinding;
import io.agora.scene.databinding.AppHomeItemBinding;

/**
 * 首页
 */
public class HomeActivity extends BaseActivity<AppHomeActivityBinding> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configInsets();
        initModulesRecyclerView();
    }

    private void configInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.appBarApp.setPaddingRelative(mBinding.appBarApp.getPaddingStart(), inset.top, mBinding.appBarApp.getPaddingEnd(), mBinding.appBarApp.getPaddingBottom());
            mBinding.recyclerView.setPaddingRelative(0, 0, 0, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initModulesRecyclerView() {
        mBinding.recyclerView.setAdapter(new RecyclerView.Adapter<ModuleItemViewHolder>() {
            @NonNull
            @Override
            public ModuleItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ModuleItemViewHolder(AppHomeItemBinding.inflate(LayoutInflater.from(parent.getContext()),parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ModuleItemViewHolder holder, int position) {
                ModuleInfo moduleInfo = ModulesConfig.instance.moduleInfo.get(position);
                holder.mBinding.homeItemBg.setImageResource(moduleInfo.bgImageRes);
                holder.mBinding.homeItemName.setText(moduleInfo.nameRes);
                holder.mBinding.homeItemDescription.setText(moduleInfo.descriptionRes);
                holder.mBinding.getRoot().setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(HomeActivity.this, Class.forName(moduleInfo.mLaunchClassName)));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return ModulesConfig.instance.moduleInfo.size();
            }
        });
    }

    private static final class ModuleItemViewHolder extends RecyclerView.ViewHolder {

        private final AppHomeItemBinding mBinding;

        public ModuleItemViewHolder(AppHomeItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
            mBinding.homeItemBg.setImageTintList(BaseUtil.getScrimColorSelector(mBinding.getRoot().getContext()));
        }

    }
}
