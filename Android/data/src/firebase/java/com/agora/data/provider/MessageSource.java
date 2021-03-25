package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Observable;

class MessageSource extends BaseMessageSource {

    private Gson mGson = new Gson();
    private Context mContext;

    private FirebaseFirestore db;

    /**
     * 申请举手用户列表
     */
    private final Map<String, Member> handUpMembers = new ConcurrentHashMap<>();

    public MessageSource(@NonNull Context context, @NonNull IRoomProxy iRoomProxy) {
        super(iRoomProxy);
        this.mContext = context;

        db = FirebaseFirestore.getInstance();
    }

    @Override
    public Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member) {
        return Observable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo("roomId.objectId", room.getObjectId())
                    .whereEqualTo("userId.objectId", member.getObjectId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                HashMap<String, Object> map = new HashMap<>();
                                map.put("roomId", room);
                                map.put("streamId", member.getStreamId());
                                map.put("userId", member.getUserId());
                                map.put("isSpeaker", member.getIsSpeaker());
                                map.put("isMuted", member.getIsMuted());
                                map.put("isSelfMuted", member.getIsSelfMuted());

                                db.collection(DataProvider.TAG_TABLE_MEMBER)
                                        .add(map)
                                        .addOnSuccessListener(documentReference -> {
                                            String objectId = documentReference.getId();
                                            member.setObjectId(objectId);
                                            emitter.onNext(member);
                                        })
                                        .addOnFailureListener(new OnFailureListener() {

                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                emitter.onError(e);
                                            }
                                        });
                                return;
                            }

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                emitter.onNext(document.toObject(Member.class));
                                break;
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        return Completable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo("roomId.objectId", room.getObjectId())
                    .whereEqualTo("userId.objectId", member.getObjectId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                db.collection(DataProvider.TAG_TABLE_MEMBER)
                                        .document(document.getId())
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                emitter.onComplete();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                emitter.onError(e);
                                            }
                                        });
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        return null;
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        return null;
    }

    @Override
    public Completable requestHandsUp(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable agreeHandsUp(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable refuseHandsUp(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable inviteSeat(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        return null;
    }

    @Override
    public Completable seatOff(@NonNull Member member) {
        return null;
    }

    @Override
    public Observable<List<Member>> getHandUpList() {
        return null;
    }

    @Override
    public int getHandUpListCount() {
        return 0;
    }
}
