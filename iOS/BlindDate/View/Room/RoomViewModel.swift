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
    var hoster: BlindDateMember?
    var leftSpeaker: BlindDateMember?
    var rightSpeaker: BlindDateMember?
    
    func sync(list: Array<BlindDateMember>) {
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
    private var first = true
    var list: Array<BlindDateMember>
    
    init() {
        self.list = []
    }
    
    func sync(list: Array<BlindDateMember>) -> (Bool, Array<BlindDateMember>) {
        let add = first ? [] : list.filter { member -> Bool in
            return self.list.count == 0 || self.list.first(where: { _member -> Bool in
                _member.id == member.id
            }) == nil
        }
        first = false
        var changed = false
        if (list.count != self.list.count) {
            changed = true
        } else if (list.count != 0) {
            for index in 0...list.count - 1 {
                if (list[index].id != self.list[index].id) {
                    changed = true
                    break
                }
            }
        }
        if (changed) {
            self.list.removeAll()
            self.list.append(contentsOf: list)
        }

        return (changed, add)
    }
}

class RoomChat {
    var member: BlindDateMember
    var message: String
    
    init(member: BlindDateMember, message: String) {
        self.message = message
        self.member = member
    }
}

class RoomViewModel {
    
    var room: BlindDateRoom {
        return member.room
    }
    
    var isSpeaker: Bool {
        return member.role != .listener
    }
    
    var isManager: Bool {
        return member.isManager
    }
    
    var role: BlindDateRoomRole {
        return member.role
    }
    
    var account: User {
        return Server.shared().account!
    }
    
    var member: BlindDateMember {
        return Server.shared().member!
    }
    
    var roomManager: BlindDateMember? = nil
    
    //var count: Int = 0
    //var speakersCount: Int = 0
    
    var topListeners: [Int: User?] = [0: nil, 1: nil, 2: nil]
    var onTopListenersChange: BehaviorRelay<Bool> = BehaviorRelay(value: false)
    //var memberList: [Any] = []
    var speakers: SpeakerGroup = SpeakerGroup()
    
    var onListenersListChange: BehaviorRelay<Bool> = BehaviorRelay(value: false)
    var listeners: MemberGroup = MemberGroup()
    
    var handsupList: [BlindDateAction] = []
    var onHandsupListChange: BehaviorRelay<[BlindDateAction]> = BehaviorRelay(value: [])

    var onMemberEnter: BehaviorRelay<BlindDateMember?> = BehaviorRelay(value: nil)
    
    var messageList: [RoomChat] = []
    
    func actionsSource() -> Observable<Result<BlindDateAction>> {
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
    
    func sendMessage(message: String) -> Observable<Result<Void>> {
        return Server.shared().sendMessage(message: message)
    }
    
    func subscribeMessages() -> Observable<Result<BlindDateMessage>> {
        return Server.shared().subscribeMessages()
            .map { [unowned self] result in
                if let message = result.data {
                    let member: BlindDateMember?
                    if (message.userId == self.speakers.hoster?.id) {
                        member = self.speakers.hoster
                    } else if (message.userId == self.speakers.leftSpeaker?.id) {
                        member = self.speakers.leftSpeaker
                    } else if (message.userId == self.speakers.rightSpeaker?.id) {
                        member = self.speakers.rightSpeaker
                    } else {
                        member = self.listeners.list.first { member -> Bool in
                            member.id == message.userId
                        }
                    }
                    if let member = member {
                        self.messageList.append(RoomChat(member: member, message: message.value))
                    }
                }
                return result
            }
    }
    
    func roomMembersDataSource() -> Observable<Result<Bool>> {
        return Server.shared().subscribeMembers()
            .map { [unowned self] result in
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
                        let (changed, add) = self.listeners.sync(list: list.filter { user in
                            return user.role == .listener
                        })
                        add.filter { member -> Bool in
                            member.id != self.speakers.hoster?.id &&
                            member.id != self.speakers.leftSpeaker?.id &&
                            member.id != self.speakers.rightSpeaker?.id
                        }
                        .forEach { member in
                            self.onMemberEnter.accept(member)
                        }
                        if (changed) {
                            self.onListenersListChange.accept(true)
                        }
                        checkTopListeners()
                        
                        self.speakers.sync(list: list.filter { user in
                            return user.role != .listener
                        })
                    } else {
                        self.roomManager = nil
                    }
                }
                return Result(success: result.success, data: roomClosed, message: result.message)
            }
    }
    
    private func checkTopListeners() {
        var changed = false
        var tops: [Int: User?] = [0: nil, 1: nil, 2: nil]
        let max = min(self.listeners.list.count, 3)
        if (max > 0) {
            for index in 0...(max - 1) {
                tops[index] = self.listeners.list[index].user
            }
        }
        if (tops.count != 0 && tops.count == self.topListeners.count) {
            for index in 0...2 {
                let a = tops[index]
                let b = self.topListeners[index]
                if (a == nil && b == nil) {
                    continue
                } else if (a??.id != b??.id || a??.avatar != b??.avatar) {
                    changed = true
                    break
                }
            }
        } else {
            changed = true
        }
        if (changed) {
            for index in 0...2 {
                self.topListeners[index] = tops[index]
            }
            self.onTopListenersChange.accept(true)
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
    
    let isMuted: BehaviorRelay<Bool> = BehaviorRelay<Bool>(value: Server.shared().isMicrophoneClose())
    let isEnableBeauty: BehaviorRelay<Bool> = BehaviorRelay<Bool>(value: Server.shared().isEnableBeauty())
    
    func muted() -> Bool {
        return Server.shared().isMicrophoneClose()
    }
    
    func enabledBeauty() -> Bool {
        return Server.shared().isEnableBeauty()
    }
    
    func enableBeauty(enable: Bool) {
        isEnableBeauty.accept(enable)
        Server.shared().enableBeauty(enable: enable)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>>{
        isMuted.accept(mute)
        return Server.shared().closeMicrophone(close: mute)
    }
    
    func handsup(left: Bool?) -> Observable<Result<Void>>{
        return Server.shared().handsUp(left: left)
    }
    
    func inviteSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return Server.shared().inviteSpeaker(member: member)
    }
    
    func muteSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return Server.shared().muteSpeaker(member: member)
    }
    
    func unMuteSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return Server.shared().unMuteSpeaker(member: member)
    }
    
    func kickSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return Server.shared().kickSpeaker(member: member)
    }
    
    func process(action: BlindDateAction, agree: Bool) -> Observable<Result<Void>> {
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
