package io.agora.scene.voice.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import io.agora.scene.voice.R;
import io.agora.scene.voice.databinding.VoiceVoiceBeautyTabGridviewBinding;
import io.agora.scene.voice.databinding.VoiceVoiceBeautyTabItemImageTextBinding;
import io.agora.scene.voice.databinding.VoiceVoiceBeautyTabItemTextBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.basic.OnItemClickListener;
import io.agora.uiwidget.function.TabScrollDialog;

public class VoiceBeautyDialog extends TabScrollDialog {
    private static final int[] VOICE_BEAUTY_CHAT_RES = {
            R.drawable.voice_voice_beauty_chat_male_magnetic,
            R.drawable.voice_voice_beauty_chat_female_fresh,
            R.drawable.voice_voice_beauty_chat_female_vatality
    };

    public static TabScrollDialog createDialog(Context context,
                                               OnItemClickListener<Pair<String, Integer>> chatBeautifierItemClick,
                                               OnItemClickListener<String> singingBeautifierItemClick,
                                               OnItemClickListener<String> timbreTransformationItemClick){
        Resources resources = context.getResources();

        return new VoiceBeautyDialog(context)
                .setTabsTitle(resources.getString(R.string.voice_voice_beauty_title))
                .addTab(resources.getString(R.string.voice_voice_beauty_type_title_1), createImageTextGridView(
                        resources.getStringArray(R.array.voice_voice_beauty_chat_names),
                        VOICE_BEAUTY_CHAT_RES,
                        0,
                        chatBeautifierItemClick
                ))
                .addTab(resources.getString(R.string.voice_voice_beauty_type_title_2), createTextGridView(
                        3,
                        resources.getStringArray(R.array.voice_voice_beauty_sing_names_simple),
                        0,
                        singingBeautifierItemClick
                ))
                .addTab(resources.getString(R.string.voice_voice_beauty_type_title_3), createTextGridView(
                        4,
                        resources.getStringArray(R.array.voice_voice_beauty_timbre),
                        0,
                        timbreTransformationItemClick
                ))
                .refresh();
    }

    private VoiceBeautyDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();
        mViewBinding.tvTitle.setTextColor(Color.WHITE);
        mViewBinding.getRoot().setBackgroundResource(R.drawable.voice_dialog_bg);
        mViewBinding.tabLayout.setTabTextColors(Color.GRAY, Color.WHITE);
        mViewBinding.tabLayout.setBackgroundColor(Color.parseColor("#FF0A0F17"));
        mViewBinding.tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
    }

    private static TabViewUpdater<VoiceVoiceBeautyTabGridviewBinding> createImageTextGridView(
            String[] textList,
            int[] imgResList,
            int selectedIndex,
            OnItemClickListener<Pair<String, Integer>> onItemClickListener
    ) {
        int pairSize = Math.min(textList.length, imgResList.length);
        List<Pair<String, Integer>> pairList = new ArrayList<>();
        for (int i = 0; i < pairSize; i++) {
            pairList.add(new Pair<>(textList[i], imgResList[i]));
        }
        BindingSingleAdapter<Pair<String, Integer>, VoiceVoiceBeautyTabItemImageTextBinding> adapter = new BindingSingleAdapter<Pair<String, Integer>, VoiceVoiceBeautyTabItemImageTextBinding>() {
            private int _selectedPosition = selectedIndex;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceVoiceBeautyTabItemImageTextBinding> holder, int position) {
                final int _position = position;
                Pair<String, Integer> item = getItem(_position);
                holder.binding.tvText.setText(item.first);
                holder.binding.ivIcon.setImageResource(item.second);
                holder.binding.getRoot().setActivated(_selectedPosition == _position);
                holder.binding.getRoot().setOnClickListener(v -> {
                    if(_selectedPosition != _position){
                        int lastItem = _selectedPosition;
                        _selectedPosition = _position;
                        if(onItemClickListener != null){
                            onItemClickListener.onItemClick(item, _position);
                        }
                        notifyItemChanged(lastItem);
                        notifyItemChanged(_position);
                    }
                });
            }
        };
        adapter.insertAll(pairList);
        return new TabViewUpdater<VoiceVoiceBeautyTabGridviewBinding>() {
            @Override
            protected void onUpdate(BindingViewHolder<VoiceVoiceBeautyTabGridviewBinding> holder) {
                holder.binding.recyclerView.setAdapter(adapter);
            }
        };
    }

    private static TabViewUpdater<VoiceVoiceBeautyTabGridviewBinding> createTextGridView(
            int spanCount,
            String[] textList,
            int selectedIndex,
            OnItemClickListener<String> onItemClickListener
    ){

        BindingSingleAdapter<String, VoiceVoiceBeautyTabItemTextBinding> adapter = new BindingSingleAdapter<String, VoiceVoiceBeautyTabItemTextBinding>() {
            private int selectedPosition = selectedIndex;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceVoiceBeautyTabItemTextBinding> holder, int position) {
                final int _position = position;
                String item = getItem(_position);
                holder.binding.tvText.setText(item);
                holder.binding.flContainer.setActivated(selectedPosition == _position);
                holder.binding.flContainer.setOnClickListener(v -> {
                    if(selectedPosition != _position){
                        int oPosition = selectedPosition;
                        selectedPosition = _position;
                        if(onItemClickListener != null){
                            onItemClickListener.onItemClick(item, _position);
                        }
                        notifyItemChanged(oPosition);
                        notifyItemChanged(_position);
                    }
                });
            }
        };
        adapter.insertAll(textList);

        return new TabViewUpdater<VoiceVoiceBeautyTabGridviewBinding>() {
            @Override
            protected void onUpdate(BindingViewHolder<VoiceVoiceBeautyTabGridviewBinding> holder) {
                ((GridLayoutManager)(holder.binding.recyclerView.getLayoutManager())).setSpanCount(spanCount);
                holder.binding.recyclerView.setAdapter(adapter);
            }
        };
    }
}
