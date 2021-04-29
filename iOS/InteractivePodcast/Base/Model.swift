//
//  Model.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/7.
//

import Foundation
import Core

class Room: Equatable {
    
    static func == (lhs: Room, rhs: Room) -> Bool {
        return lhs.id == rhs.id
    }
    
    var id: String
    let channelName: String
    var anchor: User
    var total: Int = 0
    var speakersTotal: Int = 0
    var coverCharacters: [User] = []
    
    init(id: String, channelName: String, anchor: User) {
        self.id = id
        self.channelName = channelName
        self.anchor = anchor
    }
}

class Member {
    var id: String
    var isMuted: Bool
    var isSelfMuted: Bool
    var isSpeaker: Bool = false
    var room: Room
    var streamId: UInt
    var user: User
    
    var isManager: Bool = false
    
    init(id: String, isMuted: Bool, isSelfMuted: Bool, isSpeaker: Bool, room: Room, streamId: UInt, user: User) {
        self.id = id
        self.isMuted = isMuted
        self.isSelfMuted = isSelfMuted
        self.isSpeaker = isSpeaker
        self.room = room
        self.streamId = streamId
        self.user = user
        self.isManager = room.anchor.id == id
    }
}

enum ActionType: Int {
    case handsUp = 1
    case invite = 2
    case error
    
    static func from(value: Int) -> ActionType {
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

enum ActionStatus: Int {
    case ing = 1
    case agree = 2
    case refuse = 3
    case error
    
    static func from(value: Int) -> ActionStatus {
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

class Action {
    var id: String
    var action: ActionType
    var status: ActionStatus
    
    var member: Member
    var room: Room
    
    init(id: String, action: ActionType, status: ActionStatus, member: Member, room: Room) {
        self.id = id
        self.action = action
        self.status = status
        self.member = member
        self.room = room
    }
}
