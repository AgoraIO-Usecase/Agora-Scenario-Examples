//
//  Firebase.swift
//  Scene-Examples
//
//  Created by XC on 2021/5/11.
//
#if FIREBASE
import Foundation
import Firebase
import FirebaseFirestoreSwift
import RxSwift
import Core
import Database_Firebase

extension User {
    static func from(object: DocumentSnapshot) throws -> User {
        let data = object.data()!
        let name: String = data[NAME] as! String
        let avatar: String = data[AVATAR] as! String
        return User(id: object.documentID, name: name, avatar: avatar)
    }
    
    static func create(user: User) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (TABLE, [NAME: user.name, AVATAR: user.avatar as Any], nil)
        }
    }
    
    static func getUser(by objectId: String) -> Observable<Result<User>> {
        return Database.query(className: TABLE, objectId: objectId) { (data: DocumentSnapshot) -> User in
            return try User.from(object: data)
        }
    }
    
    static func randomUser() -> Observable<Result<User>>  {
        let user = User(id: "", name: Utils.randomString(length: 8), avatar: Utils.randomAvatar())
        return create(user: user).map { result in
            if (result.success) {
                user.id = result.data!
                return Result(success: true, data: user)
            } else {
                return Result(success: false, message: result.message)
            }
        }
    }
    
    func update(name: String) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (User.TABLE, [User.NAME: name], self.id)
        }
        .map { result in
            if (result.success) {
                self.name = name
            }
            return Result(success: result.success, message: result.message)
        }
    }
}

extension Room {
    static func queryMemberCount(roomId: String) -> Observable<Result<Int>> {
        return Database.query(className: Member.TABLE) { collectionReference -> Query in
            collectionReference.whereField(Member.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: roomId))
        } transform: { data -> Int in
            data.count
        }
    }
    
    static func querySpeakerCount(roomId: String) -> Observable<Result<Int>> {
        return Database.query(className: Member.TABLE) { collectionReference -> Query in
            collectionReference
                .whereField(Member.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: roomId))
                .whereField(Member.ROLE, isEqualTo: RoomRole.listener.rawValue)
        } transform: { data -> Int in
            data.count
        }
    }
    
    static func create(room: Room) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (TABLE, [CHANNEL_NAME: room.channelName, ANCHOR_ID: Database.document(table: User.TABLE, id: room.anchor.id)], nil)
        }
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<Room>> {
        return Database.query(className: TABLE, objectId: objectId) { (object: DocumentSnapshot) -> Room in
            let data = object.data()!
            let channelName: String = data[CHANNEL_NAME] as! String
            let anchorRef = data[ANCHOR_ID] as! DocumentReference
            return Room(id: object.documentID, channelName: channelName, anchor: User(id: anchorRef.documentID, name: "", avatar: nil))
        }.flatMap { result -> Observable<Result<Room>> in
            return result.onSuccess { () -> Observable<Result<Room>> in
                let room: Room = result.data!
                return Observable.zip(
                    User.getUser(by: room.anchor.id),
                    Room.queryMemberCount(roomId: room.id),
                    Room.querySpeakerCount(roomId: room.id)
                ).map { (data: (Result<User>, Result<Int>, Result<Int>)) -> Result<Room> in
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
    
    func delete() -> Observable<Result<Void>> {
        return Database.delete(className: Room.TABLE, objectId: id)
    }
    
    static func getRooms() -> Observable<Result<Array<Room>>> {
        return Database.query(className: TABLE) { (ref: CollectionReference) -> Query in
            ref.order(by: "createdAt", descending: true)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            return data.map { (object: DocumentSnapshot) -> String in
                return object.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<Room>>> in
            return result.onSuccess { () -> Observable<Result<Array<Room>>> in
                let roomIds = result.data!
                if (roomIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(roomIds.map({ (roomId: String) -> Observable<Result<Room>> in
                        return Room.getRoom(by: roomId)
                    })).map { (results: [Result<Room>]) -> Result<Array<Room>> in
                        if let failed = results.first(where: { (result: Result<Room>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<Room>) -> Room in
                                _result.data!
                            }))
                        }
                    }
                }
            }
        }
    }
    
    static func update(room: Room) -> Observable<Result<String>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            return (TABLE, [CHANNEL_NAME: room.channelName, ANCHOR_ID: Database.document(table: User.TABLE, id: room.anchor.id)], room.id)
        }
    }
    
    func getMembers() -> Observable<Result<Array<Member>>> {
        return Database.query(className: Member.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(Member.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.id))
               .order(by: "createdAt", descending: false)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            return data.map { (snapshot: DocumentSnapshot) -> String in
                snapshot.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<Member>>> in
            return result.onSuccess { () -> Observable<Result<Array<Member>>> in
                let memberIds = result.data!
                if (memberIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(memberIds.map({ (memberId: String) -> Observable<Result<Member>> in
                        return Member.getMember(by: memberId)
                    })).map { (results: [Result<Member>]) -> Result<Array<Member>> in
                        if let failed = results.first(where: { (result: Result<Member>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<Member>) -> Member in
                                let member = _result.data!
                                member.isManager = member.user.id == self.anchor.id
                                return member
                            }))
                        }
                    }
                }
            }
        }
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<Member>>> {
        return Database.query(className: Member.TABLE) { (ref: CollectionReference) -> Query in
            ref.whereField(Member.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.id))
               .whereField(Member.ROLE, isNotEqualTo: RoomRole.listener.rawValue)
               .limit(to: 3)
        } transform: { (data: [DocumentSnapshot]) -> [String] in
            data.map { (snapshot: DocumentSnapshot) -> String in
                snapshot.documentID
            }
        }.flatMap { (result: Result<[String]>) -> Observable<Result<Array<Member>>> in
            return result.onSuccess { () -> Observable<Result<Array<Member>>> in
                let memberIds = result.data!
                if (memberIds.count == 0) {
                    return Observable.just(Result(success: true, data: []))
                } else {
                    return Observable.zip(memberIds.map({ (memberId: String) -> Observable<Result<Member>> in
                        return Member.getMember(by: memberId)
                    })).map { (results: [Result<Member>]) -> Result<Array<Member>> in
                        if let failed = results.first(where: { (result: Result<Member>) -> Bool in
                            return !result.success
                        }) {
                            return Result(success: false, message: failed.message)
                        } else {
                            return Result(success: true, data: results.map({ (_result: Result<Member>) -> Member in
                                let member = _result.data!
                                member.isManager = member.user.id == self.anchor.id
                                return member
                            }))
                        }
                    }
                }
            }
        }
    }
    
    func subscribeMembers() -> Observable<Result<Array<Member>>> {
        return Database.subscribe(className: Member.TABLE) { [unowned self] (ref: CollectionReference) -> Query in
            ref.whereField(Member.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.id))
        }
        .flatMap { [unowned self] result -> Observable<Result<Array<Member>>> in
            return result.onSuccess { self.getMembers() }
        }
    }
}

extension Member {
    func join(streamId: UInt) -> Observable<Result<Void>>{
        self.streamId = streamId
        return Database.delete(className: Member.TABLE) { collectionReference -> Query in
            collectionReference.whereField(Member.USER, isEqualTo: Database.document(table: User.TABLE, id: self.user.id))
        }
        .concatMap { result -> Observable<Result<Void>> in
            if (result.success) {
                return Database.save {
                    return self.toData()
                }.map { result in
                    if (result.success) {
                        self.id = result.data!
                    }
                    return Result(success: result.success, message: result.message)
                }
            } else {
                return Observable.just(Result(success: false, message: result.message))
            }
        }
    }
    
    func toData() -> (String, [String: Any], String?) {
        return (
            Member.TABLE,
            [
                Member.ROOM: Database.document(table: Room.TABLE, id: self.room.id),
                Member.USER: Database.document(table: User.TABLE, id: self.user.id),
                Member.STREAM_ID: self.streamId,
                Member.ROLE: self.role.rawValue,
                Member.MUTED: self.isMuted ? 1 : 0,
                Member.SELF_MUTED: self.isSelfMuted ? 1 : 0
            ],
            nil
        )
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save mute \(mute)", level: .info)
            return (Member.TABLE, [Member.MUTED: mute ? 1 : 0], self.id)
        }
        .map { $0.transform() }
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save selfMute \(mute)", level: .info)
            return (Member.TABLE, [Member.SELF_MUTED: mute ? 1 : 0], self.id)
        }
        .map { $0.transform() }
    }
    
    func asListener() -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asListener", level: .info)
            return (Member.TABLE, [Member.ROLE: RoomRole.listener.rawValue], self.id)
        }
        .map { $0.transform() }
    }
    
    func asLeftSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let data: [String: Any]
            if (agree) {
                data = [
                    Member.ROLE: agree ? RoomRole.leftSpeaker.rawValue : RoomRole.listener.rawValue,
                    Member.MUTED: 0, Member.SELF_MUTED: 0
                ]
            } else {
                data = [Member.ROLE: agree ? RoomRole.leftSpeaker.rawValue : RoomRole.listener.rawValue]
            }
            return (Member.TABLE, data, self.id)
        }
        .map { $0.transform() }
    }
    
    func asRightSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return Database.save { () -> (String, data: [String : Any], String?) in
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let data: [String: Any]
            if (agree) {
                data = [
                    Member.ROLE: agree ? RoomRole.rightSpeaker.rawValue : RoomRole.listener.rawValue,
                    Member.MUTED: 0, Member.SELF_MUTED: 0
                ]
            } else {
                data = [Member.ROLE: agree ? RoomRole.rightSpeaker.rawValue : RoomRole.listener.rawValue]
            }
            return (Member.TABLE, data, self.id)
        }
        .map { $0.transform() }
    }
    
    func leave() -> Observable<Result<Void>> {
        Logger.log(message: "Member leave isManager:\(isManager)", level: .info)
        if (self.isManager) {
            return Observable.zip(
                room.delete(),
                Database.delete(className: Member.TABLE, queryWhere: { collectionReference -> Query in
                    collectionReference.whereField(Member.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.room.id))
                }),
                Database.delete(className: Action.TABLE, queryWhere: { collectionReference -> Query in
                    collectionReference.whereField(Action.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.room.id))
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
            return Database.delete(className: Member.TABLE) { collectionReference -> Query in
                collectionReference.whereField(Member.USER, isEqualTo: Database.document(table: User.TABLE, id: self.user.id))
            }
        }
    }
    
    func action(with action: ActionType) -> Action {
        return Action(id: "", action: action, status: .ing, member: self, room: self.room)
    }
    
    func subscribeActions() -> Observable<Result<Action>> {
        return Database.subscribe(className: Action.TABLE) { [unowned self] (ref: CollectionReference) -> Query in
            if (!isManager) {
                return ref.whereField(Action.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.room.id))
                    .whereField(Action.MEMBER, isEqualTo: Database.document(table: Member.TABLE, id: self.id))
            } else {
                return ref.whereField(Action.ROOM, isEqualTo: Database.document(table: Room.TABLE, id: self.room.id))
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
            return Action.get(objectId: id!)
        }
    }
    
    func handsup() -> Observable<Result<Void>> {
        let action = self.action(with: .handsUp)
        return Database.save { () -> (String, data: [String : Any], String?) in
            return action.toData()
        }
        .map { $0.transform() }
    }
    
    func requestLeft() -> Observable<Result<Void>> {
        let action = self.action(with: .requestLeft)
        return Database.save {
            return action.toData()
        }
        .map { $0.transform() }
    }
    
    func requestRight() -> Observable<Result<Void>> {
        let action = self.action(with: .requestRight)
        return Database.save {
            return action.toData()
        }
        .map { $0.transform() }
    }
    
    func inviteSpeaker(member: Member) -> Observable<Result<Void>> {
        let action = self.action(with: .invite)
        action.member = member
        return Database.save { () -> (String, data: [String : Any], String?) in
            return action.toData()
        }
        .map { $0.transform() }
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        let action = self.action(with: .invite)
        action.status = .refuse
        return Database.save { () -> (String, data: [String : Any], String?) in
            return action.toData()
        }
        .map { $0.transform() }
    }
    
    static func getMember(by objectId: String) -> Observable<Result<Member>> {
        return Database.query(className: TABLE, objectId: objectId) { (object: DocumentSnapshot) -> Member in
            let data = object.data()!
            
            let isMuted = (data[MUTED] as? Int ?? 0) == 1
            let _role = data[ROLE] as? Int ?? 0
            let role: RoomRole
            switch _role {
            case RoomRole.listener.rawValue:
                role = .listener
            case RoomRole.manager.rawValue:
                role = .manager
            case RoomRole.leftSpeaker.rawValue:
                role = .leftSpeaker
            case RoomRole.rightSpeaker.rawValue:
                role = .rightSpeaker
            default:
                role = .listener
            }
            let isSelfMuted = (data[SELF_MUTED] as? Int ?? 0) == 1
            let streamId = data[STREAM_ID] as? UInt ?? 0
            
            let userRef = data[USER] as! DocumentReference
            let roomRef = data[ROOM] as! DocumentReference
            
            let room = Room(id: roomRef.documentID, channelName: "", anchor: User(id: "", name: "", avatar: nil))
            let user = User(id: userRef.documentID, name: "", avatar: nil)
            return Member(id: object.documentID, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, room: room, streamId: streamId, user: user)
        }.flatMap { result -> Observable<Result<Member>> in
            return result.onSuccess { () -> Observable<Result<Member>> in
                let member: Member = result.data!
                return Observable.zip(
                    User.getUser(by: member.user.id),
                    Room.getRoom(by: member.room.id)
                ).map { (data: (Result<User>, Result<Room>)) -> Result<Member> in
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
}

extension Action {
    func toData() -> (String, [String: Any], String?) {
        return (
            Action.TABLE,
            [
                Action.ROOM: Database.document(table: Room.TABLE, id: room.id),
                Action.MEMBER: Database.document(table: Member.TABLE, id: member.id),
                Action.ACTION: action.rawValue,
                Action.STATUS: status.rawValue
            ],
            nil
        )
    }
    
    static func get(objectId: String) -> Observable<Result<Action>> {
        return Database.query(className: Action.TABLE, objectId: objectId) { (object: DocumentSnapshot) -> Action in
            let data = object.data()!
            let action: Int = data[ACTION] as! Int
            let status: Int = data[STATUS] as! Int
            let roomReference = data[ROOM] as! DocumentReference
            let memberReference = data[MEMBER] as! DocumentReference
            
            let room = Room(id: roomReference.documentID, channelName: "", anchor: User(id: "", name: "", avatar: nil))
            let member = Member(id: memberReference.documentID, isMuted: false, isSelfMuted: false, role: RoomRole.listener, room: room, streamId: 0, user: User(id: "", name: "", avatar: nil))
            return Action(id: object.documentID, action: ActionType.from(value: action), status: ActionStatus.from(value: status), member: member, room: room)
        }.flatMap { result -> Observable<Result<Action>> in
            return result.onSuccess { () -> Observable<Result<Action>> in
                let action = result.data!
                return Observable.zip(
                    Room.getRoom(by: action.room.id),
                    Member.getMember(by: action.member.id)
                ).map { (data: (Result<Room>, Result<Member>)) -> Result<Action> in
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
    
    func setLeftSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return member.asLeftSpeaker(agree: agree)
    }
    
    func setRightSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return member.asRightSpeaker(agree: agree)
    }
    
    func setLeftInvition(agree: Bool) -> Observable<Result<Void>> {
        if (agree) {
            return member.asLeftSpeaker(agree: agree)
        } else {
            return member.rejectInvition()
        }
    }
    
    func setRightInvition(agree: Bool) -> Observable<Result<Void>> {
        if (agree) {
            return member.asRightSpeaker(agree: agree)
        } else {
            return member.rejectInvition()
        }
    }
}
#endif
