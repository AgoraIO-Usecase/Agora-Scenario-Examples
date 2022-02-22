package io.agora.scene.rtegame;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;

import io.agora.example.base.BaseUtil;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.scene.rtegame.bean.LocalUser;
import io.agora.scene.rtegame.bean.RoomInfo;
import io.agora.scene.rtegame.util.GameConstants;
import io.agora.syncmanager.rtm.Sync;
import io.agora.syncmanager.rtm.SyncManagerException;

@Keep
public class GlobalViewModel extends AndroidViewModel {

    @Nullable
    public static RoomInfo currentRoom = null;

    public static final int RTM_SDK = 0;
    public static final int RTC_SDK = 1;

    @Nullable
    public static LocalUser localUser;
    @Nullable
    public static RtcEngineEx rtcEngine;

    private int initResult;
    private final MutableLiveData<Integer> _isSDKsReady = new MutableLiveData<>();

    @NonNull
    public LiveData<Integer> isSDKsReady() {
        return _isSDKsReady;
    }

    public GlobalViewModel(@NonNull Application application) {
        super(application);
        BaseUtil.logD("GlobalViewModel init " + this);
        GlobalViewModel.localUser = checkLocalOrGenerate(application.getApplicationContext());
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
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

            try {
                RtcEngineEx engine = (RtcEngineEx) RtcEngineEx.create(config);
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

    /**
     * 生成用户信息
     * 本地存在==> 本地生成
     * 本地不存在==> 随机生成
     */
    @NonNull
    public static LocalUser checkLocalOrGenerate(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences("sp_rte_game", Context.MODE_PRIVATE);
        String userId = sp.getString("id", "-1");

        boolean isValidUser = true;

        try {
            int i = Integer.parseInt(userId);
            if (i == -1) isValidUser = false;
        } catch (NumberFormatException e) {
            isValidUser = false;
        }

        LocalUser localUser;
        if (isValidUser)
            localUser = new LocalUser(userId);
        else
            localUser = new LocalUser();
        sp.edit().putString("id", localUser.getUserId()).apply();

        return localUser;
    }

    private void initSyncManager(@NonNull Context context) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", context.getString(R.string.rtm_app_id));
        map.put("token", context.getString(R.string.rtm_app_token));
        map.put("defaultChannel", GameConstants.globalChannel);
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
    }}
