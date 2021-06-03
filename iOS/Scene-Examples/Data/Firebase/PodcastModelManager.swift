//
//  PodcastModelManagerProxy.swift
//  Core
//
//  Created by XC on 2021/6/2.
//
#if FIREBASE
import Foundation
import Firebase
import FirebaseFirestoreSwift
import RxSwift
import Core

class FirebasePodcastModelManager: IPodcastModelManager {
    
    static func queryMemberCount(roomId: String) -> Observable<Result<Int>> {
        return Database.query(className: PodcastMember.TABLE) { collectionReference -> Query in
            collectionReference.whereField(PodcastMember.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: roomId))
        } transform: { data -> Int in
            data.count
        }
    }
    
    static func querySpeakerCount(roomId: String) -> Observable<Result<Int>> {
        return Database.query(className: PodcastMember.TABLE) { collectionReference -> Query in
            collectionReference
                .whereField(PodcastMember.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: roomId))
                .whereField(PodcastMember.IS_SPEAKER, isEqualTo: 1)
        } transform: { data -> Int in
            data.count
        }
    }
    
    static func toData(member: PodcastMember) -> (String, [String: Any], String?) {
        return (
            PodcastMember.TABLE,
            [
                PodcastMember.ROOM: Database.document(table: PodcastRoom.TABLE, id: member.room.id),
                PodcastMember.USER: Database.document(table: User.TABLE, id: member.user.id),
                PodcastMember.STREAM_ID: member.streamId,
                PodcastMember.IS_SPEAKER: member.isSpeaker ? 1 : 0,
                PodcastMember.MUTED: member.isMuted ? 1 : 0,
                PodcastMember.SELF_MUTED: member.isSelfMuted ? 1 : 0
            ],
            nil
        )
    }
    
    static func toData(action: PodcastAction) -> (String, [String: Any], String?) {
        return (
            PodcastAction.TABLE,
            [
                PodcastAction.ROOM: Database.document(table: PodcastRoom.TABLE, id: action.room.id),
                PodcastAction.MEMBER: Database.document(table: PodcastMember.TABLE, id: action.member.id),
                PodcastAction.ACTION: action.action.rawValue,
                PodcastAction.STATUS: action.status.rawValue
            ],
            nil
        )
    }
    
    func getAction(objectId: String) -> Observable<Result<PodcastAction>> {
        return Database.query(className: PodcastAction.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> PodcastAction in
            let data = object.data()!
            let action: Int = data[PodcastAction.ACTION] as! Int
            let status: Int = data[PodcastAction.STATUS] as! Int
            let roomReference = data[PodcastAction.ROOM] as! DocumentReference
            let memberReference = data[PodcastAction.MEMBER] as! DocumentReference
            
            let room = PodcastRoom(id: roomReference.documentID, channelName: "", anchor: User(id: "", name: "", avatar: nil))
            let member = PodcastMember(id: memberReference.documentID, isMuted: false, isSelfMuted: false, isSpeaker: false, room: room, streamId: 0, user: User(id: "", name: "", avatar: nil))
            return PodcastAction(id: object.documentID, action: PodcastActionType.from(value: action), status: PodcastActionStatus.from(value: status), member: member, room: room)
        }.flatMap { result -> Observable<Result<PodcastAction>> in
            return result.onSuccess { () -> Observable<Result<PodcastAction>> in
                let action = result.data!
                return Observable.zip(
                    self.getRoom(by: action.room.id),
                    self.getMember(by: action.member.id)
                ).map { (data: (Result<PodcastRoom>, Result<PodcastMember>)) -> Result<PodcastAction> in
                    let (roomResult, memberResult) = data
                    if (roomResult.success && memberResult.success) {
                        action.room = roomResult.data!
                        action.member = memberResult.data!
                        return Result(success: true, data: action)
                    } else if (!roomResult.success) {
                        return Result(success: false, message: roomResult.message)
                    } else {
                        return Result(success: false, message: memberResult.message)
                    }
                }
            }
        }
    }
    
    func getMember(by objectId: String) -> Observable<Result<PodcastMember>> {
        return Database.query(className: PodcastMember.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> PodcastMember in
            let data = object.data()!
            
            let isMuted = (data[PodcastMember.MUTED] as? Int ?? 0) == 1
            let isSpeaker = (data[PodcastMember.IS_SPEAKER] as? Int ?? 0) == 1
            let isSelfMuted = (data[PodcastMember.SELF_MUTED] as? Int ?? 0) == 1
            let streamId = data[PodcastMember.STREAM_ID] as? UInt ?? 0
            
            let userRef = data[PodcastMember.USER] as! DocumentReference
            let roomRef = data[PodcastMember.ROOM] as! DocumentReference
            
            let room = PodcastRoom(id: roomRef.documentID, channelName: "", anchor: User(id: "", name: "", avatar: nil))
            let user = User(id: userRef.documentID, name: "", avatar: nil)
            return PodcastMember(id: object.documentID, isMuted: isMuted, isSelfMuted: isSelfMuted, isSpeaker: isSpeaker, room: room, streamId: streamId, user: user)
        }.flatMap { result -> Observable<Result<PodcastMember>> in
            return result.onSuccess { () -> Observable<Result<PodcastMember>> in
                let member: PodcastMember = result.data!
                return Observable.zip(
                    User.getUser(by: member.user.id),
                    self.getRoom(by: member.room.id)
                ).map { (data: (Result<User>, Result<PodcastRoom>)) -> Result<PodcastMember> in
                    let (userResult, roomResult) = data
                    if (userResult.success && roomResult.success) {
                        member.user = userResult.data!
                        member.room = roomResult.data!
                        return Result(success: true, data: member)
                    } else if (!userResult.success) {
                        return Result(success: false, message: userResult.message)
                    } else {
                        return Result(success: false, message: roomResult.message)
                    }
                }
            }
        }
    }
    
    func create(room: PodcastRoom) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (PodcastRoom.TABLE, [PodcastRoom.CHANNEL_NAME: room.channelName, PodcastRoom.ANCHOR_ID: Database.document(table: User.TABLE, id: room.anchor.id)], nil)
        }
    }
    
    func delete(room: PodcastRoom) -> Observable<Result<Void>> {
        return Database.delete(className: PodcastRoom.TABLE, objectId: room.id)
    }
    
    func getRooms() -> Observable<Result<Array<PodcastRoom>>> {
        return Database.query(className: PodcastRoom.TABLE) { (ref: CollectionReference) -> Query in
            ref.order(by: "createdAt", descending: true)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            return data.map { (object: DocumentSnapshot) -> String in
                return object.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<PodcastRoom>>> in
            return result.onSuccess { () -> Observable<Result<Array<PodcastRoom>>> in
                let roomIds = result.data!
                if (roomIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(roomIds.map({ (roomId: String) -> Observable<Result<PodcastRoom>> in
                        return self.getRoom(by: roomId)
                    })).map { (results: [Result<PodcastRoom>]) -> Result<Array<PodcastRoom>> in
                        if let failed = results.first(where: { (result: Result<PodcastRoom>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<PodcastRoom>) -> PodcastRoom in
                                _result.data!
                            }))
                        }
                    }
                }
            }
        }
    }
    
    func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>> {
        return Database.query(className: PodcastRoom.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> PodcastRoom in
            let data = object.data()!
            let channelName: String = data[PodcastRoom.CHANNEL_NAME] as! String
            let anchorRef = data[PodcastRoom.ANCHOR_ID] as! DocumentReference
            return PodcastRoom(id: object.documentID, channelName: channelName, anchor: User(id: anchorRef.documentID, name: "", avatar: nil))
        }.flatMap { result -> Observable<Result<PodcastRoom>> in
            return result.onSuccess { () -> Observable<Result<PodcastRoom>> in
                let room: PodcastRoom = result.data!
                return Observable.zip(
                    User.getUser(by: room.anchor.id),
                    FirebasePodcastModelManager.queryMemberCount(roomId: room.id),
                    FirebasePodcastModelManager.querySpeakerCount(roomId: room.id)
                ).map { (data: (Result<User>, Result<Int>, Result<Int>)) -> Result<PodcastRoom> in
                    let (userResult, memberCountResult, speakerCountResult) = data
                    if (userResult.success && memberCountResult.success && speakerCountResult.success) {
                        room.anchor = userResult.data!
                        room.total = memberCountResult.data!
                        room.speakersTotal = speakerCountResult.data!
                        if (room.coverCharacters.count == 0) {
                            room.coverCharacters.append(room.anchor)
                        }
                        return Result(success: true, data: room)
                    } else if (!userResult.success) {
                        return Result(success: false, message: userResult.message)
                    } else if (!memberCountResult.success) {
                        return Result(success: false, message: memberCountResult.message)
                    } else {
                        return Result(success: false, message: speakerCountResult.message)
                    }
                }
            }
        }
    }
    
    func update(room: PodcastRoom) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (PodcastRoom.TABLE, [PodcastRoom.CHANNEL_NAME: room.channelName, PodcastRoom.ANCHOR_ID: Database.document(table: User.TABLE, id: room.anchor.id)], room.id)
        }
    }
    
    func getMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return Database.query(className: PodcastMember.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(PodcastMember.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: room.id))
               .order(by: "createdAt", descending: false)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            return data.map { (snapshot: DocumentSnapshot) -> String in
                snapshot.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<PodcastMember>>> in
            return result.onSuccess { () -> Observable<Result<Array<PodcastMember>>> in
                let memberIds = result.data!
                if (memberIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(memberIds.map({ (memberId: String) -> Observable<Result<PodcastMember>> in
                        return self.getMember(by: memberId)
                    })).map { (results: [Result<PodcastMember>]) -> Result<Array<PodcastMember>> in
                        if let failed = results.first(where: { (result: Result<PodcastMember>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<PodcastMember>) -> PodcastMember in
                                let member = _result.data!
                                member.isManager = member.user.id == room.anchor.id
                                return member
                            }))
                        }
                    }
                }
            }
        }
    }
    
    func getCoverSpeakers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return Database.query(className: PodcastMember.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(PodcastMember.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: room.id))
               .whereField(PodcastMember.IS_SPEAKER, isEqualTo: 1)
               .limit(to: 3)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            data.map { (snapshot: DocumentSnapshot) -> String in
                snapshot.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<PodcastMember>>> in
            return result.onSuccess { () -> Observable<Result<Array<PodcastMember>>> in
                let memberIds = result.data!
                if (memberIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(memberIds.map({ (memberId: String) -> Observable<Result<PodcastMember>> in
                        return self.getMember(by: memberId)
                    })).map { (results: [Result<PodcastMember>]) -> Result<Array<PodcastMember>> in
                        if let failed = results.first(where: { (result: Result<PodcastMember>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<PodcastMember>) -> PodcastMember in
                                let member = _result.data!
                                member.isManager = member.user.id == room.anchor.id
                                return member
                            }))
                        }
                    }
                }
            }
        }
    }
    
    func subscribeMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return Database.subscribe(className: PodcastMember.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(PodcastMember.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: room.id))
        }
        .flatMap { result -> Observable<Result<Array<PodcastMember>>> in
            return result.onSuccess { self.getMembers(room: room) }
        }
    }
    
    func join(member: PodcastMember, streamId: UInt) -> Observable<Result<Void>> {
        member.streamId = streamId
        return Database.delete(className: PodcastMember.TABLE) { collectionReference -> Query in
            collectionReference.whereField(PodcastMember.USER, isEqualTo: Database.document(table: User.TABLE, id: member.user.id))
        }
        .concatMap { result -> Observable<Result<Void>> in
            if (result.success) {
                return Database.save {
                    return FirebasePodcastModelManager.toData(member: member)
                }.map { result in
                    if (result.success) {
                        member.id = result.data!
                    }
                    return Result(success: result.success, message: result.message)
                }
            } else {
                return Observable.just(Result(success: false, message: result.message))
            }
        }
    }
    
    func mute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save mute \(mute)", level: .info)
            return (PodcastMember.TABLE, [PodcastMember.MUTED: mute ? 1 : 0], member.id)
        }
        .map { $0.transform() }
    }
    
    func selfMute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save selfMute \(mute)", level: .info)
            return (PodcastMember.TABLE, [PodcastMember.SELF_MUTED: mute ? 1 : 0], member.id)
        }
        .map { $0.transform() }
    }
    
    func asSpeaker(member: PodcastMember, agree: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let data: [String: Any]
            if (agree) {
                data = [PodcastMember.IS_SPEAKER: agree ? 1 : 0, PodcastMember.MUTED: 0, PodcastMember.SELF_MUTED: 0]
            } else {
                data = [PodcastMember.IS_SPEAKER: agree ? 1 : 0]
            }
            return (PodcastMember.TABLE, data, member.id)
        }
        .map { $0.transform() }
    }
    
    func leave(member: PodcastMember) -> Observable<Result<Void>> {
        Logger.log(message: "Member leave isManager:\(member.isManager)", level: .info)
        if (member.isManager) {
            return Observable.zip(
                self.delete(room: member.room),
                Database.delete(className: PodcastMember.TABLE, queryWhere: { collectionReference -> Query in
                    collectionReference.whereField(PodcastMember.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: member.room.id))
                }),
                Database.delete(className: PodcastAction.TABLE, queryWhere: { collectionReference -> Query in
                    collectionReference.whereField(PodcastAction.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: member.room.id))
                })
            ).map { (args) in
                let (result0, result1, result2) = args
                if (result0.success && result1.success && result2.success) {
                    return result0
                } else {
                    return result0.success ? result1.success ? result2 : result1 : result0
                }
            }
        } else {
            return Database.delete(className: PodcastMember.TABLE) { collectionReference -> Query in
                collectionReference.whereField(PodcastMember.USER, isEqualTo: Database.document(table: User.TABLE, id: member.user.id))
            }
        }
    }
    
    func subscribeActions(member: PodcastMember) -> Observable<Result<PodcastAction>> {
        return Database.subscribe(className: PodcastAction.TABLE) { (ref: CollectionReference) -> Query in
            if (!member.isManager) {
                return ref.whereField(PodcastAction.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: member.room.id))
                    .whereField(PodcastAction.MEMBER, isEqualTo: Database.document(table: PodcastMember.TABLE, id: member.id))
            } else {
                return ref.whereField(PodcastAction.ROOM, isEqualTo: Database.document(table: PodcastRoom.TABLE, id: member.room.id))
            }
        }
        .map({ (result: Result<QuerySnapshot>) -> String? in
            if (result.success) {
                let snapshot = result.data!
                let add = snapshot.documentChanges.first { (change: DocumentChange) -> Bool in
                    change.type == .added
                }
                return add?.document.documentID
            } else {
                return nil
            }
        })
        .filter { id in
            return id != nil
        }
        .concatMap { id in
            return self.getAction(objectId: id!)
        }
    }
    
    func handsup(member: PodcastMember) -> Observable<Result<Void>> {
        let action = member.action(with: .handsUp)
        return Database.save { () -> (String, data: [String : Any], String?) in
            return FirebasePodcastModelManager.toData(action: action)
        }
        .map { $0.transform() }
    }
    
    func inviteSpeaker(master: PodcastMember, member: PodcastMember) -> Observable<Result<Void>> {
        let action = member.action(with: .invite)
        action.member = member
        return Database.save { () -> (String, data: [String : Any], String?) in
            return FirebasePodcastModelManager.toData(action: action)
        }
        .map { $0.transform() }
    }
    
    func rejectInvition(member: PodcastMember) -> Observable<Result<Void>> {
        let action = member.action(with: .invite)
        action.status = .refuse
        return Database.save { () -> (String, data: [String : Any], String?) in
            return FirebasePodcastModelManager.toData(action: action)
        }
        .map { $0.transform() }
    }
}

#endif
