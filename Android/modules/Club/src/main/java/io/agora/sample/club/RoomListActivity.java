package io.agora.sample.club;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.Random;

import io.agora.example.base.BaseActivity;
import io.agora.sample.club.RoomManager.RoomInfo;
import io.agora.sample.club.databinding.ClubRoomListActivityBinding;
import io.agora.sample.club.databinding.ClubRoomListItemBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.RandomUtil;

public class RoomListActivity extends BaseActivity<ClubRoomListActivityBinding> {
    private final String TAG = "RoomListActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RoomManager.getInstance().init(this, getString(R.string.rtm_app_id), getString(R.string.rtm_app_token));

        mBinding.titleBar
                .setBgDrawable(R.drawable.club_main_title_bar_bg)
                .setDeliverVisible(false)
                .setTitleName(getString(R.string.club_room_list_title), getResources().getColor(R.color.club_title_bar_text_color))
                .setBackIcon(true, R.drawable.title_bar_back_white, v -> finish());
        mBinding.btnStartLive.setOnClickListener(v -> gotoPreviewPage());
        mBinding.roomListView.setListAdapter(new RoomListView.CustRoomListAdapter<RoomInfo, ClubRoomListItemBinding>() {

            @Override
            protected void onItemUpdate(BindingViewHolder<ClubRoomListItemBinding> holder, RoomInfo item) {

                ImageView[] ivCrowns = new ImageView[]{
                        holder.binding.ivCrown01,
                        holder.binding.ivCrown02,
                        holder.binding.ivCrown03,
                        holder.binding.ivCrown04,
                        holder.binding.ivCrown05,
                };
                int crownValue = new Random().nextInt(ivCrowns.length);
                for (int i = 0; i < ivCrowns.length; i++) {
                    if (i > crownValue) {
                        ivCrowns[i].setVisibility(View.GONE);
                    } else {
                        ivCrowns[i].setVisibility(View.VISIBLE);
                    }
                }

                ImageView[] userIcons = new ImageView[]{
                        holder.binding.ivUser01,
                        holder.binding.ivUser02,
                        holder.binding.ivUser03,
                        holder.binding.ivUser04,
                        holder.binding.ivUser05,
                        holder.binding.ivUser06,
                        holder.binding.ivUser07,
                        holder.binding.ivUser08,
                };
                int userCount = new Random().nextInt(userIcons.length);
                for (int i = 0; i < userIcons.length; i++) {
                    if (i > userCount) {
                        userIcons[i].setVisibility(View.GONE);
                    } else {
                        userIcons[i].setVisibility(View.VISIBLE);
                        userIcons[i].setImageResource(RandomUtil.randomLiveRoomIcon());
                    }
                }

                holder.binding.tvName.setText(item.roomName);
                holder.itemView.setOnClickListener(v -> gotoAudiencePage(item));
            }

            @Override
            protected void onRefresh() {
                runOnUiThread(() -> {
                    int position = mDataList.size();
                    mDataList.add(new RoomInfo("adsfasdfg"));
                    notifyItemInserted(position);
                    triggerDataListUpdateRun();
                });

//                RoomManager.getInstance().getAllRooms(new RoomManager.DataListCallback<RoomInfo>() {
//                    @Override
//                    public void onSuccess(List<RoomInfo> dataList) {
//
//                    }
//
//                    @Override
//                    public void onFailed(Exception e) {
//                        Log.e(TAG, "", e);
//                        runOnUiThread(() -> {
//                            triggerDataListUpdateRun();
//                        });
//                    }
//                });
            }

            @Override
            protected void onLoadMore() {

            }
        });

    }


    private void gotoPreviewPage() {
        startActivity(new Intent(RoomListActivity.this, PreviewActivity.class));
    }

    private void gotoAudiencePage(RoomInfo roomInfo) {
        Intent intent = new Intent(RoomListActivity.this, AudienceDetailActivity.class);
        intent.putExtra("roomInfo", roomInfo);
        startActivity(intent);
    }
}
