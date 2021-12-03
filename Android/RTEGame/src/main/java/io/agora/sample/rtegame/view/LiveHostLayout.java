package io.agora.sample.rtegame.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class LiveHostLayout extends ConstraintLayout {

    public int bottomMarginInGameType = 0;
    public int topMarginForGameView = 0;
    public int heightForGameView = 0;

    @Nullable
    public LiveHostCardView hostView;
    @Nullable
    public LiveHostCardView subHostView;
    @Nullable
    public LiveHostCardView gameHostView;
    @Nullable
    public WebView webViewHostView;

    private Type type = Type.HOST_ONLY;

    private boolean watchGame = false;

    public LiveHostLayout(@NonNull Context context) {
        this(context, null);
    }

    public LiveHostLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveHostLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            hostView = new LiveHostCardView(context);
            hostView.setCardBackgroundColor(Color.RED);
            subHostView = new LiveHostCardView(context);
            subHostView.setCardBackgroundColor(Color.BLUE);
            addView(hostView, new LayoutParams(0, 0));
            addView(subHostView, new LayoutParams(0, 0));
            setType(Type.DOUBLE);
        }
    }

    public void initParams(boolean watchGame, int topMarginForGameView, int heightForGameView, int bottomMarginInGameType){
        this.watchGame = watchGame;
        this.topMarginForGameView = topMarginForGameView;
        this.heightForGameView = heightForGameView;
        this.bottomMarginInGameType = bottomMarginInGameType;
    }

    @NonNull
    public LiveHostCardView createHostView(){
        if (hostView != null && hostView.getParent() == this)
            this.removeView(hostView);

        hostView = new LiveHostCardView(getContext());
        hostView.setId(View.generateViewId());
        this.addView(hostView , new LayoutParams(0, 0));
        return hostView;
    }

    @NonNull
    public LiveHostCardView createSubHostView(){
        if (subHostView != null && subHostView.getParent() == this)
            this.removeView(subHostView);

        subHostView = new LiveHostCardView(getContext());
        subHostView.setId(View.generateViewId());
        this.addView(subHostView, new LayoutParams(0 ,0));
        return subHostView;
    }

    public void createDefaultGameView(){
        if (!watchGame){
            if (webViewHostView != null && webViewHostView.getParent() == this)
                removeView(webViewHostView);
            webViewHostView = generateWebView();
            this.addView(webViewHostView, new LayoutParams(0, heightForGameView));
        }else{
            if (gameHostView != null && gameHostView.getParent() == this)
                removeView(gameHostView);
            gameHostView = new LiveHostCardView(getContext());
            this.addView(gameHostView, new LayoutParams(0, heightForGameView));
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView generateWebView() {
        WebView webView = new WebView(getContext());
        webView.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().getScheme().contains("http"))
                    view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
        return webView;
    }

    public void removeGameView(){
        if (watchGame){
            if (gameHostView != null && gameHostView.getParent() == this)
                removeView(gameHostView);
            gameHostView = null;
        }else{
            if (webViewHostView != null && webViewHostView.getParent() == this)
                removeView(webViewHostView);
            webViewHostView = null;
        }
    }

    private void onDoubleInGamePerformed() {
        String dimension = getMeasuredWidth() + ":" + getMeasuredHeight();
        if (subHostView != null && subHostView.getParent() == this) {
            subHostView.setVisibility(VISIBLE);
            LayoutParams lp = (LayoutParams) subHostView.getLayoutParams();
            clearRequiredViewParams(lp);

            lp.dimensionRatio = dimension;
            lp.matchConstraintPercentWidth = 0.25f;
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            lp.bottomMargin = this.bottomMarginInGameType;
            subHostView.setLayoutParams(lp);
        }
        if (hostView != null && hostView.getParent() == this) {
            LayoutParams lp = (LayoutParams) hostView.getLayoutParams();
            clearRequiredViewParams(lp);


            lp.dimensionRatio = dimension;
            lp.matchConstraintPercentWidth = 0.25f;

            if (subHostView == null) {
                lp.rightToRight = ConstraintSet.PARENT_ID;
                lp.bottomToBottom = ConstraintSet.PARENT_ID;
                lp.bottomMargin = this.bottomMarginInGameType;
            } else {
                lp.rightToLeft = subHostView.getId();
                lp.bottomToBottom = subHostView.getId();
            }
            hostView.setLayoutParams(lp);
        }

        if (watchGame){
            if (gameHostView != null && gameHostView.getParent() == this) {
                gameHostView.setVisibility(VISIBLE);
                setUpGameView(gameHostView);
            }
            if (webViewHostView != null)
                webViewHostView.setVisibility(GONE);
        }else{
            if (webViewHostView != null && webViewHostView.getParent() == this)
                webViewHostView.setVisibility(VISIBLE);
                setUpGameView(webViewHostView);
            if (gameHostView != null) {
                gameHostView.setVisibility(GONE);
            }
        }

    }

    private void onDoublePerformed() {

        if (webViewHostView != null) webViewHostView.setVisibility(GONE);
        if (gameHostView != null) gameHostView.setVisibility(GONE);

        if (subHostView != null && subHostView.getParent() == this) {
            subHostView.setVisibility(VISIBLE);
            LayoutParams lp = (LayoutParams) subHostView.getLayoutParams();
            clearRequiredViewParams(lp);

            lp.dimensionRatio = "1:1";
            lp.verticalBias = 0.3f;
            lp.matchConstraintPercentWidth = 0.5f;
            lp.topToTop = ConstraintSet.PARENT_ID;
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            subHostView.setLayoutParams(lp);
        }
        if (hostView != null && hostView.getParent() == this) {
            LayoutParams lp = (LayoutParams) hostView.getLayoutParams();
            clearRequiredViewParams(lp);

            lp.dimensionRatio = "1:1";
            lp.verticalBias = 0.3f;
            lp.matchConstraintPercentWidth = 0.5f;
            lp.leftToLeft = ConstraintSet.PARENT_ID;
            lp.topToTop = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            hostView.setLayoutParams(lp);
        }
    }

    private void onHostOnlyPerformed() {
        if (subHostView != null) subHostView.setVisibility(GONE);
        if (webViewHostView != null) webViewHostView.setVisibility(GONE);
        if (gameHostView != null) gameHostView.setVisibility(GONE);

        if (hostView != null && hostView.getParent() == this) {
            LayoutParams lp = (LayoutParams) hostView.getLayoutParams();
            clearRequiredViewParams(lp);
            lp.leftToLeft = ConstraintSet.PARENT_ID;
            lp.rightToRight = ConstraintSet.PARENT_ID;
            lp.topToTop = ConstraintSet.PARENT_ID;
            lp.bottomToBottom = ConstraintSet.PARENT_ID;
            hostView.setLayoutParams(lp);
        }
    }

    private void setUpGameView(@NonNull View view){
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        clearRequiredViewParams(lp);
        lp.leftToLeft = ConstraintSet.PARENT_ID;
        lp.rightToRight = ConstraintSet.PARENT_ID;
        lp.topToTop = ConstraintSet.PARENT_ID;
        lp.topMargin = topMarginForGameView;
        view.setLayoutParams(lp);
    }

    public void clearRequiredViewParams(@NonNull LayoutParams lp) {
        lp.horizontalBias = 0.5f;
        lp.verticalBias = 0.5f;
        lp.dimensionRatio = null;
        lp.matchConstraintPercentWidth = 1f;
        lp.leftMargin = 0;
        lp.topMargin = 0;
        lp.rightMargin = 0;
        lp.bottomMargin = 0;

        lp.leftToLeft = ConstraintSet.UNSET;
        lp.leftToRight = ConstraintSet.UNSET;
        lp.topToTop = ConstraintSet.UNSET;
        lp.topToBottom = ConstraintSet.UNSET;
        lp.rightToRight = ConstraintSet.UNSET;
        lp.rightToLeft = ConstraintSet.UNSET;
        lp.bottomToBottom = ConstraintSet.UNSET;
        lp.bottomToTop = ConstraintSet.UNSET;
    }

    public boolean isCurrentlyInGame(){
        return webViewHostView != null || gameHostView != null;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public void setType(@NonNull Type type) {
        this.type = type;
        switch (this.type) {
            case HOST_ONLY: {
                onHostOnlyPerformed();
                break;
            }
            case DOUBLE: {
                onDoublePerformed();
                break;
            }
            case DOUBLE_IN_GAME: {
                onDoubleInGamePerformed();
                break;
            }
        }
    }

    public enum Type {
        HOST_ONLY, DOUBLE, DOUBLE_IN_GAME,
    }

}
