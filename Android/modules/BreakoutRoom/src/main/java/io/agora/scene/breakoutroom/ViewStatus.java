package io.agora.scene.breakoutroom;

public class ViewStatus {
    private ViewStatus() {
    }

    public static class Error extends ViewStatus{
        public String msg;

        public Error(String msg) {
            this.msg = msg;
        }
        public Error(Throwable t) {
            this.msg = t.getMessage();
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