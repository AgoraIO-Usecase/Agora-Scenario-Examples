package io.agora.scene.breakoutroom;

import com.google.gson.Gson;

import java.util.Random;

public class RoomConstant {
    public static final String userId = String.valueOf(new Random().nextInt(10000));
    public static final String globalChannel = "BreakOutRoom";
    public static final String globalSubRoom = "SubRoom";

    public static final Gson gson = new Gson();
}
