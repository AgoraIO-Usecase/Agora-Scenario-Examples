//
//  BlindDateModel.swift
//  Core
//
//  Created by XC on 2021/6/1.
//

import Foundation

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
