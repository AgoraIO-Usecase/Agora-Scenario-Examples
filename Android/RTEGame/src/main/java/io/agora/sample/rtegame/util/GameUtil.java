package io.agora.sample.rtegame.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.HashMap;
import java.util.Random;

import io.agora.example.base.BaseUtil;
import io.agora.sample.rtegame.R;
import io.agora.sample.rtegame.bean.RoomInfo;
import io.agora.sample.rtegame.ui.roompage.RoomViewModelFactory;
import io.agora.syncmanager.rtm.Scene;

public class GameUtil {
    private static String getGameURLByID(int gameId){
        if (gameId == 1){
            return "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html";
        } else return "";
    }
    private static final String[] nameList = {
            "一马当先",
            "二姓之好",
            "三生有幸",
            "四分五裂",
            "五光十色",
            "六神无主",
            "七上八下",
            "八面玲珑",
            "九霄云外",
            "十全十美",
    };

    @DrawableRes
    public static int getBgdFromRoomBgdId(String bgdId) {
        int i = 1;
        try {
            if (bgdId != null)
                i = Integer.parseInt(bgdId.toLowerCase().substring(8, 10));
        } catch (Exception ignored) {
        }
        switch (i) {
            case 1:
                return R.drawable.portrait01;
            case 2:
                return R.drawable.portrait02;
            case 3:
                return R.drawable.portrait03;
            case 4:
                return R.drawable.portrait04;
            case 5:
                return R.drawable.portrait05;
            case 6:
                return R.drawable.portrait06;
            case 7:
                return R.drawable.portrait07;
            case 8:
                return R.drawable.portrait08;
            case 9:
                return R.drawable.portrait09;
            case 10:
                return R.drawable.portrait10;
            case 11:
                return R.drawable.portrait11;
            case 12:
                return R.drawable.portrait12;
            case 13:
                return R.drawable.portrait13;
            default:
                return R.drawable.portrait14;
        }
    }

    @DrawableRes
    public static int getAvatarFromUserId(String userId) {
        return R.mipmap.ic_launcher;
    }

    @NonNull
    public static Scene getSceneFromRoomInfo(@NonNull RoomInfo roomInfo) {
        Scene scene = new Scene();
        scene.setId(roomInfo.getId());
        scene.setUserId(roomInfo.getUserId());

        HashMap<String, String> map = new HashMap<>();
        map.put("backgroundId", roomInfo.getBackgroundId());
        map.put("roomName", roomInfo.getRoomName());

        scene.setProperty(map);
        return scene;
    }

    public static String getRandomRoomName() {
        return getRandomRoomName(new Random().nextInt(10));
    }

    public static String getRandomRoomName(int number) {
        return nameList[number % 10];
    }

    public static void setBottomDialogBackground(@NonNull View view) {
        MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable();

        ShapeAppearanceModel shapeAppearanceModel = ShapeAppearanceModel.builder(view.getContext(),
                R.style.cornerNormalTopStyle, R.style.cornerNormalTopStyle).build();
        materialShapeDrawable.setShapeAppearanceModel(shapeAppearanceModel);

        int desiredColor = BaseUtil.getColorInt(view.getContext(), R.attr.colorSurface);
        desiredColor = getMaterialBackgroundColor(desiredColor);
        materialShapeDrawable.setFillColor(ColorStateList.valueOf(desiredColor));
        ViewCompat.setBackground(view, materialShapeDrawable);
    }

    public static int getMaterialBackgroundColor(int color) {
        if (color == Color.BLACK)
            return Color.parseColor("#2D2D2D");
        else return color;
    }

    public static <T extends ViewModel> T getViewModel(Fragment fragment, Class<T> viewModelClass) {
        return new ViewModelProvider(fragment.getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(viewModelClass);
    }

    public static <T extends ViewModel> T getViewModel(Fragment fragment, Class<T> viewModelClass, ViewModelProvider.NewInstanceFactory factory) {
        return new ViewModelProvider(fragment.getViewModelStore(), factory).get(viewModelClass);
    }

    public static <T extends ViewModel> T getViewModel(ComponentActivity activity, Class<T> viewModelClass) {
        return new ViewModelProvider(activity, new ViewModelProvider.NewInstanceFactory()).get(viewModelClass);
    }

}
