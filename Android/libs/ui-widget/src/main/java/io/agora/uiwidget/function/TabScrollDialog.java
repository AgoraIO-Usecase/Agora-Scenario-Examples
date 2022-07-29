package io.agora.uiwidget.function;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.uiwidget.R;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.TabScrollLayoutBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class TabScrollDialog extends BottomSheetDialog {

    protected TabScrollLayoutBinding mViewBinding;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mVPAdapter;
    private final List<Tab<?>> mVPTabs = new ArrayList<>();
    private final List<Tab<?>> mVPPendingAddTabs = new ArrayList<>();
    private final Map<Class<?>, Integer> mVPItemViewTypes = new HashMap<>();
    private OnTabChangedListener onTabChangedListener;

    public TabScrollDialog(@NonNull Context context) {
        super(context, R.style.BottomSheetDialog);
        init();
    }

    protected void init() {
        setCanceledOnTouchOutside(true);
        mViewBinding = TabScrollLayoutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(mViewBinding.getRoot());
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mVPAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                Class<?> bindingClass = null;
                for (Map.Entry<Class<?>, Integer> entry : mVPItemViewTypes.entrySet()) {
                    if(entry.getValue() == viewType){
                        bindingClass = entry.getKey();
                    }
                }
                if(bindingClass == null){
                    throw new RuntimeException("can not find the binding class of viewType: " + viewType);
                }
                ViewBinding binding = null;
                try {
                    binding = (ViewBinding) bindingClass.getDeclaredMethod("inflate",
                            LayoutInflater.class, ViewGroup.class, boolean.class)
                            .invoke(null, LayoutInflater.from(parent.getContext()), parent, false);
                } catch (Exception e) {
                    throw new RuntimeException("Binding inflate error: " + e);
                }
                return new BindingViewHolder<>(binding);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Tab<?> tab = mVPTabs.get(position);
                tab.onBindViewHolder(holder);
            }

            @Override
            public int getItemCount() {
                return mVPTabs.size();
            }

            @Override
            public int getItemViewType(int position) {
                Tab<?> tab = mVPTabs.get(position);
                Integer type = mVPItemViewTypes.get(tab.bindingClass);
                if(type == null){
                    type = mVPItemViewTypes.size();
                    mVPItemViewTypes.put(tab.bindingClass, type);
                }
                return type;
            }
        };
        mViewBinding.viewPager2.setAdapter(mVPAdapter);

        mViewBinding.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                mViewBinding.tabLayout.setScrollPosition(position, positionOffset, false);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mViewBinding.tabLayout.selectTab(mViewBinding.tabLayout.getTabAt(position), true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        mViewBinding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                mViewBinding.viewPager2.setCurrentItem(position);

                onTabChanged(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    protected void onTabChanged(int position) {
        if (onTabChangedListener != null) {
            onTabChangedListener.onTabChanged(position);
        }
    }

    public TabScrollDialog setTabsTitle(CharSequence title){
        mViewBinding.tvTitle.setText(title);
        return this;
    }

    public TabScrollDialog setOnTabChangedListener(OnTabChangedListener onTabChangedListener) {
        this.onTabChangedListener = onTabChangedListener;
        return this;
    }

    public <T extends ViewBinding> TabScrollDialog addTab(String title, TabViewUpdater<T> updater){
        mVPPendingAddTabs.add(new Tab<>(title, updater));
        return this;
    }

    public TabScrollDialog refresh(){
        for (Tab<?> tab : mVPPendingAddTabs) {
            TabLayout.Tab _tab = mViewBinding.tabLayout.newTab();
            _tab.setText(tab.title);
            mViewBinding.tabLayout.addTab(_tab);
        }
        int oSize = mVPTabs.size();
        mVPTabs.addAll(mVPPendingAddTabs);
        int nSize = mVPTabs.size();
        mVPPendingAddTabs.clear();
        mVPAdapter.notifyItemRangeChanged(oSize, nSize - oSize);
        return this;
    }

    private final static class Tab<T extends ViewBinding> {
        private final String title;
        private final TabViewUpdater<T> updater;
        private Class<T> bindingClass;

        private Tab(String title, TabViewUpdater<T> updater){
            this.title = title;
            this.updater = updater;
            try {
                bindingClass = (Class<T>) ((ParameterizedType) updater.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void onBindViewHolder(RecyclerView.ViewHolder holder){
            if(updater != null){
                updater.onUpdate((BindingViewHolder<T>) holder);
            }
        }
    }

    public abstract static class TabViewUpdater<T extends ViewBinding> {
        protected abstract void onUpdate(BindingViewHolder<T> holder);
    }

    public interface OnTabChangedListener {
        void onTabChanged(int position);
    }

}
