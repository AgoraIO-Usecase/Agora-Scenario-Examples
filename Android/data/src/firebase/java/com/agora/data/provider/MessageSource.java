package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.google.android.gms.tasks.OnFailureListener;
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
                    .whereEqualTo(DataProvider.MEMBER_ROOMID + "." + DataProvider.ROOM_OBJECTID, room.getObjectId())
                    .whereEqualTo(DataProvider.MEMBER_USERID + "." + DataProvider.USER_OBJECTID, member.getObjectId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                HashMap<String, Object> map = new HashMap<>();
                                map.put(DataProvider.MEMBER_ROOMID, room);
                                map.put(DataProvider.MEMBER_STREAMID, member.getStreamId());
                                map.put(DataProvider.MEMBER_USERID, member.getUserId());
                                map.put(DataProvider.MEMBER_ISSPEAKER, member.getIsSpeaker());
                                map.put(DataProvider.MEMBER_ISMUTED, member.getIsMuted());
                                map.put(DataProvider.MEMBER_ISSELFMUTED, member.getIsSelfMuted());

                                db.collection(DataProvider.TAG_TABLE_MEMBER)
                                        .add(map)
                                        .addOnSuccessListener(documentReference -> {
                                            String objectId = documentReference.getId();
                                            StringBuilder sb = new StringBuilder();
                                            for (char c : objectId.toCharArray()) {
                                                sb.append((int) c);
                                            }
                                            room.setObjectId(sb.toString());
                                            member.setRoomId(room);
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
                    .whereEqualTo(DataProvider.MEMBER_ROOMID + "." + DataProvider.ROOM_OBJECTID, room.getObjectId())
                    .whereEqualTo(DataProvider.MEMBER_USERID + "." + DataProvider.USER_OBJECTID, member.getObjectId())
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
                                        .addOnSuccessListener(aVoid -> emitter.onComplete())
                                        .addOnFailureListener(e -> emitter.onError(e));
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        return Completable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .document(member.getObjectId())
                    .update(DataProvider.MEMBER_ISMUTED, muted)
                    .addOnCompleteListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        return Completable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .document(member.getObjectId())
                    .update(DataProvider.MEMBER_ISSELFMUTED, muted)
                    .addOnCompleteListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable requestHandsUp(@NonNull Member member) {
        return Completable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put(DataProvider.ACTION_MEMBERID, member);
            map.put(DataProvider.ACTION_ROOMID, member.getRoomId());
            map.put(DataProvider.ACTION_ACTION, Action.ACTION.HandsUp.getValue());
            map.put(DataProvider.ACTION_STATUS, Action.ACTION_STATUS.Ing.getValue());

            db.collection(DataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable agreeHandsUp(@NonNull Member member) {
        return Completable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put(DataProvider.ACTION_MEMBERID, member);
            map.put(DataProvider.ACTION_ROOMID, member.getRoomId());
            map.put(DataProvider.ACTION_ACTION, Action.ACTION.HandsUp.getValue());
            map.put(DataProvider.ACTION_STATUS, Action.ACTION_STATUS.Agree.getValue());

            db.collection(DataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable refuseHandsUp(@NonNull Member member) {
        return Completable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put(DataProvider.ACTION_MEMBERID, member);
            map.put(DataProvider.ACTION_ROOMID, member.getRoomId());
            map.put(DataProvider.ACTION_ACTION, Action.ACTION.HandsUp.getValue());
            map.put(DataProvider.ACTION_STATUS, Action.ACTION_STATUS.Refuse.getValue());

            db.collection(DataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable inviteSeat(@NonNull Member member) {
        return Completable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put(DataProvider.ACTION_MEMBERID, member);
            map.put(DataProvider.ACTION_ROOMID, member.getRoomId());
            map.put(DataProvider.ACTION_ACTION, Action.ACTION.Invite.getValue());
            map.put(DataProvider.ACTION_STATUS, Action.ACTION_STATUS.Ing.getValue());

            db.collection(DataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        return Completable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put(DataProvider.ACTION_MEMBERID, member);
            map.put(DataProvider.ACTION_ROOMID, member.getRoomId());
            map.put(DataProvider.ACTION_ACTION, Action.ACTION.Invite.getValue());
            map.put(DataProvider.ACTION_STATUS, Action.ACTION_STATUS.Agree.getValue());

            db.collection(DataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        return Completable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put(DataProvider.ACTION_MEMBERID, member);
            map.put(DataProvider.ACTION_ROOMID, member.getRoomId());
            map.put(DataProvider.ACTION_ACTION, Action.ACTION.Invite.getValue());
            map.put(DataProvider.ACTION_STATUS, Action.ACTION_STATUS.Refuse.getValue());

            db.collection(DataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable seatOff(@NonNull Member member) {
        return Completable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .document(member.getObjectId())
                    .update(DataProvider.MEMBER_ISSPEAKER, 0)
                    .addOnCompleteListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(e -> emitter.onError(e));
        });
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
