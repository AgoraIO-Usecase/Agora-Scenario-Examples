package io.agora.sample.breakoutroom.repo;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Random;

import io.agora.sample.breakoutroom.bean.RoomInfo;

public interface RoomApi {

    void createSubRoom(@NonNull String name);
    void fetchAllSubRooms();
    void subscribeSubRooms();
}
