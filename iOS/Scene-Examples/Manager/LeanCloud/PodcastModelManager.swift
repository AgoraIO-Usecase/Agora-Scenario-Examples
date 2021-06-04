//
//  PodcastModelManagerProxy.swift
//  Core
//
//  Created by XC on 2021/6/1.
//
#if LEANCLOUD
import Foundation
import Core
import LeanCloud
import RxSwift
import InteractivePodcast

class LeanCloudPodcastModelManager: IPodcastModelManager {
    
    static func queryMemberCount(room: LCObject) throws -> Int {
        let query = LCQuery(className: PodcastMember.TABLE)
        try query.where(PodcastMember.ROOM, .equalTo(room))
        return query.count().intValue
    }
    
    static func querySpeakerCount(room: LCObject) throws -> Int {
        let query = LCQuery(className: PodcastMember.TABLE)
        try query.where(PodcastMember.ROOM, .equalTo(room))
        try query.where(PodcastMember.IS_SPEAKER, .equalTo(1))
        return query.count().intValue
    }
    
    static func from(object: LCObject) throws -> PodcastRoom {
        let objectId: String = object.objectId!.stringValue!
        let channelName: String = object.get(PodcastRoom.CHANNEL_NAME)!.stringValue!
        let anchor: User = try LeanCloudUserManager.from(object: object.get(PodcastRoom.ANCHOR_ID) as! LCObject)
        let room = PodcastRoom(id: objectId, channelName: channelName, anchor: anchor)
        room.total = try queryMemberCount(room: object)
        room.speakersTotal = try querySpeakerCount(room: object)
        if (room.coverCharacters.count == 0) {
            room.coverCharacters.append(room.anchor)
        }
        return room
    }
    
    static func from(object: LCObject, room: PodcastRoom) throws -> PodcastMember {
        let userObject = object.get(PodcastMember.USER) as! LCObject
        let user = try LeanCloudUserManager.from(object: userObject)
        let isMuted = (object.get(PodcastMember.MUTED)?.intValue ?? 0) == 1
        let isSpeaker = (object.get(PodcastMember.IS_SPEAKER)?.intValue ?? 0) == 1
        let isSelfMuted = (object.get(PodcastMember.SELF_MUTED)?.intValue ?? 0) == 1
        let streamId = object.get(PodcastMember.STREAM_ID)?.uintValue ?? 0
        let id = object.objectId!.stringValue!
        return PodcastMember(id: id, isMuted: isMuted, isSelfMuted: isSelfMuted, isSpeaker: isSpeaker, room: room, streamId: streamId, user: user)
    }
    
    static func toObject(member: PodcastMember) throws -> LCObject {
        let object = LCObject(className: PodcastMember.TABLE)
        try object.set(PodcastMember.ROOM, value: LCObject(className: PodcastRoom.TABLE, objectId: member.room.id))
        try object.set(PodcastMember.USER, value: LCObject(className: User.TABLE, objectId: member.user.id))
        try object.set(PodcastMember.STREAM_ID, value: member.streamId)
        try object.set(PodcastMember.IS_SPEAKER, value: member.isSpeaker ? 1 : 0)
        try object.set(PodcastMember.MUTED, value: member.isMuted ? 1 : 0)
        try object.set(PodcastMember.SELF_MUTED, value: member.isSelfMuted ? 1 : 0)
        return object
    }
    
    static func toActionObject(action: PodcastAction) throws -> LCObject {
        let object = LCObject(className: PodcastAction.TABLE)
        try object.set(PodcastAction.ROOM, value: LCObject(className: PodcastRoom.TABLE, objectId: action.room.id))
        try object.set(PodcastAction.MEMBER, value: LCObject(className: PodcastMember.TABLE, objectId: action.member.id))
        try object.set(PodcastAction.ACTION, value: action.action.rawValue)
        try object.set(PodcastAction.STATUS, value: action.status.rawValue)
        return object
    }
    
    static func from(object: LCObject) throws -> PodcastAction {
        let id = object.objectId!.stringValue!
        let room: PodcastRoom = try from(object: object.get(PodcastAction.ROOM) as! LCObject)
        let member: PodcastMember = try from(object: object.get(PodcastAction.MEMBER) as! LCObject, room: room)
        let action: Int = object.get(PodcastAction.ACTION)!.intValue!
        let status: Int = object.get(PodcastAction.STATUS)!.intValue!
        
        return PodcastAction(id: id, action: PodcastActionType.from(value: action), status: PodcastActionStatus.from(value: status), member: member, room: room)
    }
    
    static func getPodcastAction(objectId: String) -> Observable<Result<PodcastAction>> {
        return Database.query(className: PodcastAction.TABLE, objectId: objectId) { (query) in
            try query.where(PodcastAction.ROOM, .included)
            try query.where("\(PodcastAction.ROOM).\(PodcastRoom.ANCHOR_ID)", .included)
            try query.where(PodcastAction.MEMBER, .included)
            try query.where("\(PodcastAction.MEMBER).\(PodcastMember.USER)", .included)
        } transform: { (object: LCObject) -> PodcastAction in
            return try from(object: object)
        }
    }
    
    func create(room: PodcastRoom) -> Observable<Result<String>> {
        return Database.save {
            let object = LCObject(className: PodcastRoom.TABLE)
            let anchor = LCObject(className: User.TABLE, objectId: room.anchor.id)
            try object.set(PodcastRoom.CHANNEL_NAME, value: room.channelName)
            try object.set(PodcastRoom.ANCHOR_ID, value: anchor)
            return object
        }
    }
    
    func delete(room: PodcastRoom) -> Observable<Result<Void>> {
        return Database.delete(className: PodcastRoom.TABLE, objectId: room.id)
    }
    
    func getRooms() -> Observable<Result<Array<PodcastRoom>>> {
        return Database.query(className: PodcastRoom.TABLE) { (query) in
            try query.where(PodcastRoom.CHANNEL_NAME, .selected)
            try query.where(PodcastRoom.ANCHOR_ID, .selected)
            try query.where(PodcastRoom.ANCHOR_ID, .included)
            try query.where("createdAt", .descending)
        } transform: { (list) -> Array<PodcastRoom> in
            let rooms: Array<PodcastRoom> = try list.map { room in
                return try LeanCloudPodcastModelManager.from(object: room)
            }
            return rooms
        }
    }
    
    func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>> {
        return Database.query(className: PodcastRoom.TABLE, objectId: objectId) { query in
            try query.where(PodcastRoom.ANCHOR_ID, .included)
        } transform: { (data: LCObject) -> PodcastRoom in
            let channelName: String = data.get(PodcastRoom.CHANNEL_NAME)!.stringValue!
            let anchor = try LeanCloudUserManager.from(object: data.get(PodcastRoom.ANCHOR_ID) as! LCObject)
            return PodcastRoom(id: objectId, channelName: channelName, anchor: anchor)
        }
    }
    
    func update(room: PodcastRoom) -> Observable<Result<String>> {
        return Database.save {
            let object = LCObject(className: PodcastRoom.TABLE, objectId: room.id)
            try object.set(PodcastRoom.CHANNEL_NAME, value: room.channelName)
            try object.set(PodcastRoom.ANCHOR_ID, value: LCObject(className: User.TABLE, objectId: room.anchor.id))
            return object
        }
    }
    
    func getMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return Database.query(className: PodcastMember.TABLE) { (query) in
            let room = LCObject(className: PodcastRoom.TABLE, objectId: room.id)
            try query.where(PodcastMember.ROOM, .equalTo(room))
            try query.where(PodcastMember.USER, .included)
            try query.where("createdAt", .ascending)
        } transform: { (list) -> Array<PodcastMember> in
            let members: Array<PodcastMember> = try list.map { data in
                let member: PodcastMember = try LeanCloudPodcastModelManager.from(object: data, room: room)
                member.isManager = member.user.id == room.anchor.id
                return member
            }
            return members
        }
    }
    
    func getCoverSpeakers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return Database.query(className: PodcastMember.TABLE) { (query) in
            let room = LCObject(className: PodcastRoom.TABLE, objectId: room.id)
            try query.where(PodcastMember.ROOM, .equalTo(room))
            try query.where(PodcastMember.IS_SPEAKER, .equalTo(1))
            query.limit = 3
            try query.where(PodcastMember.USER, .included)
        } transform: { (list) -> Array<PodcastMember> in
            let members: Array<PodcastMember> = try list.map { data in
                let member: PodcastMember = try LeanCloudPodcastModelManager.from(object: data, room: room)
                member.isManager = member.user.id == room.anchor.id
                return member
            }
            return members
        }
    }
    
    func subscribeMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return Database.subscribe(className: PodcastMember.TABLE) { query in
            let room = LCObject(className: PodcastRoom.TABLE, objectId: room.id)
            try query.where(PodcastMember.ROOM, .equalTo(room))
        } onEvent: { (event) -> Bool in
            return true
        }
        //.startWith(Result(success: true))
        .flatMap { [unowned self] result -> Observable<Result<Array<PodcastMember>>> in
            return result.onSuccess { self.getMembers(room: room) }
        }
    }
    
    func join(member: PodcastMember, streamId: UInt) -> Observable<Result<Void>> {
        member.streamId = streamId
        return Database.delete(className: PodcastMember.TABLE) { query in
            let user = LCObject(className: User.TABLE, objectId: member.user.id)
            try query.where(PodcastMember.USER, .equalTo(user))
        }
        .concatMap { result -> Observable<Result<Void>> in
            if (result.success) {
                return Database.save {
                    return try LeanCloudPodcastModelManager.toObject(member: member)
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
        return Database.save {
            Logger.log(message: "save mute \(mute)", level: .info)
            let object = LCObject(className: PodcastMember.TABLE, objectId: member.id)
            try object.set(PodcastMember.MUTED, value: mute ? 1 : 0)
            return object
        }
        .map { $0.transform() }
    }
    
    func selfMute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>> {
        return Database.save {
            Logger.log(message: "save selfMute \(mute)", level: .info)
            let object = LCObject(className: PodcastMember.TABLE, objectId: member.id)
            try object.set(PodcastMember.SELF_MUTED, value: mute ? 1 : 0)
            return object
        }
        .map { $0.transform() }
    }
    
    func asSpeaker(member: PodcastMember, agree: Bool) -> Observable<Result<Void>> {
        return Database.save {
            Logger.log(message: "save asSpeaker \(agree)", level: .info)
            let object = LCObject(className: PodcastMember.TABLE, objectId: member.id)
            try object.set(PodcastMember.IS_SPEAKER, value: agree ? 1 : 0)
            if (agree) {
                try object.set(PodcastMember.MUTED, value: 0)
                try object.set(PodcastMember.SELF_MUTED, value: 0)
            }
            return object
        }
        .map { $0.transform() }
    }
    
    func leave(member: PodcastMember) -> Observable<Result<Void>> {
        Logger.log(message: "Member leave isManager:\(member.isManager)", level: .info)
        if (member.isManager) {
            return Observable.zip(
                self.delete(room: member.room),
                Database.delete(className: PodcastMember.TABLE) { query in
                    let room = LCObject(className: PodcastRoom.TABLE, objectId: member.room.id)
                    try query.where(PodcastMember.ROOM, .equalTo(room))
                },
                Database.delete(className: PodcastAction.TABLE) { query in
                    let room = LCObject(className: PodcastRoom.TABLE, objectId: member.room.id)
                    try query.where(PodcastAction.ROOM, .equalTo(room))
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
            return Database.delete(className: PodcastMember.TABLE) { query in
                let user = LCObject(className: User.TABLE, objectId: member.user.id)
                try query.where(PodcastMember.USER, .equalTo(user))
            }
        }
    }
    
    func subscribeActions(member: PodcastMember) -> Observable<Result<PodcastAction>> {
        return Database.subscribe(className: PodcastAction.TABLE) { query in
            let room = LCObject(className: PodcastRoom.TABLE, objectId: member.room.id)
            try query.where(PodcastAction.ROOM, .equalTo(room))
//            try query.where(Action.ROOM, .included)
//            try query.where(Action.MEMBER, .included)
            if (!member.isManager) {
                let member = LCObject(className: PodcastMember.TABLE, objectId: member.id)
                try query.where(PodcastAction.MEMBER, .equalTo(member))
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
            return LeanCloudPodcastModelManager.getPodcastAction(objectId: result.data!)
        }
    }
    
    func handsup(member: PodcastMember) -> Observable<Result<Void>> {
        let action = member.action(with: .handsUp)
        return Database.save {
            return try LeanCloudPodcastModelManager.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
    
    func inviteSpeaker(master: PodcastMember, member: PodcastMember) -> Observable<Result<Void>> {
        let action = master.action(with: .invite)
        action.member = member
        return Database.save {
            return try LeanCloudPodcastModelManager.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
    
    func rejectInvition(member: PodcastMember) -> Observable<Result<Void>> {
        let action = member.action(with: .invite)
        action.status = .refuse
        return Database.save {
            return try LeanCloudPodcastModelManager.toActionObject(action: action)
        }
        .map { $0.transform() }
    }
}

#endif
