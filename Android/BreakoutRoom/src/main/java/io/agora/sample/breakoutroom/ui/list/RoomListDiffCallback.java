package io.agora.sample.breakoutroom.ui.list;

import java.util.List;
import java.util.Objects;

import io.agora.example.base.BaseDiffCallback;
import io.agora.sample.breakoutroom.bean.RoomInfo;

public class RoomListDiffCallback extends BaseDiffCallback<RoomInfo> {

    public RoomListDiffCallback(List<RoomInfo> oldList, List<RoomInfo> newList) {
        super(oldList, newList);
    }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        return Objects.equals(getOldList().get(oldPos).getId(), getNewList().get(oldPos).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        return getOldList().get(oldPos).equals(getNewList().get(newPos));
    }

}
