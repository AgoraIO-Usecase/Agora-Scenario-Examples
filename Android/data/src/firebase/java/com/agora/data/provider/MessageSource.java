package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.BaseError;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.observer.DataMaybeObserver;
import com.agora.data.provider.model.ActionTemp;
import com.agora.data.provider.model.MemberTemp;
import com.agora.data.provider.model.RoomTemp;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;

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
        return Observable.create((ObservableOnSubscribe<Member>) emitter -> {
            DocumentReference drRoom = db.collection(DataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
            DocumentReference drUser = db.collection(DataProvider.TAG_TABLE_USER).document(member.getUserId().getObjectId());

            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo(DataProvider.MEMBER_ROOMID, drRoom)
                    .whereEqualTo(DataProvider.MEMBER_USERID, drUser)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                HashMap<String, Object> map = new HashMap<>();
                                map.put(DataProvider.MEMBER_ROOMID, drRoom);
                                map.put(DataProvider.MEMBER_USERID, drUser);
                                map.put(DataProvider.MEMBER_STREAMID, member.getStreamId());
                                map.put(DataProvider.MEMBER_ISSPEAKER, member.getIsSpeaker());
                                map.put(DataProvider.MEMBER_ISMUTED, member.getIsMuted());
                                map.put(DataProvider.MEMBER_ISSELFMUTED, member.getIsSelfMuted());

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
                                MemberTemp memberTemp = document.toObject(MemberTemp.class);
                                Member member2 = memberTemp.createMember(document.getId());

                                Task<Void> taskRoom = memberTemp.getRoomId()
                                        .get()
                                        .continueWith(new Continuation<DocumentSnapshot, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                                if (!task.isSuccessful()) {
                                                    throw task.getException();
                                                }

                                                DocumentSnapshot document = task.getResult();
                                                if (document == null || !document.exists()) {
                                                    return null;
                                                }

                                                RoomTemp roomTemp = document.toObject(RoomTemp.class);
                                                Room room = roomTemp.createRoom(document.getId());
                                                member.setRoomId(room);
                                                return null;
                                            }
                                        });
                                Task<Void> taskUser = memberTemp.getUserId()
                                        .get()
                                        .continueWith(new Continuation<DocumentSnapshot, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                                if (!task.isSuccessful()) {
                                                    throw task.getException();
                                                }

                                                DocumentSnapshot document = task.getResult();
                                                if (document == null || !document.exists()) {
                                                    return null;
                                                }

                                                User user = document.toObject(User.class);
                                                user.setObjectId(document.getId());
                                                member2.setUserId(user);
                                                return null;
                                            }
                                        });


                                List<Task<Void>> tasks = new ArrayList<>();
                                tasks.add(taskRoom);
                                tasks.add(taskUser);
                                Tasks.whenAll(tasks).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            emitter.onNext(member2);
                                        } else {
                                            emitter.onError(task.getException());
                                        }
                                    }
                                });
                                break;
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        }).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                registerMemberChanged();

                if (ObjectsCompat.equals(room.getAnchorId(), member.getUserId())) {
                    registerAnchorActionStatus();
                } else {
                    registerMemberActionStatus();
                }
            }
        });
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        unregisterAnchorActionStatus();
        unregisterMemberActionStatus();
        unregisterMemberChanged();

        if (ObjectsCompat.equals(room.getAnchorId(), member.getUserId())) {
            return Completable.create(emitter -> {
                DocumentReference drRoom = db.collection(DataProvider.TAG_TABLE_ROOM).document(room.getObjectId());

                db.collection(DataProvider.TAG_TABLE_ACTION)
                        .whereEqualTo(DataProvider.ACTION_ROOMID, drRoom)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference dr = db.collection(DataProvider.TAG_TABLE_ACTION)
                                            .document(document.getId());
                                    batch.delete(dr);
                                }

                                batch.commit();
                            } else {
                            }
                        });

                db.collection(DataProvider.TAG_TABLE_MEMBER)
                        .whereEqualTo(DataProvider.MEMBER_ROOMID, drRoom)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    emitter.onComplete();
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference dr = db.collection(DataProvider.TAG_TABLE_MEMBER)
                                            .document(document.getId());
                                    batch.delete(dr);
                                }

                                batch.commit();
                            }
                        });

                drRoom.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                emitter.onComplete();
                            } else {
                                emitter.onError(task.getException());
                            }
                        });

            });
        } else {
            return Completable.create(emitter -> {
                DocumentReference drRoom = db.collection(DataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
                DocumentReference drMember = db.collection(DataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

                db.collection(DataProvider.TAG_TABLE_ACTION)
                        .whereEqualTo(DataProvider.ACTION_ROOMID, drRoom)
                        .whereEqualTo(DataProvider.ACTION_MEMBERID, drMember)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference dr = db.collection(DataProvider.TAG_TABLE_ACTION)
                                            .document(document.getId());
                                    batch.delete(dr);
                                }

                                batch.commit();
                            } else {
                            }
                        });

                drMember.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                emitter.onComplete();
                            } else {
                                emitter.onError(task.getException());
                            }
                        });

            });
        }
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
        return Observable.just(new ArrayList<>(handUpMembers.values()));
    }

    @Override
    public int getHandUpListCount() {
        return handUpMembers.size();
    }

    ListenerRegistration lrAction;

    /**
     * 作为房主，需要监听房间中Action变化。
     */
    private void registerAnchorActionStatus() {
        Room room = iRoomProxy.getRoom();
        if (room == null) {
            return;
        }

        final DocumentReference drRoom = db.collection(DataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
        lrAction = db.collection(DataProvider.TAG_TABLE_ACTION)
                .whereEqualTo(DataProvider.ACTION_ROOMID, drRoom)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            ActionTemp actionTemp = dc.getDocument().toObject(ActionTemp.class);
                            Action item = actionTemp.create(dc.getDocument().getId());
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                if (item.getAction() == Action.ACTION.HandsUp.getValue()) {
                                    if (item.getStatus() == Action.ACTION_STATUS.Ing.getValue()) {
                                        Member member = item.getMemberId();
                                        member = iRoomProxy.getMemberById(member.getObjectId());
                                        if (member == null) {
                                            return;
                                        }

                                        if (handUpMembers.containsKey(member.getObjectId())) {
                                            return;
                                        }

                                        handUpMembers.put(member.getObjectId(), member);
                                        iRoomProxy.onReceivedHandUp(member);
                                    }
                                } else if (item.getAction() == Action.ACTION.Invite.getValue()) {
                                    if (item.getStatus() == Action.ACTION_STATUS.Agree.getValue()) {
                                        Member member = item.getMemberId();
                                        member = iRoomProxy.getMemberById(member.getObjectId());
                                        if (member == null) {
                                            return;
                                        }

                                        iRoomProxy.onInviteAgree(member);
                                    } else if (item.getStatus() == Action.ACTION_STATUS.Refuse.getValue()) {
                                        Member member = item.getMemberId();
                                        member = iRoomProxy.getMemberById(member.getObjectId());
                                        if (member == null) {
                                            return;
                                        }

                                        iRoomProxy.onInviteRefuse(member);
                                    }
                                }
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            }
                        }
                    }
                });
    }

    private void unregisterAnchorActionStatus() {
        if (lrAction != null) {
            lrAction.remove();
        }
    }

    ListenerRegistration lrAction2;

    /**
     * 作为观众，需要监听自己的Action变化。
     */
    private void registerMemberActionStatus() {
        Member member = iRoomProxy.getMine();
        if (member == null) {
            return;
        }

        final DocumentReference drMember = db.collection(DataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());
        lrAction2 = db.collection(DataProvider.TAG_TABLE_ACTION)
                .whereEqualTo(DataProvider.ACTION_MEMBERID, drMember)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            ActionTemp actionTemp = dc.getDocument().toObject(ActionTemp.class);
                            Action item = actionTemp.create(dc.getDocument().getId());
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                if (item.getAction() == Action.ACTION.HandsUp.getValue()) {
                                    if (item.getStatus() == Action.ACTION_STATUS.Agree.getValue()) {
                                        iRoomProxy.onHandUpAgree(member);
                                    } else if (item.getStatus() == Action.ACTION_STATUS.Refuse.getValue()) {
                                        iRoomProxy.onHandUpRefuse(member);
                                    }
                                } else if (item.getAction() == Action.ACTION.Invite.getValue()) {
                                    if (item.getStatus() == Action.ACTION_STATUS.Ing.getValue()) {
                                        iRoomProxy.onReceivedInvite(member);
                                    }
                                }
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            }
                        }
                    }
                });
    }

    private void unregisterMemberActionStatus() {
        if (lrAction2 != null) {
            lrAction2.remove();
        }
    }

    ListenerRegistration lrMember;

    /**
     * 监听房间内部成员信息变化
     */
    private void registerMemberChanged() {
        Room room = iRoomProxy.getRoom();
        if (room == null) {
            return;
        }

        final DocumentReference drRoom = db.collection(DataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
        lrMember = db.collection(DataProvider.TAG_TABLE_MEMBER)
                .whereEqualTo(DataProvider.MEMBER_ROOMID, drRoom)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            MemberTemp temp = dc.getDocument().toObject(MemberTemp.class);
                            Member member = temp.createMember(dc.getDocument().getId());
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                if (iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                                    return;
                                }

                                iRoomProxy.getMember(room.getObjectId(), member.getUserId().getObjectId())
                                        .subscribe(new DataMaybeObserver<Member>(mContext) {
                                            @Override
                                            public void handleError(@NonNull BaseError e) {

                                            }

                                            @Override
                                            public void handleSuccess(@Nullable Member member) {
                                                if (member == null) {
                                                    return;
                                                }

                                                if (iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                                                    return;
                                                }

                                                iRoomProxy.onMemberJoin(member);
                                            }
                                        });
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                if (!iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                                    return;
                                }

                                Member memberOld = iRoomProxy.getMemberById(member.getObjectId());
                                if (memberOld == null) {
                                    return;
                                }

                                if (memberOld.getIsSpeaker() != member.getIsSpeaker()) {
                                    iRoomProxy.onRoleChanged(false, member);
                                }

                                if (memberOld.getIsSelfMuted() != member.getIsSelfMuted()) {
                                    iRoomProxy.onAudioStatusChanged(false, member);
                                }

                                if (memberOld.getIsMuted() != member.getIsMuted()) {
                                    iRoomProxy.onAudioStatusChanged(false, member);
                                }
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                if (!iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                                    return;
                                }

                                Member member2 = iRoomProxy.getMemberById(member.getObjectId());
                                if (member2 == null) {
                                    return;
                                }

                                iRoomProxy.onMemberLeave(member2);
                            }
                        }
                    }
                });
    }

    private void unregisterMemberChanged() {
        if (lrMember != null) {
            lrMember.remove();
        }
    }
}
