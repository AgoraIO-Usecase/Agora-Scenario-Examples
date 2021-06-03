//
//  BlindDateModelManagerProxy.swift
//  Core
//
//  Created by XC on 2021/6/1.
//
#if LEANCLOUD
import Foundation
import Core
import LeanCloud
import RxSwift

class LeanCloudBlindDateModelProxy: IBlindDateModelManagerProxy {
    
    static func queryMemberCount(room: LCObject) throws -> Int {
        let query = LCQuery(className: BlindDateMember.TABLE)
        try query.where(BlindDateMember.ROOM, .equalTo(room))
        return query.count().intValue
    }
    
    static func querySpeakerCount(room: LCObject) throws -> Int {
        let query = LCQuery(className: BlindDateMember.TABLE)
        try query.where(BlindDateMember.ROOM, .equalTo(room))
        try query.where(BlindDateMember.ROLE, .notEqualTo(BlindDateRoomRole.listener.rawValue))
        return query.count().intValue
    }
    
    static func from(object: LCObject) throws -> BlindDateRoom {
        let objectId: String = object.objectId!.stringValue!
        let channelName: String = object.get(BlindDateRoom.CHANNEL_NAME)!.stringValue!
        let anchor: User = try LeanCloudUserProxy.from(object: object.get(BlindDateRoom.ANCHOR_ID) as! LCObject)
        let room = BlindDateRoom(id: objectId, channelName: channelName, anchor: anchor)
        room.total = try queryMemberCount(room: object)
        room.speakersTotal = try querySpeakerCount(room: object)
        if (room.coverCharacters.count == 0) {
            room.coverCharacters.append(room.anchor)
        }
        return room
    }
    
    static func from(object: LCObject, room: BlindDateRoom) throws -> BlindDateMember {
        let userObject = object.get(BlindDateMember.USER) as! LCObject
        let user = try LeanCloudUserProxy.from(object: userObject)
        let isMuted = (object.get(BlindDateMember.MUTED)?.intValue ?? 0) == 1
        let _role = object.get(BlindDateMember.ROLE)?.intValue ?? 0
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
        let isSelfMuted = (object.get(BlindDateMember.SELF_MUTED)?.intValue ?? 0) == 1
        let streamId = object.get(BlindDateMember.STREAM_ID)?.uintValue ?? 0
        let id = object.objectId!.stringValue!
        return BlindDateMember(id: id, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, room: room, streamId: streamId, user: user)
    }
    
    static func toObject(member: BlindDateMember) throws -> LCObject {
        let object = LCObject(className: BlindDateMember.TABLE)
        try object.set(BlindDateMember.ROOM, value: LCObject(className: BlindDateRoom.TABLE, objectId: member.room.id))
        try object.set(BlindDateMember.USER, value: LCObject(className: User.TABLE, objectId: member.user.id))
        try object.set(BlindDateMember.STREAM_ID, value: member.streamId)
        try object.set(BlindDateMember.ROLE, value: member.role.rawValue)
        try object.set(BlindDateMember.MUTED, value: member.isMuted ? 1 : 0)
        try object.set(BlindDateMember.SELF_MUTED, value: member.isSelfMuted ? 1 : 0)
        return object
    }

    static func toActionObject(action: BlindDateAction) throws -> LCObject {
        let object = LCObject(className: BlindDateAction.TABLE)
        try object.set(BlindDateAction.ROOM, value: LCObject(className: BlindDateRoom.TABLE, objectId: action.room.id))
        try object.set(BlindDateAction.MEMBER, value: LCObject(className: BlindDateMember.TABLE, objectId: action.member.id))
        try object.set(BlindDateAction.ACTION, value: action.action.rawValue)
        try object.set(BlindDateAction.STATUS, value: action.status.rawValue)
        return object
    }
    
    static func from(object: LCObject) throws -> BlindDateAction {
        let id = object.objectId!.stringValue!
        let room: BlindDateRoom = try from(object: object.get(BlindDateAction.ROOM) as! LCObject)
        let member: BlindDateMember = try from(object: object.get(BlindDateAction.MEMBER) as! LCObject, room: room)
        let action: Int = object.get(BlindDateAction.ACTION)!.intValue!
        let status: Int = object.get(BlindDateAction.STATUS)!.intValue!
        
        return BlindDateAction(id: id, action: BlindDateActionType.from(value: action), status: BlindDateActionStatus.from(value: status), member: member, room: room)
    }
    
    static func getBlindDateAction(objectId: String) -> Observable<Result<BlindDateAction>> {
        return Database.query(className: BlindDateAction.TABLE, objectId: objectId) { (query) in
            try query.where(BlindDateAction.ROOM, .included)
            try query.where("\(BlindDateAction.ROOM).\(BlindDateRoom.ANCHOR_ID)", .included)
            try query.where(BlindDateAction.MEMBER, .included)
            try query.where("\(BlindDateAction.MEMBER).\(BlindDateMember.USER)", .included)
        } transform: { (object: LCObject) -> BlindDateAction in
            return try from(object: object)
        }
    }
    
    func create(room: BlindDateRoom) -> Observable<Result<String>> {
        return Database.save {
            let object = LCObject(className: BlindDateRoom.TABLE)
            let anchor = LCObject(className: User.TABLE, objectId: room.anchor.id)
            try object.set(BlindDateRoom.CHANNEL_NAME, value: room.channelName)
            try object.set(BlindDateRoom.ANCHOR_ID, value: anchor)
            return object
        }
    }
    
    func delete(room: BlindDateRoom) -> Observable<Result<Void>> {
        return Database.delete(className: BlindDateRoom.TABLE, objectId: room.id)
    }
    
    func getRooms() -> Observable<Result<Array<BlindDateRoom>>> {
        return Database.query(className: BlindDateRoom.TABLE) { (query) in
            try query.where(BlindDateRoom.CHANNEL_NAME, .selected)
            try query.where(BlindDateRoom.ANCHOR_ID, .selected)
            try query.where(BlindDateRoom.ANCHOR_ID, .included)
            try query.where("createdAt", .descending)
        } transform: { (list) -> Array<BlindDateRoom> in
            let rooms: Array<BlindDateRoom> = try list.map { room in
                return try LeanCloudBlindDateModelProxy.from(object: room)
            }
            return rooms
        }
    }
    
    func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>> {
        return Database.query(className: BlindDateRoom.TABLE, objectId: objectId) { query in
            try query.where(BlindDateRoom.ANCHOR_ID, .included)
        } transform: { (data: LCObject) -> BlindDateRoom in
            let channelName: String = data.get(BlindDateRoom.CHANNEL_NAME)!.stringValue!
            let anchor = try LeanCloudUserProxy.from(object: data.get(BlindDateRoom.ANCHOR_ID) as! LCObject)
            return BlindDateRoom(id: objectId, channelName: channelName, anchor: anchor)
        }
    }
    
    func update(room: BlindDateRoom) -> Observable<Result<String>> {
        return Database.save {
            let object = LCObject(className: BlindDateRoom.TABLE, objectId: room.id)
            try object.set(BlindDateRoom.CHANNEL_NAME, value: room.channelName)
            try object.set(BlindDateRoom.ANCHOR_ID, value: LCObject(className: User.TABLE, objectId: room.anchor.id))
            return object
        }
    }
    
    func getMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return Database.query(className: BlindDateMember.TABLE) { (query) in
            let room = LCObject(className: BlindDateRoom.TABLE, objectId: room.id)
            try query.where(BlindDateMember.ROOM, .equalTo(room))
            try query.where(BlindDateMember.USER, .included)
            try query.where("createdAt", .ascending)
        } transform: { (list) -> Array<BlindDateMember> in
            let members: Array<BlindDateMember> = try list.map { data in
                let member: BlindDateMember = try LeanCloudBlindDateModelProxy.from(object: data, room: room)
                member.isManager = member.user.id == room.anchor.id
                return member
            }
            return members
        }
    }
    
    func getCoverSpeakers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return Database.query(className: BlindDateMember.TABLE) { (query) in
            let room = LCObject(className: BlindDateRoom.TABLE, objectId: room.id)
            try query.where(BlindDateMember.ROOM, .equalTo(room))
            try query.where(BlindDateMember.ROLE, .notEqualTo(BlindDateRoomRole.listener.rawValue))
            query.limit = 3
            try query.where(BlindDateMember.USER, .included)
        } transform: { (list) -> Array<BlindDateMember> in
            let members: Array<BlindDateMember> = try list.map { data in
                let member = try LeanCloudBlindDateModelProxy.from(object: data, room: room)
                member.isManager = member.user.id == room.anchor.id
                return member
            }
            return members
        }
    }
    
    func subscribeMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return Database.subscribe(className: BlindDateMember.TABLE) { query in
            let room = LCObject(className: BlindDateRoom.TABLE, objectId: room.id)
            try query.where(BlindDateMember.ROOM, .equalTo(room))
        } onEvent: { (event) -> Bool in
            return true
        }
        .startWith(Result(success: true))
        .flatMap { [unowned self] result -> Observable<Result<Array<BlindDateMember>>> in
            return result.onSuccess { self.getMembers(room: room) }
        }
    }
    
    func join(member: BlindDateMember, streamId: UInt) -> Observable<Result<Void>> {
        member.streamId = streamId
        return Database.delete(className: BlindDateMember.TABLE) { query in
            let user = LCObject(className: User.TABLE, objectId: member.user.id)
            try query.where(BlindDateMember.USER, .equalTo(user))
        }
        .concatMap { result -> Observable<Result<Void>> in
            if (result.success) {
                return Database.save {
                    return try LeanCloudBlindDateModelProxy.toObject(member: member)
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
        return Database.save {
            Logger.log(message: "save mute \(mute)", level: .info)
            let object = LCObject(className: BlindDateMember.TABLE, objectId: member.id)
            try object.set(BlindDateMember.MUTED, value: mute ? 1 : 0)
            return object
        }
        .map { $0.transform() }
    }

    func selfMute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>> {
        return Database.save {
            Logger.log(message: "save selfMute \(mute)", level: .info)
            let object = LCObject(className: BlindDateMember.TABLE, objectId: member.id)
            try object.set(BlindDateMember.SELF_MUTED, value: mute ? 1 : 0)
            return object
        }
        .map { $0.transform() }
    }
    
    func asListener(member: BlindDateMember) -> Observable<Result<Void>> {
        return Database.save {
            Logger.log(message: "save asListener", level: .info)
            let object = LCObject(className: BlindDateMember.TABLE, objectId: member.id)
            try object.set(BlindDateMember.ROLE, value: BlindDateRoomRole.listener.rawValue)
            return object
        }
        .map { $0.transform() }
    }
    
    func asLeftSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>> {
        return Database.save {
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let object = LCObject(className: BlindDateMember.TABLE, objectId: member.id)
            try object.set(BlindDateMember.ROLE, value: agree ? BlindDateRoomRole.leftSpeaker.rawValue : BlindDateRoomRole.listener.rawValue)
            if (agree) {
                try object.set(BlindDateMember.MUTED, value: 0)
                try object.set(BlindDateMember.SELF_MUTED, value: 0)
            }
            return object
        }
        .map { $0.transform() }
    }
    
    func asRightSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>> {
        return Database.save {
            Logger.log(message: "save asRightSpeaker \(agree)", level: .info)
            let object = LCObject(className: BlindDateMember.TABLE, objectId: member.id)
            try object.set(BlindDateMember.ROLE, value: agree ? BlindDateRoomRole.rightSpeaker.rawValue : BlindDateRoomRole.listener.rawValue)
            if (agree) {
                try object.set(BlindDateMember.MUTED, value: 0)
                try object.set(BlindDateMember.SELF_MUTED, value: 0)
            }
            return object
        }
        .map { $0.transform() }
    }
    
    func leave(member: BlindDateMember) -> Observable<Result<Void>> {
        Logger.log(message: "Member leave isManager:\(member.isManager)", level: .info)
        if (member.isManager) {
            return Observable.zip(
                self.delete(room: member.room),
                Database.delete(className: BlindDateMember.TABLE) { query in
                    let room = LCObject(className: BlindDateRoom.TABLE, objectId: member.room.id)
                    try query.where(BlindDateMember.ROOM, .equalTo(room))
                },
                Database.delete(className: BlindDateAction.TABLE) { query in
                    let room = LCObject(className: BlindDateRoom.TABLE, objectId: member.room.id)
                    try query.where(BlindDateAction.ROOM, .equalTo(room))
                }
            ).map { (args) in
                let (result0, result1, result2) = args
                if (result0.success && result1.success && result2.success) {
                    return result0
                } else {
                    return result0.success ? result1.success ? result2 : result1 : result0
                }
            }
        } else {
            return Database.delete(className: BlindDateMember.TABLE) { query in
                let user = LCObject(className: User.TABLE, objectId: member.user.id)
                try query.where(BlindDateMember.USER, .equalTo(user))
            }
        }
    }
    
    func subscribeActions(member: BlindDateMember) -> Observable<Result<BlindDateAction>> {
        return Database.subscribe(className: BlindDateAction.TABLE) { query in
            let room = LCObject(className: BlindDateRoom.TABLE, objectId: member.room.id)
            try query.where(BlindDateAction.ROOM, .equalTo(room))
//            try query.where(Action.ROOM, .included)
//            try query.where(Action.MEMBER, .included)
            if (!member.isManager) {
                let member = LCObject(className: BlindDateMember.TABLE, objectId: member.id)
                try query.where(BlindDateAction.MEMBER, .equalTo(member))
            }
        } onEvent: { (event) -> String? in
            switch event {
            case .create(object: let object):
//                Logger.log(message: object.jsonString, level: .info)
                return object.objectId!.stringValue!
            default:
                return nil
            }
        }
        .filter { result in
            return result.data != nil
        }
        .concatMap { result in
            return LeanCloudBlindDateModelProxy.getBlindDateAction(objectId: result.data!)
        }
    }
    
    func handsup(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .handsUp)
        return Database.save {
            return try LeanCloudBlindDateModelProxy.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
    
    func requestLeft(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .requestLeft)
        return Database.save {
            return try LeanCloudBlindDateModelProxy.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
    
    func requestRight(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .requestRight)
        return Database.save {
            return try LeanCloudBlindDateModelProxy.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
    
    func inviteSpeaker(master: BlindDateMember, member: BlindDateMember) -> Observable<Result<Void>> {
        let action = master.action(with: .invite)
        action.member = member
        return Database.save {
            return try LeanCloudBlindDateModelProxy.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
    
    func rejectInvition(member: BlindDateMember) -> Observable<Result<Void>> {
        let action = member.action(with: .invite)
        action.status = .refuse
        return Database.save {
            return try LeanCloudBlindDateModelProxy.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
}

#endif
