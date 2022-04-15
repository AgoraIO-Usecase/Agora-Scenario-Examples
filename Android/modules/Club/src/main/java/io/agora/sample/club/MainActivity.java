package io.agora.sample.club;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import io.agora.example.base.BaseActivity;
import io.agora.sample.club.databinding.ClubMainActivityBinding;
import io.agora.uiwidget.utils.StatusBarUtil;

public class MainActivity extends BaseActivity<ClubMainActivityBinding> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.hideStatusBar(getWindow(), false);

        mBinding.titleBar
                .setBgDrawable(R.drawable.club_main_title_bar_bg)
                .setDeliverVisible(false)
                .setTitleName(getString(R.string.club_main_title), getResources().getColor(R.color.club_title_bar_text_color));
        mBinding.listItem01.ivIcon.setImageResource(R.drawable.club_main_banner1);
        mBinding.listItem01.tvTag.setVisibility(View.VISIBLE);
        mBinding.listItem01.tvTag.setText("2520人关注");
        mBinding.listItem01.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem02.ivIcon.setImageResource(R.drawable.club_main_banner2);
        mBinding.listItem02.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem02.tvTag.setVisibility(View.VISIBLE);
        mBinding.listItem02.tvTag.setText("33333人关注");
        mBinding.listItem03.tvTitle.setText("Latest Release");
        mBinding.listItem03.tvCard01.ivIcon.setImageResource(R.drawable.club_main_banner3);
        mBinding.listItem03.tvCard01.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem03.tvCard02.ivIcon.setImageResource(R.drawable.club_main_banner4);
        mBinding.listItem03.tvCard02.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem03.tvCard03.ivIcon.setImageResource(R.drawable.club_main_banner5);
        mBinding.listItem03.tvCard03.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem03.tvCard03.tvTag.setVisibility(View.VISIBLE);
        mBinding.listItem03.tvCard03.tvTag.setText("55555人关注");
        mBinding.listItem04.tvTitle.setText("Popular Parties");
        mBinding.listItem04.tvCard01.ivIcon.setImageResource(R.drawable.club_main_banner6);
        mBinding.listItem04.tvCard01.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem04.tvCard02.ivIcon.setImageResource(R.drawable.club_main_banner7);
        mBinding.listItem04.tvCard02.ivIcon.setOnClickListener(this::goToRoomList);
        mBinding.listItem04.tvCard03.ivIcon.setImageResource(R.drawable.club_main_banner8);
        mBinding.listItem04.tvCard03.ivIcon.setOnClickListener(this::goToRoomList);
    }

    private void goToRoomList(View view){
        startActivity(new Intent(this, RoomListActivity.class));
    }
}
