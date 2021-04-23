//
//  RoomViewModel.swift
//  BlindDate
//
//  Created by XC on 2021/4/22.
//

import Foundation
import RxSwift
import IGListKit
import RxRelay
import RxCocoa
import Core

enum LeaveRoomAction {
    case closeRoom
    case leave
    //case mini
}

class SpeakerGroup {
    var hoster: Member?
    var leftSpeaker: Member?
    var rightSpeaker: Member?
    
    func sync(list: Array<Member>) {
        hoster = list.first { member in
            member.role == .manager
        }
        leftSpeaker = list.first { member in
            member.role == .leftSpeaker
        }
        rightSpeaker = list.first { member in
            member.role == .rightSpeaker
        }
    }
}

class MemberGroup {
    var list: Array<Member>
    
    init() {
        self.list = []
    }
    
    func sync(list: Array<Member>) -> Bool {
        self.list.removeAll()
        self.list.append(contentsOf: list)
        
        return true
    }
}

class RoomViewModel {
    
    var room: Room {
        return member.room
    }
    
    var isSpeaker: Bool {
        return member.role != .listener
    }
    
    var isManager: Bool {
        return member.isManager
    }
    
    var role: RoomRole {
        return member.role
    }
    
    var account: User {
        return Server.shared().account!
    }
    
    var member: Member {
        return Server.shared().member!
    }
    
    var roomManager: Member? = nil
    
    //var count: Int = 0
    //var speakersCount: Int = 0
    
    var topListeners: [User] = []
    var onTopListenersChange: BehaviorRelay<[User]> = BehaviorRelay(value: [])
    //var memberList: [Any] = []
    var speakers: SpeakerGroup = SpeakerGroup()
    
    var onListenersListChange: BehaviorRelay<Bool> = BehaviorRelay(value: false)
    var listeners: MemberGroup = MemberGroup()
    
    var handsupList: [Action] = []
    var onHandsupListChange: BehaviorRelay<[Action]> = BehaviorRelay(value: [])
    
    func actionsSource() -> Observable<Result<Action>> {
        return Server.shared().subscribeActions()
            .map { [unowned self] result in
                if let action = result.data {
                    if ((action.action == .handsUp || action.action == .requestLeft || action.action == .requestRight) &&
                            self.handsupList.first { item in
                        return item.member.id == action.member.id
                    } == nil) {
                        handsupList.append(action)
                        onHandsupListChange.accept(handsupList)
                    }
                }
                return result
            }
    }
    
    func roomMembersDataSource() -> Observable<Result<Bool>> {
        return Server.shared().subscribeMembers()
            .map { [unowned self] result in
                Logger.log(message: "member isMuted:\(member.isMuted) member:\(member.isSelfMuted)", level: .info)
                var roomClosed = false
                if (result.success) {
                    syncLocalUIStatus()
                    if let list = result.data {
                        if (list.count == 0) {
                            roomClosed = true
                            self.roomManager = nil
                        } else {
                            roomManager = list.first { member in
                                return member.isManager
                            }
                        }
                        self.speakers.sync(list: list.filter { user in
                            return user.role != .listener
                        })
                        let changed = self.listeners.sync(list: list.filter { user in
                            return user.role == .listener
                        })
                        if (changed) {
                            self.onListenersListChange.accept(true)
                        }
                        checkTopListeners()
                    } else {
                        self.roomManager = nil
                    }
                }
                return Result(success: result.success, data: roomClosed, message: result.message)
            }
    }
    
    private func checkTopListeners() {
        var changed = false
        var tops: [User] = []
        let max = min(self.listeners.list.count, 3)
        if (max > 0) {
            for index in 0...(max - 1) {
                tops.append(self.listeners.list[index].user)
            }
        }
        if (tops.count != 0 && tops.count == self.topListeners.count) {
            for index in 0...tops.count - 1 {
                let a = tops[index]
                let b = self.topListeners[index]
                if (a.avatar != b.avatar) {
                    changed = true
                    break
                }
            }
        } else {
            changed = true
        }
        if (changed) {
            self.topListeners.removeAll()
            self.topListeners.append(contentsOf: tops)
            self.onTopListenersChange.accept(self.topListeners)
        }
    }
    
    func leaveRoom(action: LeaveRoomAction) -> Observable<Result<Void>> {
        Logger.log(message: "RoomViewModel leaveRoom action:\(action)", level: .info)
        switch action {
        case .leave:
            return Server.shared().leave()
        case .closeRoom:
            return Server.shared().leave()
        }
    }
    
    let isMuted: PublishRelay<Bool> = PublishRelay<Bool>()
    
    func muted() -> Bool {
        return Server.shared().isMicrophoneClose()
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>>{
        isMuted.accept(mute)
        return Server.shared().closeMicrophone(close: mute)
    }
    
    func handsup(left: Bool?) -> Observable<Result<Void>>{
        return Server.shared().handsUp(left: left)
    }
    
    func inviteSpeaker(member: Member) -> Observable<Result<Void>> {
        return Server.shared().inviteSpeaker(member: member)
    }
    
    func muteSpeaker(member: Member) -> Observable<Result<Void>> {
        return Server.shared().muteSpeaker(member: member)
    }
    
    func unMuteSpeaker(member: Member) -> Observable<Result<Void>> {
        return Server.shared().unMuteSpeaker(member: member)
    }
    
    func kickSpeaker(member: Member) -> Observable<Result<Void>> {
        return Server.shared().kickSpeaker(member: member)
    }
    
    func process(action: Action, agree: Bool) -> Observable<Result<Void>> {
        switch action.action {
        case .handsUp, .requestLeft, .requestRight:
            if (isManager) {
                let find = handsupList.firstIndex { item in
                    return item.id == action.id
                }
                if let find = find {
                    handsupList.remove(at: find)
                }
                onHandsupListChange.accept(handsupList)
                if (self.speakers.leftSpeaker != nil && self.speakers.rightSpeaker != nil) {
                    return Observable.just(Result(success: false, message: "位置已满！"))
                } else if (action.action == .handsUp) {
                    action.action = self.speakers.leftSpeaker == nil ? .requestLeft : .requestRight
                } else if (action.action == .requestLeft) {
                    action.action = self.speakers.leftSpeaker == nil ? .requestLeft : .requestRight
                } else if (action.action == .requestRight) {
                    action.action = self.speakers.rightSpeaker == nil ? .requestRight : .requestLeft
                }
                return Server.shared().process(request: action, agree: agree)
            } else {
                return Observable.just(Result(success: true))
            }
        case .invite:
            if (!isManager && !isSpeaker) {
                if (self.speakers.leftSpeaker != nil && self.speakers.rightSpeaker != nil) {
                    return Observable.just(Result(success: false, message: "位置已满！"))
                } else if (self.speakers.leftSpeaker == nil) {
                    return Server.shared().process(invitionLeft: action, agree: agree)
                } else {
                    return Server.shared().process(invitionRight: action, agree: agree)
                }
            } else {
                return Observable.just(Result(success: true))
            }
        default:
            return Observable.just(Result(success: true))
        }
    }
    
    func syncLocalUIStatus() {
        isMuted.accept(muted())
    }
}
