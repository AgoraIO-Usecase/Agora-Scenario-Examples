package io.agora.scene.rtegame.bean;

import androidx.annotation.NonNull;

public class Gift {
    private final int id;
    private final int value;
    private final String name;

    public int iconRes;
    public int gifRes;

    public Gift(int id, int value, @NonNull String name) {
        this.id = id;
        this.value = value;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    public String getName() {
        return name;
    }
}
