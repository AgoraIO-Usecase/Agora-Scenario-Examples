package io.agora.uiwidget.function;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import io.agora.uiwidget.R;
import io.agora.uiwidget.basic.BindingSingleAdapter;
import io.agora.uiwidget.basic.BindingViewHolder;
import io.agora.uiwidget.databinding.RoomListItemBinding;

public class RoomListView extends FrameLayout {
    private static final int SPAN_COUNT = 2;
    private static final int REFRESH_DELAY = 1000 * 60;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View mNoDataBg;
    private View mNetworkErrorBg;

    private CustRoomListAdapter<?, ?> mListAdapter;
    private final Runnable mPageRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (mListAdapter != null) {
                mListAdapter.onRefresh();
            }
            postDelayed(mPageRefreshRunnable, REFRESH_DELAY);
        }
    };

    public RoomListView(@NonNull Context context) {
        this(context, null);
    }

    public RoomListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoomListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View.inflate(getContext(), R.layout.room_list_layout, this);
        mSwipeRefreshLayout = findViewById(R.id.room_list_host_in_swipe);
        mSwipeRefreshLayout.setNestedScrollingEnabled(false);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (mListAdapter != null) {
                mListAdapter.onRefresh();
            }
        });

        mRecyclerView = findViewById(R.id.room_list_recycler);
        mRecyclerView.setVisibility(View.VISIBLE);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), SPAN_COUNT));
        mRecyclerView.addItemDecoration(new RoomListItemDecoration());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    stopRefreshTimer();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (mSwipeRefreshLayout.isRefreshing()) {
                        // The swipe layout is refreshing when
                        // we want to refresh the whole page.
                        // In this case, we'll let the refreshing
                        // listener to handle all the work.
                        return;
                    }

                    startRefreshTimer();
                    if (mListAdapter != null) {
                        int lastItemPosition = recyclerView.getChildAdapterPosition(
                                recyclerView.getChildAt(recyclerView.getChildCount() - 1));
                        if (lastItemPosition == mListAdapter.getItemCount() - 1) {
                            mListAdapter.onLoadMore();
                        }
                    }

                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mNoDataBg = findViewById(R.id.room_list_no_data_bg);
        mNetworkErrorBg = findViewById(R.id.network_error_bg);
        mNetworkErrorBg.setVisibility(View.GONE);
    }

    public <Data> void setListAdapter(AbsRoomListAdapter<Data> listAdapter) {
        setListAdapter((CustRoomListAdapter<Data, RoomListItemBinding>)listAdapter);
    }

    public <Data, Binding extends ViewBinding> void setListAdapter(CustRoomListAdapter<Data, Binding> listAdapter) {
        if (mListAdapter != null) {
            mListAdapter.releaseInner();
        }
        mListAdapter = listAdapter;
        mRecyclerView.setAdapter(mListAdapter);
        if (mListAdapter != null) {
            mListAdapter.onNetErrorRun = () -> {
                mNoDataBg.setVisibility(View.GONE);
                mNetworkErrorBg.setVisibility(View.VISIBLE);
            };
            mListAdapter.onDataListUpdateRun = () -> {
                mNetworkErrorBg.setVisibility(View.GONE);
                checkRoomListEmpty();
                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            };
            mSwipeRefreshLayout.setRefreshing(true);
            mListAdapter.onRefresh();
        }
    }

    private void checkRoomListEmpty() {
        mRecyclerView.setVisibility(mListAdapter != null && mListAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        mNoDataBg.setVisibility(mListAdapter != null && mListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void startRefreshTimer() {
        postDelayed(mPageRefreshRunnable, REFRESH_DELAY);
    }

    private void stopRefreshTimer() {
        removeCallbacks(mPageRefreshRunnable);
    }

    private class RoomListItemDecoration extends RecyclerView.ItemDecoration {

        private final int mItemSpacing;

        private RoomListItemDecoration() {
            mItemSpacing = getResources().getDimensionPixelSize(R.dimen.room_list_item_margin);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);

            int position = parent.getChildAdapterPosition(view);
            int total = parent.getAdapter() == null ? 0 : parent.getAdapter().getItemCount();
            int half = mItemSpacing / 2;

            outRect.top = half;
            outRect.bottom = half;

            if (position < SPAN_COUNT) {
                outRect.top = mItemSpacing;
            } else {
                int remain = total % SPAN_COUNT;
                if (remain == 0) remain = SPAN_COUNT;
                if (position + remain >= total) {
                    outRect.bottom = mItemSpacing;
                }
            }

            if (position % SPAN_COUNT == 0) {
                outRect.left = mItemSpacing;
                outRect.right = mItemSpacing / 2;
            } else {
                outRect.left = mItemSpacing / 2;
                outRect.right = mItemSpacing;
            }
        }
    }

    public static final class RoomListItemViewHolder extends BindingViewHolder<RoomListItemBinding> {
        public final View bgView;
        public final View participantsLayout;
        public final TextView participantsCount;
        public final TextView roomName;

        public RoomListItemViewHolder(@NonNull RoomListItemBinding itemView) {
            super(itemView);
            bgView = itemView.roomListItemBackground;
            participantsLayout = itemView.roomListParticipantsLayout;
            participantsCount = itemView.roomListItemParticipantCount;
            roomName = itemView.roomListItemRoomName;
        }

    }

    public abstract static class AbsRoomListAdapter<Data> extends CustRoomListAdapter<Data, RoomListItemBinding> {

        @Override
        protected void onItemUpdate(BindingViewHolder<RoomListItemBinding> holder, Data item) {
            onItemUpdate(new RoomListItemViewHolder(holder.binding), item);
        }

        protected abstract void onItemUpdate(RoomListItemViewHolder holder, Data item);

    }

    public abstract static class CustRoomListAdapter<Data, Binding extends ViewBinding> extends BindingSingleAdapter<Data, Binding> {
        private Runnable onDataListUpdateRun;
        private Runnable onNetErrorRun;

        @Override
        public final void onBindViewHolder(@NonNull BindingViewHolder<Binding> holder, int position) {
            Data data = getItem(position);
            onItemUpdate(holder, data);
        }

        private void releaseInner() {
            onDataListUpdateRun = null;
            onNetErrorRun = null;
        }

        public final void triggerNetErrorRun() {
            if (onNetErrorRun != null) {
                onNetErrorRun.run();
            }
        }

        public final void triggerDataListUpdateRun() {
            if (onDataListUpdateRun != null) {
                onDataListUpdateRun.run();
            }
        }

        protected abstract void onItemUpdate(BindingViewHolder<Binding> holder, Data item);

        protected abstract void onRefresh();

        protected abstract void onLoadMore();
    }
}
