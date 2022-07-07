package io.agora.scene;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

import io.agora.example.base.BaseActivity;
import io.agora.scene.databinding.AppHomeActivityBinding;
import io.agora.scene.databinding.AppHomeItemImageBinding;
import io.agora.scene.databinding.AppHomeItemTextBinding;

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
    private final String TAG = "";
    private void configInsets() {
        try {
            throw new RuntimeException();
        }catch (Exception e){
            Log.d(TAG, "CameraVideoManager >> create thread=" + Thread.currentThread().getName() + "\n" + e);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.appBarApp.setPaddingRelative(mBinding.appBarApp.getPaddingStart(), inset.top, mBinding.appBarApp.getPaddingEnd(), mBinding.appBarApp.getPaddingBottom());
            mBinding.recyclerView.setPaddingRelative(0, 0, 0, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initModulesRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                ModuleInfo moduleInfo = ModulesConfig.instance.moduleInfo.get(position);
                if (moduleInfo.type == ModuleInfo.ModuleType.title) {
                    return 2;
                }
                return 1;
            }
        };

        layoutManager.setSpanSizeLookup(spanSizeLookup);
        mBinding.recyclerView.setLayoutManager(layoutManager);
        final List<ModuleInfo> moduleInfoList = ModulesConfig.instance.moduleInfo;
        mBinding.recyclerView.setAdapter(new RecyclerView.Adapter<BindingViewHolder>() {

            @NonNull
            @Override
            public BindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                if(viewType == ModuleInfo.ModuleType.content){
                    return new BindingViewHolder(AppHomeItemImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
                }
                else{
                    return new BindingViewHolder(AppHomeItemTextBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
                }
            }

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder holder, int position) {
                ModuleInfo moduleInfo = moduleInfoList.get(position);
                if(getItemViewType(position) == ModuleInfo.ModuleType.content){
                    AppHomeItemImageBinding binding = holder.getBinding(AppHomeItemImageBinding.class);
                    binding.homeItemBg.setImageResource(moduleInfo.bgImageRes);
                    binding.homeItemName.setText(moduleInfo.nameRes);
                    holder.mBinding.getRoot().setOnClickListener(v -> {
                        try {
                            Intent intent = new Intent(HomeActivity.this, Class.forName(moduleInfo.launchClassName));
                            intent.putExtra("from", "appHomeActivity");
                            startActivity(intent);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
                }else{
                    holder.getBinding(AppHomeItemTextBinding.class).text.setText(moduleInfo.titleRes);
                }
            }

            @Override
            public int getItemCount() {
                return moduleInfoList.size();
            }

            @Override
            public int getItemViewType(int position) {
                return moduleInfoList.get(position).type;
            }
        });
    }

    private static final class BindingViewHolder extends RecyclerView.ViewHolder {
        private final ViewBinding mBinding;

        public BindingViewHolder(@NonNull ViewBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        public <T> T getBinding(Class<T> tClass) {
            if (tClass.isInstance(mBinding)) {
                return (T) mBinding;
            }
            throw new RuntimeException();
        }
    }
}
