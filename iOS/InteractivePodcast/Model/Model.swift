//
//  Model.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/7.
//

import Foundation
import RxSwift
import Core

public class PodcastRoom: Equatable {
    public static func == (lhs: PodcastRoom, rhs: PodcastRoom) -> Bool {
        return lhs.id == rhs.id
    }
    
    public var id: String
    public let channelName: String
    public var anchor: User
    public var total: Int = 0
    public var speakersTotal: Int = 0
    public var coverCharacters: [User] = []
    
    public init(id: String, channelName: String, anchor: User) {
        self.id = id
        self.channelName = channelName
        self.anchor = anchor
    }
}

public class PodcastMember {
    public var id: String
    public var isMuted: Bool
    public var isSelfMuted: Bool
    public var isSpeaker: Bool = false
    public var room: PodcastRoom
    public var streamId: UInt
    public var user: User
    
    public var isManager: Bool = false
    
    public init(id: String, isMuted: Bool, isSelfMuted: Bool, isSpeaker: Bool, room: PodcastRoom, streamId: UInt, user: User) {
        self.id = id
        self.isMuted = isMuted
        self.isSelfMuted = isSelfMuted
        self.isSpeaker = isSpeaker
        self.room = room
        self.streamId = streamId
        self.user = user
        self.isManager = room.anchor.id == user.id
    }
    
    public func action(with action: PodcastActionType) -> PodcastAction {
        return PodcastAction(id: "", action: action, status: .ing, member: self, room: self.room)
    }
}

public enum PodcastActionType: Int {
    case handsUp = 1
    case invite = 2
    case error
    
    public static func from(value: Int) -> PodcastActionType {
        switch value {
        case 1:
            return .handsUp
        case 2:
            return .invite
        default:
            return .error
        }
    }
}

public enum PodcastActionStatus: Int {
    case ing = 1
    case agree = 2
    case refuse = 3
    case error
    
    public static func from(value: Int) -> PodcastActionStatus {
        switch value {
        case 1:
            return .ing
        case 2:
            return .agree
        case 3:
            return .refuse
        default:
            return .error
        }
    }
}

public class PodcastAction {
    public var id: String
    public var action: PodcastActionType
    public var status: PodcastActionStatus
    
    public var member: PodcastMember
    public var room: PodcastRoom
    
    public init(id: String, action: PodcastActionType, status: PodcastActionStatus, member: PodcastMember, room: PodcastRoom) {
        self.id = id
        self.action = action
        self.status = status
        self.member = member
        self.room = room
    }
}

extension PodcastRoom {
    private static var manager: IPodcastModelManager {
        InjectionService.shared.resolve(IPodcastModelManager.self)
    }
    
    static func create(room: PodcastRoom) -> Observable<Result<String>> {
        return PodcastRoom.manager.create(room: room)
    }
    
    func delete() -> Observable<Result<Void>> {
        return PodcastRoom.manager.delete(room: self)
    }
    
    static func getRooms() -> Observable<Result<Array<PodcastRoom>>> {
        return PodcastRoom.manager.getRooms()
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>> {
        return PodcastRoom.manager.getRoom(by: objectId)
    }
    
    static func update(room: PodcastRoom) -> Observable<Result<String>> {
        return PodcastRoom.manager.update(room: room)
    }
    
    func getMembers() -> Observable<Result<Array<PodcastMember>>> {
        return PodcastRoom.manager.getMembers(room: self)
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<PodcastMember>>> {
        return PodcastRoom.manager.getCoverSpeakers(room: self)
    }
    
    func subscribeMembers() -> Observable<Result<Array<PodcastMember>>> {
        return PodcastRoom.manager.subscribeMembers(room: self)
    }
}

extension PodcastMember {
    private static var manager: IPodcastModelManager {
        InjectionService.shared.resolve(IPodcastModelManager.self)
    }
    
    func join(streamId: UInt) -> Observable<Result<Void>>{
        return PodcastMember.manager.join(member: self, streamId: streamId)
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return PodcastMember.manager.mute(member: self, mute: mute)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return PodcastMember.manager.selfMute(member: self, mute: mute)
    }
    
    func asSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return PodcastMember.manager.asSpeaker(member: self, agree: agree)
    }
    
    func leave() -> Observable<Result<Void>> {
        return PodcastMember.manager.leave(member: self)
    }
    
    func subscribeActions() -> Observable<Result<PodcastAction>> {
        return PodcastMember.manager.subscribeActions(member: self)
    }
    
    func handsup() -> Observable<Result<Void>> {
        return PodcastMember.manager.handsup(member: self)
    }
    
    func inviteSpeaker(member: PodcastMember) -> Observable<Result<Void>> {
        return PodcastMember.manager.inviteSpeaker(master: self, member: member)
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        return PodcastMember.manager.rejectInvition(member: self)
    }
}

extension PodcastAction {
    func setSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return member.asSpeaker(agree: agree)
    }
    
    func setInvition(agree: Bool) -> Observable<Result<Void>> {
        if (agree) {
            return member.asSpeaker(agree: agree)
        } else {
            return member.rejectInvition()
        }
    }
}
