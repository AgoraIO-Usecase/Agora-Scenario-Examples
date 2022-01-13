package io.agora.scene.onelive.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.HashMap;
import java.util.Random;

import io.agora.example.base.BaseUtil;
import io.agora.scene.onelive.GlobalViewModelFactory;
import io.agora.scene.onelive.R;
import io.agora.scene.onelive.bean.RoomInfo;
import io.agora.syncmanager.rtm.Scene;

public class OneUtil {
    private static final String[] avatarList = {
            "https://terrigen-cdn-dev.marvel.com/content/prod/1x/012scw_ons_crd_02.jpg",
            "https://terrigen-cdn-dev.marvel.com/content/prod/1x/003cap_ons_crd_03.jpg",
            "https://terrigen-cdn-dev.marvel.com/content/prod/1x/011blw_ons_crd_04.jpg",
            "https://terrigen-cdn-dev.marvel.com/content/prod/1x/009drs_ons_crd_02.jpg",
            "https://terrigen-cdn-dev.marvel.com/content/prod/1x/017lok_ons_crd_03.jpg",
            "https://terrigen-cdn-dev.marvel.com/content/prod/1x/004tho_ons_crd_03.jpg"};

    @NonNull
    public static String randomAvatar(){
        int index = new Random().nextInt(10000);
        return avatarList[index % avatarList.length];
    }


    private static final String[] nameList = {
           "Tokyo",
           "Delhi",
           "Shanghai",
           "Sao Paulo",
           "Mexico City",
           "Cairo",
           "Dhaka",
           "Mumbai",
           "Beijing",
           "Osaka",
    };

    @DrawableRes
    public static int getBgdByRoomBgdId(@Nullable String bgdId) {
        int i = 1;
        try {
            if (bgdId != null)
                i = Integer.parseInt(bgdId.toLowerCase().substring(8, 10));
        } catch (Exception ignored) {
        }
        switch (i) {
            case 1:
                return R.drawable.one_portrait01;
            case 2:
                return R.drawable.one_portrait02;
            case 3:
                return R.drawable.one_portrait03;
            case 4:
                return R.drawable.one_portrait04;
            case 5:
                return R.drawable.one_portrait05;
            case 6:
                return R.drawable.one_portrait06;
            case 7:
                return R.drawable.one_portrait07;
            case 8:
                return R.drawable.one_portrait08;
            case 9:
                return R.drawable.one_portrait09;
            case 10:
                return R.drawable.one_portrait10;
            case 11:
                return R.drawable.one_portrait11;
            case 12:
                return R.drawable.one_portrait12;
            case 13:
                return R.drawable.one_portrait13;
            default:
                return R.drawable.one_portrait14;
        }
    }

    @NonNull
    public static Scene getSceneFromRoomInfo(@NonNull RoomInfo roomInfo) {
        Scene scene = new Scene();
        scene.setId(roomInfo.getId());
        scene.setUserId(roomInfo.getUserId());

        HashMap<String, String> map = new HashMap<>();
        map.put("backgroundId", roomInfo.getBackgroundId());
        map.put("roomName", roomInfo.getRoomName());
        map.put("roomId", roomInfo.getId());

        scene.setProperty(map);
        return scene;
    }

    @NonNull
    public static String getRandomRoomName() {
        return getRandomRoomName(new Random().nextInt(10));
    }

    @NonNull
    public static String getRandomRoomName(int number) {
        return nameList[number % 10];
    }


    public static void setBottomMarginForConstraintLayoutChild(@NonNull View view,int desiredBottom){
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        if (lp != null) {
            lp.bottomMargin = desiredBottom;
            view.setLayoutParams(lp);
        }
    }

    public static void setBottomDialogBackground(@NonNull View view) {
        MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable();

        ShapeAppearanceModel shapeAppearanceModel = ShapeAppearanceModel.builder(view.getContext(),
                R.style.one_cornerNormalTopStyle, R.style.one_cornerNormalTopStyle).build();
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

    public static <T extends ViewModel> T getViewModel(@NonNull Fragment fragment, @NonNull Class<T> viewModelClass) {
        return new ViewModelProvider(fragment.getViewModelStore(), new ViewModelProvider.NewInstanceFactory()).get(viewModelClass);
    }

    public static <T extends ViewModel> T getViewModel(@NonNull Fragment fragment, @NonNull Class<T> viewModelClass, @NonNull ViewModelProvider.NewInstanceFactory factory) {
        return new ViewModelProvider(fragment.getViewModelStore(), factory).get(viewModelClass);
    }

    public static <T extends ViewModel> T getAndroidViewModel(@NonNull ComponentActivity activity, @NonNull Class<T> viewModelClass) {
        return new ViewModelProvider(activity, new GlobalViewModelFactory(activity.getApplication())).get(viewModelClass);
    }

    public static <T extends ViewModel> T getAndroidViewModel(@NonNull Fragment fragment, @NonNull Class<T> viewModelClass) {
        return new ViewModelProvider(fragment.requireActivity(), new GlobalViewModelFactory(fragment.requireActivity().getApplication())).get(viewModelClass);
    }

    public static float lerp(float startValue, float endValue, float fraction) {
        return startValue + (fraction * (endValue - startValue));
    }

}
