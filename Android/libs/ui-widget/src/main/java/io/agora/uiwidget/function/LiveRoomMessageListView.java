package io.agora.uiwidget.function;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import io.agora.uiwidget.R;

public class LiveRoomMessageListView extends RecyclerView {

    private static final int MESSAGE_TEXT_COLOR = Color.rgb(196, 196, 196);
    private static final int MESSAGE_TEXT_COLOR_LIGHT = Color.argb(101, 35, 35, 35);
    private static final int MAX_SAVED_MESSAGE = 50;
    private static final int MESSAGE_ITEM_MARGIN = 16;

    private LiveRoomMessageAdapter<?> mAdapter;
    private LinearLayoutManager mLayoutManager;

    private boolean mLightMode;
    private boolean mNarrow = false;

    public LiveRoomMessageListView(@NonNull Context context) {
        this(context, null);
    }

    public LiveRoomMessageListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveRoomMessageListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        boolean isLight = false;
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LiveRoomMessageListView);
            isLight = typedArray.getBoolean(R.styleable.LiveRoomMessageListView_isLight, false);
            typedArray.recycle();
        }
        init(isLight);
    }

    private void init(boolean lightMode) {
        mLightMode = lightMode;
        mLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        mLayoutManager.setStackFromEnd(true);
        setLayoutManager(mLayoutManager);
        addItemDecoration(new MessageItemDecorator());
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        if (adapter instanceof LiveRoomMessageAdapter) {
            mAdapter = (LiveRoomMessageAdapter<?>) adapter;
            mAdapter.isLight = mLightMode;
            mAdapter.isNarrow = mNarrow;
            mAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    scrollToBottom();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);
                    scrollToBottom();
                }
            });
            super.setAdapter(adapter);
        } else {
            throw new RuntimeException("the adapter must be instance of LiveRoomMessageAdapter");
        }
    }

    private void scrollToBottom() {
        if(mAdapter == null){
            return;
        }
        mLayoutManager.scrollToPositionWithOffset(Math.max(0, mAdapter.getItemCount() - 1), 0);
    }

    public void setNarrow(boolean narrow) {
        mNarrow = narrow;
        if (mAdapter != null) {
            mAdapter.isNarrow = mNarrow;
            mAdapter.notifyDataSetChanged();
        }
    }

    public abstract static class LiveRoomMessageAdapter<T> extends Adapter<MessageListViewHolder> {
        private final ArrayList<T> mMessageList = new ArrayList<>();
        private boolean isLight = false;
        private boolean isNarrow = false;

        @NonNull
        @Override
        public final MessageListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MessageListViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.live_room_message_list_item_layout, parent, false));
        }

        @Override
        public final void onBindViewHolder(@NonNull MessageListViewHolder holder, int position) {
            holder.isLight = isLight;
            holder.isNarrow = isNarrow;

            T item = mMessageList.get(position);
            onItemUpdate(holder, item, position);
        }

        @Override
        public final int getItemCount() {
            return mMessageList.size();
        }

        protected abstract void onItemUpdate(MessageListViewHolder holder, T item, int position);

        public void addMessage(T item) {
            int size = mMessageList.size();
            if (size == MAX_SAVED_MESSAGE) {
                mMessageList.remove(0);
                mMessageList.add(item);
                notifyDataSetChanged();
            }else{
                mMessageList.add(item);
                notifyItemInserted(size);
            }
        }
    }

    public static class MessageListViewHolder extends ViewHolder {
        private boolean isLight = false;
        private boolean isNarrow = false;

        private final AppCompatTextView messageText;
        private final AppCompatImageView giftIconIv;
        private final View layout;

        MessageListViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.live_message_item_text);
            giftIconIv = itemView.findViewById(R.id.live_message_gift_icon);
            layout = itemView.findViewById(R.id.live_message_item_layout);
        }

        public void setupMessage(String user, String message, @DrawableRes int giftIcon) {
            int background = isLight
                    ? R.drawable.live_room_message_item_bg_light
                    : R.drawable.live_room_message_item_bg;
            int nameColor = isLight
                    ? Color.BLACK
                    : Color.WHITE;
            int messageColor = isLight
                    ? MESSAGE_TEXT_COLOR_LIGHT
                    : MESSAGE_TEXT_COLOR;

            layout.setBackgroundResource(background);

            String text = isNarrow ? user + ": " : user + ":  " + message;
            SpannableString messageSpan = new SpannableString(text);
            messageSpan.setSpan(new StyleSpan(Typeface.BOLD),
                    0, user.length() + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            messageSpan.setSpan(new ForegroundColorSpan(nameColor),
                    0, user.length() + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            if (!isNarrow || giftIcon == View.NO_ID) {
                messageSpan.setSpan(new ForegroundColorSpan(messageColor),
                        user.length() + 2, messageSpan.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            if(giftIcon != View.NO_ID){
                giftIconIv.setImageResource(giftIcon);
                giftIconIv.setVisibility(View.VISIBLE);
            }else{
                giftIconIv.setVisibility(View.GONE);
            }

            messageText.setText(messageSpan);
        }
    }

    private static class MessageItemDecorator extends ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.top = MESSAGE_ITEM_MARGIN;
            outRect.bottom = MESSAGE_ITEM_MARGIN;
        }
    }
}
