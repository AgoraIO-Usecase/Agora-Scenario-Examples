package io.agora.scene.onelive.util;

import androidx.annotation.NonNull;

public class ViewStatus {
    private ViewStatus() {
    }

    public static class Error extends ViewStatus{
        @NonNull
        public String msg;

        public Error(@NonNull String msg) {
            this.msg = msg;
        }
        public Error(@NonNull Throwable t) {
            this.msg = t.getMessage() == null ? "" : t.getMessage();
        }
    }

    public static class Done extends ViewStatus{}
    public static class Loading extends ViewStatus{
        public boolean showLoading;

        public Loading(boolean showLoading) {
            this.showLoading = showLoading;
        }
    }
}