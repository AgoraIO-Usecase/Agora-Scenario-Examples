package io.agora.livepk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.agora.mediaplayer.AgoraMediaPlayerKit;
import io.agora.mediaplayer.Constants;
import io.agora.mediaplayer.MediaPlayerObserver;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannelAttribute;

import static io.agora.mediaplayer.Constants.PLAYER_RENDER_MODE_HIDDEN;

public class AudienceActivity extends AppCompatActivity {
    private static final String TAG = "AudienceActivity";

    private static final String EXTRA_ROOM_NAME = "roomName";

    private AgoraMediaPlayerKit agoraMediaPlayerKitA, agoraMediaPlayerKitB;
    private final RtmManager rtmManager = new RtmManager();

    public static Intent launch(Context context, String roomName) {
        Intent intent = new Intent(context, AudienceActivity.class);
        intent.putExtra(EXTRA_ROOM_NAME, roomName);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        String roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);
        setTitle(roomName);

        initRtmManager(roomName);
        initMediaPlayer(roomName);
        initView();
    }

    private void initRtmManager(String roomName) {
        rtmManager.init(this, getAgoraAppId(), new RtmManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {

            }

            @Override
            public void onSuccess() {
                rtmManager.joinChannel(roomName, new RtmManager.OnChannelListener() {
                    @Override
                    public void onError(int code, String message) {

                    }

                    @Override
                    public void onJoinSuccess() {
                        rtmManager.getChannelAttributes(roomName, new ResultCallback<List<RtmChannelAttribute>>() {
                            @Override
                            public void onSuccess(List<RtmChannelAttribute> attributes) {
                                String pkName = getPkNameFromChannelAttr(attributes);
                                if (!TextUtils.isEmpty(pkName)) {
                                    runOnUiThread(() -> agoraMediaPlayerKitB = playUrl(findViewById(R.id.fl_remote_container), getVideoPullUrl(pkName)));
                                }
                            }

                            @Override
                            public void onFailure(ErrorInfo errorInfo) {

                            }
                        });
                    }

                    @Override
                    public void onChannelAttributesUpdated(List<RtmChannelAttribute> list) {
                        String pkName = getPkNameFromChannelAttr(list);
                        if (!TextUtils.isEmpty(pkName)) {
                            runOnUiThread(() -> agoraMediaPlayerKitB = playUrl(findViewById(R.id.fl_remote_container), getVideoPullUrl(pkName)));
                        } else {
                            runOnUiThread(()->{
                                if(agoraMediaPlayerKitB != null){
                                    agoraMediaPlayerKitB.destroy();
                                    agoraMediaPlayerKitB = null;
                                    ((ViewGroup)findViewById(R.id.fl_remote_container)).removeAllViews();
                                }
                            });
                        }
                    }
                });
            }
        });
        rtmManager.login(UUID.randomUUID().toString(), null);

    }

    private void initMediaPlayer(String roomName) {
        agoraMediaPlayerKitA = playUrl(findViewById(R.id.fl_local_container), getVideoPullUrl(roomName));
    }

    private void initView() {
        Button button = findViewById(R.id.btn);
        button.setText("Connect");
        button.setOnClickListener(v -> {
            showConnectDialog();
        });
    }

    private void showConnectDialog() {
        EditText editText = new EditText(this);
        editText.setHint("PK Room Name");
        new AlertDialog.Builder(this)
                .setTitle("Connect")
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> {
                    String pkRoomName = editText.getText().toString();
                    if (TextUtils.isEmpty(pkRoomName)) {
                        Toast.makeText(AudienceActivity.this, "The pk room name is empty", Toast.LENGTH_LONG).show();
                        return;
                    }
                    agoraMediaPlayerKitB = playUrl(findViewById(R.id.fl_remote_container), getVideoPullUrl(pkRoomName));
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private AgoraMediaPlayerKit playUrl(FrameLayout container, String url) {
        AgoraMediaPlayerKit mediaPlayer = new AgoraMediaPlayerKit(this);
        SurfaceView videoView = new SurfaceView(this);
        container.addView(videoView);
        mediaPlayer.setView(videoView);
        mediaPlayer.setRenderMode(PLAYER_RENDER_MODE_HIDDEN);
        mediaPlayer.open(url, 0);
        mediaPlayer.registerPlayerObserver(new MediaPlayerObserver() {
            @Override
            public void onPlayerStateChanged(Constants.MediaPlayerState state, Constants.MediaPlayerError error) {
                Log.i(TAG, "agoraMediaPlayerKit1 onPlayerStateChanged:" + state + " " + error);
                if (state == Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
                    mediaPlayer.play();
                }
            }

            @Override
            public void onPositionChanged(long l) {

            }

            @Override
            public void onPlayerEvent(Constants.MediaPlayerEvent mediaPlayerEvent) {

            }

            @Override
            public void onMetaData(Constants.MediaPlayerMetadataType mediaPlayerMetadataType, byte[] bytes) {

            }

            @Override
            public void onPlayBufferUpdated(long l) {

            }

            @Override
            public void onPreloadEvent(String s, Constants.MediaPlayerPreloadEvent mediaPlayerPreloadEvent) {

            }
        });

        return mediaPlayer;
    }

    private String getPkNameFromChannelAttr(List<RtmChannelAttribute> attributes) {
        if (attributes.size() > 0) {
            RtmChannelAttribute pkAttribute = attributes.get(0);
            if (pkAttribute.getKey().equals("PK") && !TextUtils.isEmpty(pkAttribute.getValue())) {
                return pkAttribute.getValue();

            }
        }
        return "";
    }

    private String getAgoraAppId() {
        String appId = getString(R.string.agora_app_id);
        if (TextUtils.isEmpty(appId)) {
            throw new RuntimeException("the app id is empty");
        }
        return appId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (agoraMediaPlayerKitA != null) {
            agoraMediaPlayerKitA.destroy();
        }
        if (agoraMediaPlayerKitB != null) {
            agoraMediaPlayerKitB.destroy();
        }
        rtmManager.release();
    }

    private String getVideoPullUrl(String roomName) {
        return String.format(Locale.US, "rtmp://mdetest.pull.agoramde.agoraio.cn/live/%s", roomName);
    }
}
