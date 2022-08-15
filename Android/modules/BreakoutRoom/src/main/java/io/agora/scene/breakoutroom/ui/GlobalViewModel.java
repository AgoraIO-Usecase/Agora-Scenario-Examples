package io.agora.scene.breakoutroom.ui;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;

import io.agora.example.base.BaseUtil;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.RtcEngineConfig;
import io.agora.scene.breakoutroom.R;
import io.agora.scene.breakoutroom.RoomConstant;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

/**
 * @author lq
 */
public class GlobalViewModel extends AndroidViewModel {

    public static final int RTM_SDK = 0;
    public static final int RTC_SDK = 1;

    @Nullable
    public static RtcEngine rtcEngine;

    private int initResult;
    private final MutableLiveData<Integer> _isSDKsReady = new MutableLiveData<>();

    @NonNull
    public LiveData<Integer> isSDKsReady() {
        return _isSDKsReady;
    }

    public GlobalViewModel(@NonNull Application application) {
        super(application);
        BaseUtil.logD("GlobalViewModel init " + this);
        initSyncManager(application.getApplicationContext());
        initRtcSDK(application.getApplicationContext());
    }


    @MainThread
    private void setInitResult(@IntRange(from = RTM_SDK, to = RTC_SDK) int type, boolean success) {
        // 设置标志位
        int res = 0b11 << (2 * type);
        // 设置数值位
        if (!success)
            res = res & (0b10 << (2 * type));
        // 赋值
        initResult = initResult | res;
        _isSDKsReady.setValue(initResult);
    }

    private void initRtcSDK(Context mContext) {
        String appID = mContext.getString(R.string.rtc_app_id);
        if (appID.isEmpty() || appID.codePointCount(0, appID.length()) != 32) {
            setInitResult(RTC_SDK, false);
        } else {

            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = mContext;
            config.mAppId = appID;
            config.mEventHandler = new IRtcEngineEventHandler() {
            };
            RtcEngineConfig.LogConfig logConfig = new RtcEngineConfig.LogConfig();
            logConfig.filePath = mContext.getExternalCacheDir().getAbsolutePath();
            config.mLogConfig = logConfig;

            try {
                RtcEngine engine = RtcEngine.create(config);
                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
                engine.enableAudio();
                engine.enableVideo();
                rtcEngine = engine;
                setInitResult(RTC_SDK, true);
            } catch (Exception e) {
                e.printStackTrace();
                setInitResult(RTC_SDK, false);
            }
        }

    }

    private void initSyncManager(@NonNull Context context) {
        String appID = context.getString(R.string.sync_app_id);
        if (appID.isEmpty() || appID.codePointCount(0, appID.length()) != 32) {
            setInitResult(RTM_SDK, false);
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", appID);
        map.put("defaultChannel", RoomConstant.globalChannel);
        Sync.Instance().init(context, map, new Sync.Callback() {
            @Override
            public void onSuccess() {
                new Handler(Looper.getMainLooper()).post(() -> setInitResult(RTM_SDK, true));
            }

            @Override
            public void onFail(SyncManagerException exception) {
                new Handler(Looper.getMainLooper()).post(() -> setInitResult(RTM_SDK, false));
            }
        });
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            Sync.Instance().destroy();
            RtcEngine.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
