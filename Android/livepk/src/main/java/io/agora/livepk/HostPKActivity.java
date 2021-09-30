package io.agora.livepk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannelAttribute;

public class HostPKActivity extends AppCompatActivity {
    private static final String TAG = "HostPKActivity";

    private static final String EXTRA_ROOM_NAME = "roomName";

    public static Intent launch(Context context, String roomName) {
        Intent intent = new Intent(context, HostPKActivity.class);
        intent.putExtra(EXTRA_ROOM_NAME, roomName);
        return intent;
    }

    private final RtmManager rtmManager = new RtmManager();
    private final RtcManager rtcManager = new RtcManager();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String roomName = getLocalChannelId();
        setTitle(roomName);

        initView();
        initManager();
        setupLocalVideo();
        joinLocalChannel();
    }

    private void initView() {
        Button button = findViewById(R.id.btn);
        button.setVisibility(View.VISIBLE);
        button.setText("PK");
        button.setOnClickListener(v -> {
            checkIsPKing(getLocalChannelId(), this::showPkEditDialog, false);
        });

        Button button2 = findViewById(R.id.btn2);
        button2.setVisibility(View.VISIBLE);
        button2.setText("Exit PK");
        button2.setOnClickListener(v -> {
            stopPK();
        });
    }

    private void initManager() {
        rtcManager.init(this, getAgoraAppId(), new RtcManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {
                if(!TextUtils.isEmpty(message)){
                    runOnUiThread(() -> {
                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }

            @Override
            public void onSuccess() {

            }
        });
        rtmManager.init(this, getAgoraAppId(), new RtmManager.OnInitializeListener() {
            @Override
            public void onError(int code, String message) {
                if(!TextUtils.isEmpty(message)){
                    runOnUiThread(() -> {
                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onSuccess() {

            }
        });
    }

    private void setupPkVideo(String channelId, int uid) {
        rtcManager.renderRemoteVideo(findViewById(R.id.fl_remote_container), channelId, uid);
    }

    private void joinLocalRtm(String uid){
        rtmManager.login(uid, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                String channelId = getLocalChannelId();
                rtmManager.joinChannel(channelId, new RtmManager.OnChannelListener() {
                    @Override
                    public void onError(int code, String message) {
                        if(!TextUtils.isEmpty(message)){
                            runOnUiThread(() -> {
                                Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onJoinSuccess() {
                        checkIsPKing(getLocalChannelId(), null, true);
                    }

                    @Override
                    public void onChannelAttributesUpdated(List<RtmChannelAttribute> list) {
                        Log.d(TAG, "Local RTM ChannelAttributesUpdated : " + list);

                        String pkName = getPkNameFromChannelAttr(list);
                        if (!TextUtils.isEmpty(pkName)) {
                            joinPKChannel(pkName);
                        } else {
                            leavePKChannel();
                        }
                    }
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                if(errorInfo != null && !TextUtils.isEmpty(errorInfo.getErrorDescription())){
                    runOnUiThread(() -> {
                        Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void joinPKRtm(String channelId){
        RtmChannelAttribute pkInfo = new RtmChannelAttribute();
        pkInfo.setKey("PK");
        pkInfo.setValue(channelId);
        rtmManager.updateChannelAttributes(getLocalChannelId(), Collections.singletonList(pkInfo), null);

        RtmChannelAttribute pkInfo2 = new RtmChannelAttribute();
        pkInfo2.setKey("PK");
        pkInfo2.setValue(getLocalChannelId());
        rtmManager.updateChannelAttributes(channelId, Collections.singletonList(pkInfo2), null);
    }

    private void setupLocalVideo() {
        rtcManager.renderLocalVideo(findViewById(R.id.fl_local_container));
    }

    private void joinLocalChannel() {
        String channelId = getLocalChannelId();
        rtcManager.joinChannel(channelId, true, new RtcManager.OnChannelListener() {
            @Override
            public void onError(int code, String message) {
                if(!TextUtils.isEmpty(message)){
                    runOnUiThread(() -> {
                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }

            @Override
            public void onJoinSuccess(int uid) {
                joinLocalRtm(uid + "");
            }

            @Override
            public void onUserJoined(String channelId, int uid) {

            }
        });
    }

    private void showPkEditDialog() {
        EditText editText = new EditText(this);
        editText.setHint("PK Room Name");
        new AlertDialog.Builder(this)
                .setTitle("PK")
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> {
                    String pkRoomName = editText.getText().toString();
                    if (TextUtils.isEmpty(pkRoomName)) {
                        Toast.makeText(HostPKActivity.this, "The pk room name is empty", Toast.LENGTH_LONG).show();
                        return;
                    }
                    startPK(pkRoomName);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkIsPKing(String channelId, Runnable idle, boolean shouldStartPk){
        rtmManager.getChannelAttributes(channelId, new ResultCallback<List<RtmChannelAttribute>>() {
            @Override
            public void onSuccess(List<RtmChannelAttribute> attributes) {
                Log.d(TAG, "checkIsPKing attributes : " + attributes);
                String pkName = getPkNameFromChannelAttr(attributes);
                if (!TextUtils.isEmpty(pkName)) {
                    runOnUiThread(() -> Toast.makeText(HostPKActivity.this, "The host "+ channelId + " is kping with " + pkName, Toast.LENGTH_LONG).show());
                    if(shouldStartPk){
                        joinPKChannel(pkName);
                    }
                }else if(idle != null){
                    runOnUiThread(idle);
                }
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                if (errorInfo != null && TextUtils.isEmpty(errorInfo.getErrorDescription())) {
                    runOnUiThread(()->{
                        Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private String getPkNameFromChannelAttr(List<RtmChannelAttribute> attributes){
        if (attributes.size() > 0) {
            RtmChannelAttribute pkAttribute = attributes.get(0);
            if(pkAttribute.getKey().equals("PK") && !TextUtils.isEmpty(pkAttribute.getValue())){
                return pkAttribute.getValue();

            }
        }
        return "";
    }

    private void startPK(String channelId){
        checkIsPKing(channelId, () -> joinPKRtm(channelId), false);
    }

    private void stopPK() {
        String localChannelId = getLocalChannelId();
        rtmManager.getChannelAttributes(localChannelId, new ResultCallback<List<RtmChannelAttribute>>() {
            @Override
            public void onSuccess(List<RtmChannelAttribute> attributes) {
                String pkName = getPkNameFromChannelAttr(attributes);
                if(!TextUtils.isEmpty(pkName)){
                    rtmManager.deleteChannelAttribute(getLocalChannelId(), Collections.singletonList("PK"), null);

                    rtmManager.getChannelAttributes(pkName, new ResultCallback<List<RtmChannelAttribute>>() {
                        @Override
                        public void onSuccess(List<RtmChannelAttribute> attributes) {
                            String _pkName = getPkNameFromChannelAttr(attributes);
                            if(localChannelId.equals(_pkName)){
                                rtmManager.deleteChannelAttribute(pkName, Collections.singletonList("PK"), null);
                            }
                        }

                        @Override
                        public void onFailure(ErrorInfo errorInfo) {
                            if (errorInfo != null && TextUtils.isEmpty(errorInfo.getErrorDescription())) {
                                runOnUiThread(()->{
                                    Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                if (errorInfo != null && TextUtils.isEmpty(errorInfo.getErrorDescription())) {
                    runOnUiThread(()->{
                        Toast.makeText(HostPKActivity.this, errorInfo.getErrorDescription(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void joinPKChannel(String channelId) {
        rtcManager.joinChannel(channelId, false, new RtcManager.OnChannelListener() {
            @Override
            public void onError(int code, String message) {
                if(!TextUtils.isEmpty(message)){
                    runOnUiThread(() -> {
                        Toast.makeText(HostPKActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onJoinSuccess(int uid) {

            }

            @Override
            public void onUserJoined(String channelId, int uid) {
                runOnUiThread(() -> setupPkVideo(channelId, uid));
            }
        });
    }

    private void leavePKChannel(){
        rtcManager.leaveChannelExcept(getLocalChannelId());
        runOnUiThread(() -> ((ViewGroup)findViewById(R.id.fl_remote_container)).removeAllViews());
    }

    private String getLocalChannelId(){
        return getIntent().getStringExtra(EXTRA_ROOM_NAME);
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
        rtcManager.release();
        rtmManager.release();
    }
}
