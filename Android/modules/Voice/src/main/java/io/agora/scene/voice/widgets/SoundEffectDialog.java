package io.agora.scene.voice.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Pair;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.agora.scene.voice.R;
import io.agora.scene.voice.databinding.VoiceSoundEffectTabCommingSoonBinding;
import io.agora.scene.voice.databinding.VoiceSoundEffectTabElectronicLayoutBinding;
import io.agora.scene.voice.databinding.VoiceSoundEffectTabGridviewBinding;
import io.agora.scene.voice.databinding.VoiceSoundEffectTabItemImageTextBinding;
import io.agora.scene.voice.databinding.VoiceSoundEffectTabItemTextBinding;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.basic.OnItemClickListener;
import io.agora.uiwidget.function.TabScrollDialog;

public class SoundEffectDialog extends TabScrollDialog {

    public static final int[] SPACE_EFFECT_IMAGE_RES = {
            R.drawable.voice_sound_effect_space_ktv,
            R.drawable.voice_sound_effect_space_concert,
            R.drawable.voice_sound_effect_space_studio,
            R.drawable.voice_sound_effect_space_phonograph,
            R.drawable.voice_sound_effect_space_stereo,
            R.drawable.voice_sound_effect_space_spacial,
            R.drawable.voice_sound_effect_space_ethereal,
            R.drawable.voice_sound_effect_space_3d_voice
    };

    public static final int[] CHANGE_EFFECT_IMAGE_RES = {
            R.drawable.voice_voice_beauty_chat_male_magnetic,
            R.drawable.voice_sound_effect_change_old_man,
            R.drawable.voice_sound_effect_change_boy,
            R.drawable.voice_sound_effect_change_sister,
            R.drawable.voice_sound_effect_change_girl,
            R.drawable.voice_sound_effect_change_bajie,
            R.drawable.voice_sound_effect_change_hawk,
    };

    public static final int[] FLAVOUR_EFFECT_IMAGE_RES = {
            R.drawable.voice_sound_effect_flavour_r_n_b,
            R.drawable.voice_sound_effect_flavour_pop,
            R.drawable.voice_sound_effect_flavour_rock,
            R.drawable.voice_sound_effect_flavour_hipop,
    };

    public static void showDialog(Context context) {
        Resources resources = context.getResources();
        String[] soundEffectTypeNames = resources.getStringArray(R.array.voice_sound_effect_types);
        new SoundEffectDialog(context)
                .setTabsTitle(resources.getString(R.string.voice_sound_effect_dialog_title))
                // 空间塑造
                .addTab(soundEffectTypeNames[0], createImageTextGridView(
                        resources.getStringArray(R.array.voice_sound_effect_space_names),
                        SPACE_EFFECT_IMAGE_RES,
                        0,
                        (item, position) -> {

                        }
                ))
                // 变声音效
                .addTab(soundEffectTypeNames[1], createImageTextGridView(
                        resources.getStringArray(R.array.voice_sound_effect_change_names),
                        CHANGE_EFFECT_IMAGE_RES,
                        0,
                        (item, position) -> {

                        }
                ))
                // 曲风音效
                .addTab(soundEffectTypeNames[2], createImageTextGridView(
                        resources.getStringArray(R.array.voice_sound_effect_style_names),
                        FLAVOUR_EFFECT_IMAGE_RES,
                        0,
                        (item, position) -> {

                        }
                ))
                // 电音音效
                .addTab(soundEffectTypeNames[3], createElectronicLayout(
                        (buttonView, isChecked) -> {

                        },
                        resources.getStringArray(R.array.voice_sound_effect_electronic_keys),
                        0,
                        (item, position) -> {

                        },
                        resources.getStringArray(R.array.voice_sound_effect_electronic_tones),
                        0,
                        (item, position) -> {

                        }
                ))
                // 魔力音阶
                .addTab(soundEffectTypeNames[4], createCommingSoon())
                .refresh()
                .show();
    }

    private SoundEffectDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();
        mViewBinding.tvTitle.setTextColor(Color.WHITE);
        mViewBinding.getRoot().setBackgroundResource(R.drawable.voice_dialog_bg);
        mViewBinding.tabLayout.setTabTextColors(Color.GRAY, Color.WHITE);
        mViewBinding.tabLayout.setBackgroundColor(Color.parseColor("#FF0A0F17"));
    }

    private static TabViewUpdater<VoiceSoundEffectTabGridviewBinding> createImageTextGridView(
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
        BindingSingleAdapter<Pair<String, Integer>, VoiceSoundEffectTabItemImageTextBinding> adapter = new BindingSingleAdapter<Pair<String, Integer>, VoiceSoundEffectTabItemImageTextBinding>() {
            private int _selectedPosition = selectedIndex;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceSoundEffectTabItemImageTextBinding> holder, int position) {
                final int _position = position;
                Pair<String, Integer> item = getItem(_position);
                holder.binding.tvText.setText(item.first);
                holder.binding.ivIcon.setImageResource(item.second);
                holder.binding.getRoot().setActivated(_selectedPosition == _position);
                holder.binding.getRoot().setOnClickListener(v -> {
                    if (_selectedPosition != _position) {
                        int lastItem = _selectedPosition;
                        _selectedPosition = _position;
                        if (onItemClickListener != null) {
                            onItemClickListener.onItemClick(item, _position);
                        }
                        notifyItemChanged(lastItem);
                        notifyItemChanged(_position);
                    }
                });
            }
        };
        adapter.insertAll(pairList);
        return new TabViewUpdater<VoiceSoundEffectTabGridviewBinding>() {
            @Override
            protected void onUpdate(BindingViewHolder<VoiceSoundEffectTabGridviewBinding> holder) {
                holder.binding.recyclerView.setAdapter(adapter);
            }
        };
    }

    private static TabViewUpdater<VoiceSoundEffectTabElectronicLayoutBinding> createElectronicLayout(
            CompoundButton.OnCheckedChangeListener enableCheckListener,
            String[] keyNames,
            int keySelectedIndex,
            OnItemClickListener<String> keyClickListener,
            String[] toneNames,
            int toneSelectedIndex,
            OnItemClickListener<String> toneClickListener
    ) {

        BindingSingleAdapter<String, VoiceSoundEffectTabItemTextBinding> keyAdapter = new BindingSingleAdapter<String, VoiceSoundEffectTabItemTextBinding>() {
            private int selectedPosition = keySelectedIndex;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceSoundEffectTabItemTextBinding> holder, int position) {
                final int _position = position;
                String item = getItem(_position);
                holder.binding.tvText.setText(item);
                holder.binding.flContainer.setActivated(_position == selectedPosition);
                holder.binding.flContainer.setOnClickListener(v -> {
                    if (selectedPosition != _position) {
                        int oPosition = selectedPosition;
                        selectedPosition = _position;
                        if (keyClickListener != null) {
                            keyClickListener.onItemClick(item, _position);
                        }
                        notifyItemChanged(oPosition);
                        notifyItemChanged(_position);
                    }
                });
            }
        };

        BindingSingleAdapter<String, VoiceSoundEffectTabItemTextBinding> toneAdapter = new BindingSingleAdapter<String, VoiceSoundEffectTabItemTextBinding>() {
            private int selectedPosition = toneSelectedIndex;

            @Override
            public void onBindViewHolder(@NonNull BindingViewHolder<VoiceSoundEffectTabItemTextBinding> holder, int position) {
                final int _position = position;
                String item = getItem(_position);
                holder.binding.tvText.setText(item);
                holder.binding.flContainer.setActivated(selectedPosition == _position);
                holder.binding.flContainer.setOnClickListener(v -> {
                    if (selectedPosition != _position) {
                        int oPosition = selectedPosition;
                        selectedPosition = _position;
                        if (toneClickListener != null) {
                            toneClickListener.onItemClick(item, _position);
                        }
                        notifyItemChanged(oPosition);
                        notifyItemChanged(_position);
                    }
                });
            }
        };

        return new TabViewUpdater<VoiceSoundEffectTabElectronicLayoutBinding>() {
            @Override
            protected void onUpdate(BindingViewHolder<VoiceSoundEffectTabElectronicLayoutBinding> holder) {
                holder.binding.switchElectronic.setOnCheckedChangeListener(enableCheckListener);
                holder.binding.rvKey.setAdapter(keyAdapter);
                holder.binding.rvTone.setAdapter(toneAdapter);

                keyAdapter.removeAll();
                keyAdapter.insertAll(keyNames);

                toneAdapter.removeAll();
                toneAdapter.insertAll(toneNames);
            }
        };
    }

    private static TabViewUpdater<VoiceSoundEffectTabCommingSoonBinding> createCommingSoon() {
        return new TabViewUpdater<VoiceSoundEffectTabCommingSoonBinding>() {
            @Override
            protected void onUpdate(BindingViewHolder<VoiceSoundEffectTabCommingSoonBinding> holder) {

            }
        };
    }
}
