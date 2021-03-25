package com.agora.data.provider;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;

class StoreSource implements IStoreSource {

    private FirebaseFirestore db;

    public StoreSource() {
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public Observable<User> login(@NonNull User user) {
        return Observable.create(emitter -> {
            String objectId = user.getObjectId();

            if (TextUtils.isEmpty(objectId)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", user.getName());
                map.put("avatar", user.getAvatar());

                db.collection(DataProvider.TAG_TABLE_USER)
                        .add(map)
                        .addOnSuccessListener(documentReference -> {
                            String objectIdNew = documentReference.getId();
                            user.setObjectId(objectIdNew);
                            emitter.onNext(user);
                        })
                        .addOnFailureListener(new OnFailureListener() {

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                emitter.onError(e);
                            }
                        });
            } else {
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", user.getName());
                map.put("avatar", user.getAvatar());

                db.collection(DataProvider.TAG_TABLE_USER)
                        .document(objectId)
                        .set(map)
                        .addOnSuccessListener(aVoid -> emitter.onNext(user))
                        .addOnFailureListener(new OnFailureListener() {

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                emitter.onError(e);
                            }
                        });
            }
        });
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        return Observable.create(emitter -> {
            String objectId = user.getObjectId();
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", user.getName());
            map.put("avatar", user.getAvatar());

            db.collection(DataProvider.TAG_TABLE_USER)
                    .document(objectId)
                    .set(map)
                    .addOnSuccessListener(aVoid -> emitter.onNext(user))
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    @Override
    public Observable<List<Room>> getRooms() {
        return Observable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_ROOM)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Room> rooms = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Room room = document.toObject(Room.class);
                                room.setObjectId(document.getId());
                                rooms.add(room);
                            }
                            emitter.onNext(rooms);
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Observable<Room> getRoomCountInfo(@NonNull Room room) {
        return Observable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            List<Member> speakers = new ArrayList<>();
                            int members = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int isSpeaker = document.get("isSpeaker", Integer.class);
                                if (isSpeaker == 1) {
                                    speakers.add(document.toObject(Member.class));
                                }
                                members++;
                            }
                            room.setSpeakers(speakers);
                            room.setMembers(members);
                            emitter.onNext(room);
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Maybe<Room> getRoomSpeakersInfo(@NonNull Room room) {
        return Maybe.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo("isSpeaker", 1)
                    .limit(3)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            List<Member> members = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                members.add(document.toObject(Member.class));
                            }
                            room.setSpeakers(members);
                            emitter.onSuccess(room);
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Observable<Room> creatRoom(@NonNull Room room) {
        return Observable.create(emitter -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("channelName", room.getChannelName());
            map.put("anchorId", room.getAnchorId());

            db.collection(DataProvider.TAG_TABLE_ROOM)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectId = documentReference.getId();
                        room.setObjectId(objectId);
                        emitter.onNext(room);
                    })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    @Override
    public Maybe<Room> getRoom(@NonNull Room room) {
        return Maybe.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_ROOM)
                    .document(room.getObjectId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                emitter.onSuccess(document.toObject(Room.class));
                            } else {
                                emitter.onComplete();
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }

    @Override
    public Observable<List<Member>> getMembers(@NonNull Room room) {
        return Observable.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<Member> members = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Member member = document.toObject(Member.class);
                                    member.setObjectId(document.getId());
                                    members.add(member);
                                }
                                emitter.onNext(members);
                            } else {
                                emitter.onError(task.getException());
                            }
                        }
                    });
        });
    }

    @Override
    public Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId) {
        return Maybe.create(emitter -> {
            db.collection(DataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo("roomId.objectId", roomId)
                    .whereEqualTo("userId.objectId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                emitter.onSuccess(document.toObject(Member.class));
                                break;
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        });
    }
}
