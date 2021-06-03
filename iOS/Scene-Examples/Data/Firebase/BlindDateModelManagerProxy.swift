//
//  BlindDateModelManagerProxy.swift
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

class FirebaseBlindDateModelProxy: IBlindDateModelManagerProxy {
    
    static func queryMemberCount(roomId: String) -> Observable<Result<Int>> {
        return Database.query(className: BlindDateMember.TABLE) { collectionReference -> Query in
            collectionReference.whereField(BlindDateMember.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: roomId))
        } transform: { data -> Int in
            data.count
        }
    }
    
    static func querySpeakerCount(roomId: String) -> Observable<Result<Int>> {
        return Database.query(className: BlindDateMember.TABLE) { collectionReference -> Query in
            collectionReference
                .whereField(BlindDateMember.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: roomId))
                .whereField(BlindDateMember.ROLE, isEqualTo: BlindDateRoomRole.listener.rawValue)
        } transform: { data -> Int in
            data.count
        }
    }
    
    static func toData(member: BlindDateMember) -> (String, [String: Any], String?) {
        return (
            BlindDateMember.TABLE,
            [
                BlindDateMember.ROOM: Database.document(table: BlindDateRoom.TABLE, id: member.room.id),
                BlindDateMember.USER: Database.document(table: User.TABLE, id: member.user.id),
                BlindDateMember.STREAM_ID: member.streamId,
                BlindDateMember.ROLE: member.role.rawValue,
                BlindDateMember.MUTED: member.isMuted ? 1 : 0,
                BlindDateMember.SELF_MUTED: member.isSelfMuted ? 1 : 0
            ],
            nil
        )
    }
    
    static func toData(action: BlindDateAction) -> (String, [String: Any], String?) {
        return (
            BlindDateAction.TABLE,
            [
                BlindDateAction.ROOM: Database.document(table: BlindDateRoom.TABLE, id: action.room.id),
                BlindDateAction.MEMBER: Database.document(table: BlindDateMember.TABLE, id: action.member.id),
                BlindDateAction.ACTION: action.action.rawValue,
                BlindDateAction.STATUS: action.status.rawValue
            ],
            nil
        )
    }
    
    func getAction(objectId: String) -> Observable<Result<BlindDateAction>> {
        return Database.query(className: BlindDateAction.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> BlindDateAction in
            let data = object.data()!
            let action: Int = data[BlindDateAction.ACTION] as! Int
            let status: Int = data[BlindDateAction.STATUS] as! Int
            let roomReference = data[BlindDateAction.ROOM] as! DocumentReference
            let memberReference = data[BlindDateAction.MEMBER] as! DocumentReference
            
            let room = BlindDateRoom(id: roomReference.documentID, channelName: "", anchor: User(id: "", name: "", avatar: nil))
            let member = BlindDateMember(id: memberReference.documentID, isMuted: false, isSelfMuted: false, role: BlindDateRoomRole.listener, room: room, streamId: 0, user: User(id: "", name: "", avatar: nil))
            return BlindDateAction(id: object.documentID, action: BlindDateActionType.from(value: action), status: BlindDateActionStatus.from(value: status), member: member, room: room)
        }.flatMap { result -> Observable<Result<BlindDateAction>> in
            return result.onSuccess { () -> Observable<Result<BlindDateAction>> in
                let action = result.data!
                return Observable.zip(
                    self.getRoom(by: action.room.id),
                    self.getMember(by: action.member.id)
                ).map { (data: (Result<BlindDateRoom>, Result<BlindDateMember>)) -> Result<BlindDateAction> in
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
    
    func getMember(by objectId: String) -> Observable<Result<BlindDateMember>> {
        return Database.query(className: BlindDateMember.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> BlindDateMember in
            let data = object.data()!
            
            let isMuted = (data[BlindDateMember.MUTED] as? Int ?? 0) == 1
            let _role = data[BlindDateMember.ROLE] as? Int ?? 0
            let role: BlindDateRoomRole
            switch _role {
            case BlindDateRoomRole.listener.rawValue:
                role = .listener
            case BlindDateRoomRole.manager.rawValue:
                role = .manager
            case BlindDateRoomRole.leftSpeaker.rawValue:
                role = .leftSpeaker
            case BlindDateRoomRole.rightSpeaker.rawValue:
                role = .rightSpeaker
            default:
                role = .listener
            }
            let isSelfMuted = (data[BlindDateMember.SELF_MUTED] as? Int ?? 0) == 1
            let streamId = data[BlindDateMember.STREAM_ID] as? UInt ?? 0
            
            let userRef = data[BlindDateMember.USER] as! DocumentReference
            let roomRef = data[BlindDateMember.ROOM] as! DocumentReference
            
            let room = BlindDateRoom(id: roomRef.documentID, channelName: "", anchor: User(id: "", name: "", avatar: nil))
            let user = User(id: userRef.documentID, name: "", avatar: nil)
            return BlindDateMember(id: object.documentID, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, room: room, streamId: streamId, user: user)
        }.flatMap { result -> Observable<Result<BlindDateMember>> in
            return result.onSuccess { () -> Observable<Result<BlindDateMember>> in
                let member: BlindDateMember = result.data!
                return Observable.zip(
                    User.getUser(by: member.user.id),
                    self.getRoom(by: member.room.id)
                ).map { (data: (Result<User>, Result<BlindDateRoom>)) -> Result<BlindDateMember> in
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
    
    func create(room: BlindDateRoom) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (BlindDateRoom.TABLE, [BlindDateRoom.CHANNEL_NAME: room.channelName, BlindDateRoom.ANCHOR_ID: Database.document(table: User.TABLE, id: room.anchor.id)], nil)
        }
    }
    
    func delete(room: BlindDateRoom) -> Observable<Result<Void>> {
        return Database.delete(className: BlindDateRoom.TABLE, objectId: room.id)
    }
    
    func getRooms() -> Observable<Result<Array<BlindDateRoom>>> {
        return Database.query(className: BlindDateRoom.TABLE) { (ref: CollectionReference) -> Query in
            ref.order(by: "createdAt", descending: true)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            return data.map { (object: DocumentSnapshot) -> String in
                return object.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<BlindDateRoom>>> in
            return result.onSuccess { () -> Observable<Result<Array<BlindDateRoom>>> in
                let roomIds = result.data!
                if (roomIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(roomIds.map({ (roomId: String) -> Observable<Result<BlindDateRoom>> in
                        return self.getRoom(by: roomId)
                    })).map { (results: [Result<BlindDateRoom>]) -> Result<Array<BlindDateRoom>> in
                        if let failed = results.first(where: { (result: Result<BlindDateRoom>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<BlindDateRoom>) -> BlindDateRoom in
                                _result.data!
                            }))
                        }
                    }
                }
            }
        }
    }
    
    func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>> {
        return Database.query(className: BlindDateRoom.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> BlindDateRoom in
            let data = object.data()!
            let channelName: String = data[BlindDateRoom.CHANNEL_NAME] as! String
            let anchorRef = data[BlindDateRoom.ANCHOR_ID] as! DocumentReference
            return BlindDateRoom(id: object.documentID, channelName: channelName, anchor: User(id: anchorRef.documentID, name: "", avatar: nil))
        }.flatMap { result -> Observable<Result<BlindDateRoom>> in
            return result.onSuccess { () -> Observable<Result<BlindDateRoom>> in
                let room: BlindDateRoom = result.data!
                return Observable.zip(
                    User.getUser(by: room.anchor.id),
                    FirebaseBlindDateModelProxy.queryMemberCount(roomId: room.id),
                    FirebaseBlindDateModelProxy.querySpeakerCount(roomId: room.id)
                ).map { (data: (Result<User>, Result<Int>, Result<Int>)) -> Result<BlindDateRoom> in
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
    
    func update(room: BlindDateRoom) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (BlindDateRoom.TABLE, [BlindDateRoom.CHANNEL_NAME: room.channelName, BlindDateRoom.ANCHOR_ID: Database.document(table: User.TABLE, id: room.anchor.id)], room.id)
        }
    }
    
    func getMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return Database.query(className: BlindDateMember.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(BlindDateMember.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: room.id))
               .order(by: "createdAt", descending: false)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            return data.map { (snapshot: DocumentSnapshot) -> String in
                snapshot.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<BlindDateMember>>> in
            return result.onSuccess { () -> Observable<Result<Array<BlindDateMember>>> in
                let memberIds = result.data!
                if (memberIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(memberIds.map({ (memberId: String) -> Observable<Result<BlindDateMember>> in
                        return self.getMember(by: memberId)
                    })).map { (results: [Result<BlindDateMember>]) -> Result<Array<BlindDateMember>> in
                        if let failed = results.first(where: { (result: Result<BlindDateMember>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<BlindDateMember>) -> BlindDateMember in
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
    
    func getCoverSpeakers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return Database.query(className: BlindDateMember.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(BlindDateMember.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: room.id))
               .whereField(BlindDateMember.ROLE, isNotEqualTo: BlindDateRoomRole.listener.rawValue)
               .limit(to: 3)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            data.map { (snapshot: DocumentSnapshot) -> String in
                snapshot.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<BlindDateMember>>> in
            return result.onSuccess { () -> Observable<Result<Array<BlindDateMember>>> in
                let memberIds = result.data!
                if (memberIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(memberIds.map({ (memberId: String) -> Observable<Result<BlindDateMember>> in
                        return self.getMember(by: memberId)
                    })).map { (results: [Result<BlindDateMember>]) -> Result<Array<BlindDateMember>> in
                        if let failed = results.first(where: { (result: Result<BlindDateMember>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<BlindDateMember>) -> BlindDateMember in
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
    
    func subscribeMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return Database.subscribe(className: BlindDateMember.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(BlindDateMember.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: room.id))
        }
        .flatMap { result -> Observable<Result<Array<BlindDateMember>>> in
            return result.onSuccess { self.getMembers(room: room) }
        }
    }
    
    func join(member: BlindDateMember, streamId: UInt) -> Observable<Result<Void>> {
        member.streamId = streamId
        return Database.delete(className: BlindDateMember.TABLE) { collectionReference -> Query in
            collectionReference.whereField(BlindDateMember.USER, isEqualTo: Database.document(table: User.TABLE, id: member.user.id))
        }
        .concatMap { result -> Observable<Result<Void>> in
            if (result.success) {
                return Database.save {
                    return FirebaseBlindDateModelProxy.toData(member: member)
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
    
    func mute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save mute \(mute)", level: .info)
            return (BlindDateMember.TABLE, [BlindDateMember.MUTED: mute ? 1 : 0], member.id)
        }
        .map { $0.transform() }
    }
    
    func selfMute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save selfMute \(mute)", level: .info)
            return (BlindDateMember.TABLE, [BlindDateMember.SELF_MUTED: mute ? 1 : 0], member.id)
        }
        .map { $0.transform() }
    }
    
    func asListener(member: BlindDateMember) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asListener", level: .info)
            return (BlindDateMember.TABLE, [BlindDateMember.ROLE: BlindDateRoomRole.listener.rawValue], member.id)
        }
        .map { $0.transform() }
    }
    
    func asLeftSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let data: [String: Any]
            if (agree) {
                data = [
                    BlindDateMember.ROLE: agree ? BlindDateRoomRole.leftSpeaker.rawValue : BlindDateRoomRole.listener.rawValue,
                    BlindDateMember.MUTED: 0, BlindDateMember.SELF_MUTED: 0
                ]
            } else {
                data = [BlindDateMember.ROLE: agree ? BlindDateRoomRole.leftSpeaker.rawValue : BlindDateRoomRole.listener.rawValue]
            }
            return (BlindDateMember.TABLE, data, member.id)
        }
        .map { $0.transform() }
    }
    
    func asRightSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let data: [String: Any]
            if (agree) {
                data = [
                    BlindDateMember.ROLE: agree ? BlindDateRoomRole.rightSpeaker.rawValue : BlindDateRoomRole.listener.rawValue,
                    BlindDateMember.MUTED: 0, BlindDateMember.SELF_MUTED: 0
                ]
            } else {
                data = [BlindDateMember.ROLE: agree ? BlindDateRoomRole.rightSpeaker.rawValue : BlindDateRoomRole.listener.rawValue]
            }
            return (BlindDateMember.TABLE, data, member.id)
        }
        .map { $0.transform() }
    }
    
    func leave(member: BlindDateMember) -> Observable<Result<Void>> {
        Logger.log(message: "Member leave isManager:\(member.isManager)", level: .info)
        if (member.isManager) {
            return Observable.zip(
                self.delete(room: member.room),
                Database.delete(className: BlindDateMember.TABLE, queryWhere: { collectionReference -> Query in
                    collectionReference.whereField(BlindDateMember.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: member.room.id))
                }),
                Database.delete(className: BlindDateAction.TABLE, queryWhere: { collectionReference -> Query in
                    collectionReference.whereField(BlindDateAction.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: member.room.id))
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
            return Database.delete(className: BlindDateMember.TABLE) { collectionReference -> Query in
                collectionReference.whereField(BlindDateMember.USER, isEqualTo: Database.document(table: User.TABLE, id: member.user.id))
            }
        }
    }
    
    func subscribeActions(member: BlindDateMember) -> Observable<Result<BlindDateAction>> {
        return Database.subscribe(className: BlindDateAction.TABLE) { (ref: CollectionReference) -> Query in
            if (!member.isManager) {
                return ref.whereField(BlindDateAction.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: member.room.id))
                    .whereField(BlindDateAction.MEMBER, isEqualTo: Database.document(table: BlindDateMember.TABLE, id: member.id))
            } else {
                return ref.whereField(BlindDateAction.ROOM, isEqualTo: Database.document(table: BlindDateRoom.TABLE, id: member.room.id))
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
    
    func handsup(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .handsUp)
        return Database.save { () -> (String, data: [String : Any], String?) in
            return FirebaseBlindDateModelProxy.toData(action: action)
        }
        .map { $0.transform() }
    }
    
    func requestLeft(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .requestLeft)
        return Database.save {
            return FirebaseBlindDateModelProxy.toData(action: action)
        }
        .map { $0.transform() }
    }
    
    func requestRight(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .requestRight)
        return Database.save {
            return FirebaseBlindDateModelProxy.toData(action: action)
        }
        .map { $0.transform() }
    }
    
    func inviteSpeaker(master: BlindDateMember, member: BlindDateMember) -> Observable<Result<Void>> {
        let action = master.action(with: .invite)
        action.member = member
        return Database.save { () -> (String, data: [String : Any], String?) in
            return FirebaseBlindDateModelProxy.toData(action: action)
        }
        .map { $0.transform() }
    }
    
    func rejectInvition(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .invite)
        action.status = .refuse
        return Database.save { () -> (String, data: [String : Any], String?) in
            return FirebaseBlindDateModelProxy.toData(action: action)
        }
        .map { $0.transform() }
    }
}

#endif
