package io.agora.scene.comlive.bean;

import androidx.annotation.NonNull;

public class GiftInfo {
//    gif名称, 列: SuperBell
    private final String gifName;
//    金币
    private int coin;
//    礼物名称
    private final String title;
//    刷礼物的用户ID
    private final String userId;
    private final int giftType;

    public GiftInfo(@NonNull String gifName, int coin, @NonNull String title, @NonNull String userId) {
        this.gifName = gifName;
        this.coin = coin;
        this.title = title;
        this.userId = userId;
        this.giftType = (coin / 10) % 5;
    }

    @NonNull
    public String getGifName() {
        return gifName;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    public int getCoin() {
        return coin;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public int getGiftType() {
        return giftType;
    }

    @NonNull
    @Override
    public String toString() {
        return "GiftInfo{" +
                "gifName='" + gifName + '\'' +
                ", coin=" + coin +
                ", title='" + title + '\'' +
                ", userId='" + userId + '\'' +
                ", giftType=" + giftType +
                '}';
    }
}
