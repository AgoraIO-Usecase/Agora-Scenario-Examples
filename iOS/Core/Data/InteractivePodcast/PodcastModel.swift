//
//  PodcastModel.swift
//  Core
//
//  Created by XC on 2021/6/1.
//

import Foundation

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
