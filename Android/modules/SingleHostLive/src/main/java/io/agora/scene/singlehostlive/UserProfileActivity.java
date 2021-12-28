package io.agora.scene.singlehostlive;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.uiwidget.basic.TitleBar;

public class UserProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_user_profile_activity);

        TitleBar titleBar = findViewById(R.id.title_bar);
        titleBar.setTitleName("UserProfile", Color.BLACK);
        titleBar.setBackIcon(true, io.agora.uiwidget.R.drawable.title_bar_back_black, v -> {
            finish();
        });
    }

}
