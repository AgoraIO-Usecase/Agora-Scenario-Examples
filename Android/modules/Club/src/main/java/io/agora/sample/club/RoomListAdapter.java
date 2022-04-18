package io.agora.sample.club;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.Locale;
import java.util.Random;

import io.agora.sample.club.databinding.ClubRoomListItemBinding;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.function.RoomListView;
import io.agora.uiwidget.utils.RandomUtil;

public abstract class RoomListAdapter extends RoomListView.CustRoomListAdapter<RoomManager.RoomInfo, ClubRoomListItemBinding> {

    @Override
    protected void onItemUpdate(BindingViewHolder<ClubRoomListItemBinding> holder, RoomManager.RoomInfo item) {
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

        Context context = holder.itemView.getContext();
        holder.binding.tvName.setText(String.format(Locale.US, "%s(%s)", item.roomName, item.roomId));
        holder.itemView.setOnClickListener(v -> onItemClicked(v, item));
    }

    @Override
    protected void onLoadMore() {

    }

    protected void onItemClicked(View v, RoomManager.RoomInfo item) {
        gotoNextPageSafe(v, item);
    }

    protected void gotoNextPageSafe(View v, RoomManager.RoomInfo item) {
        Context context = v.getContext();
        checkPermission(context, () -> gotoRoomDetailPage(context, item));
    }

    private void checkPermission(Context context, Runnable granted) {
        String[] permissions = {Permission.CAMERA, Permission.RECORD_AUDIO};
        if (AndPermission.hasPermissions(context, permissions)) {
            if (granted != null) {
                granted.run();
            }
            return;
        }
        AndPermission.with(context)
                .runtime()
                .permission(permissions)
                .onGranted(data -> {
                    if (granted != null) {
                        granted.run();
                    }
                })
                .onDenied(data -> Toast.makeText(context, "The permission request failed.", Toast.LENGTH_SHORT).show())
                .start();
    }

    private void gotoRoomDetailPage(Context context, RoomManager.RoomInfo roomInfo) {
        Intent intent = new Intent(context, RoomDetailActivity.class);
        intent.putExtra("roomInfo", roomInfo);
        context.startActivity(intent);
    }
}
