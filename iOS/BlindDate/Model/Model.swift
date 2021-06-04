//
//  Model.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import RxSwift
import Core

public class BlindDateRoom: Equatable {
    public static func == (lhs: BlindDateRoom, rhs: BlindDateRoom) -> Bool {
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

public enum BlindDateRoomRole: Int {
    case listener = 0
    case manager = 1
    case leftSpeaker = 2
    case rightSpeaker = 3
}

public class BlindDateMember {
    public var id: String
    public var isMuted: Bool
    public var isSelfMuted: Bool
    public var role: BlindDateRoomRole = .listener
    //var isSpeaker: Bool = false
    public var room: BlindDateRoom
    public var streamId: UInt
    public var user: User
    
    public var isManager: Bool = false
    public var isLocal: Bool = false
    
    public init(id: String, isMuted: Bool, isSelfMuted: Bool, role: BlindDateRoomRole, room: BlindDateRoom, streamId: UInt, user: User) {
        self.id = id
        self.isMuted = isMuted
        self.isSelfMuted = isSelfMuted
        self.role = role
        self.room = room
        self.streamId = streamId
        self.user = user
        self.isManager = room.anchor.id == id
    }
    
    public func isSpeaker() -> Bool {
        return isManager || role != .listener
    }
    
    public func action(with action: BlindDateActionType) -> BlindDateAction {
        return BlindDateAction(id: "", action: action, status: .ing, member: self, room: self.room)
    }
}

public enum BlindDateActionType: Int {
    case handsUp = 1
    case invite = 2
    case requestLeft = 3
    case requestRight = 4
    case error
    
    public static func from(value: Int) -> BlindDateActionType {
        switch value {
        case 1:
            return .handsUp
        case 2:
            return .invite
        case 3:
            return .requestLeft
        case 4:
            return .requestRight
        default:
            return .error
        }
    }
}

public enum BlindDateActionStatus: Int {
    case ing = 1
    case agree = 2
    case refuse = 3
    case error
    
    public static func from(value: Int) -> BlindDateActionStatus {
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

public class BlindDateAction {
    public var id: String
    public var action: BlindDateActionType
    public var status: BlindDateActionStatus
    
    public var member: BlindDateMember
    public var room: BlindDateRoom
    
    public init(id: String, action: BlindDateActionType, status: BlindDateActionStatus, member: BlindDateMember, room: BlindDateRoom) {
        self.id = id
        self.action = action
        self.status = status
        self.member = member
        self.room = room
    }
}

public class BlindDateMessage {
    public var channelId: String
    public var userId: String
    public var value: String
    
    public init(channelId: String, userId: String, value: String) {
        self.channelId = channelId
        self.userId = userId
        self.value = value
    }
}


extension BlindDateRoom {
    private static var manager: IBlindDateModelManager {
        InjectionService.shared.resolve(IBlindDateModelManager.self)
    }
    
    static func create(room: BlindDateRoom) -> Observable<Result<String>> {
        return BlindDateRoom.manager.create(room: room)
    }
    
    func delete() -> Observable<Result<Void>> {
        return BlindDateRoom.manager.delete(room: self)
    }
    
    static func getRooms() -> Observable<Result<Array<BlindDateRoom>>> {
        return BlindDateRoom.manager.getRooms()
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>> {
        return BlindDateRoom.manager.getRoom(by: objectId)
    }
    
    static func update(room: BlindDateRoom) -> Observable<Result<String>> {
        return BlindDateRoom.manager.update(room: room)
    }
    
    func getMembers() -> Observable<Result<Array<BlindDateMember>>> {
        return BlindDateRoom.manager.getMembers(room: self)
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<BlindDateMember>>> {
        return BlindDateRoom.manager.getCoverSpeakers(room: self)
    }
    
    func subscribeMembers() -> Observable<Result<Array<BlindDateMember>>> {
        return BlindDateRoom.manager.subscribeMembers(room: self)
    }
}

extension BlindDateMember {
    private static var manager: IBlindDateModelManager {
        InjectionService.shared.resolve(IBlindDateModelManager.self)
    }
    
    func join(streamId: UInt) -> Observable<Result<Void>>{
        return BlindDateMember.manager.join(member: self, streamId: streamId)
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return BlindDateMember.manager.mute(member: self, mute: mute)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return BlindDateMember.manager.selfMute(member: self, mute: mute)
    }
    
    func asListener() -> Observable<Result<Void>> {
        return BlindDateMember.manager.asListener(member: self)
    }
    
    func asLeftSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return BlindDateMember.manager.asLeftSpeaker(member: self, agree: agree)
    }
    
    func asRightSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return BlindDateMember.manager.asRightSpeaker(member: self, agree: agree)
    }
    
    func leave() -> Observable<Result<Void>> {
        return BlindDateMember.manager.leave(member: self)
    }
    
    func subscribeActions() -> Observable<Result<BlindDateAction>> {
        return BlindDateMember.manager.subscribeActions(member: self)
    }
    
    func handsup() -> Observable<Result<Void>> {
        return BlindDateMember.manager.handsup(member: self)
    }
    
    func requestLeft() -> Observable<Result<Void>> {
        return BlindDateMember.manager.requestLeft(member: self)
    }
    
    func requestRight() -> Observable<Result<Void>> {
        return BlindDateMember.manager.requestRight(member: self)
    }
    
    func inviteSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return BlindDateMember.manager.inviteSpeaker(master: self, member: member)
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        return BlindDateMember.manager.rejectInvition(member: self)
    }
}

extension BlindDateAction {
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
