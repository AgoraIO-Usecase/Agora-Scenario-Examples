//
//  Server.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import RxSwift
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

class Server: NSObject {
    fileprivate static let instance = Server()
    static func shared() -> Service {
        return instance
    }
    
    var account: User? = nil
    var member: Member? = nil
    var setting: LocalSetting = CoreData.getSetting() ?? LocalSetting()
    //var room: Room? = nil
    private var rtcServer: RtcServer = RtcServer()
    private var rtmServer: RtmServer = RtmServer()
    private var scheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "rtc")
}

extension Server: Service {
    func updateSetting() {
        if (rtcServer.isJoinChannel) {
            rtcServer.setClientRole(rtcServer.role!, setting.audienceLatency)
        }
    }
    
    func getAccount() -> Observable<Result<User>> {
        if (account == nil) {
            let user = CoreData.getAccount()
            if (user != nil) {
                return User.getUser(by: user!.id).map { result in
                    if (result.success) {
                        self.account = result.data!
                    } else {
                        
                    }
                    return result
                }
            } else {
                return User.randomUser().flatMap { result in
                    return result.onSuccess {
                        self.account = result.data!
                        return CoreData.saveAccount(user: result.data!)
                    }
                }
            }
        } else {
            return Observable.just(Result(success: true, data: account))
        }
    }
    
    func getRooms() -> Observable<Result<Array<Room>>> {
        return Room.getRooms()
    }
    
    func create(room: Room) -> Observable<Result<Room>> {
        if let user = account {
            room.anchor = user
            return Room.create(room: room)
                .map { result in
                    if (result.success) {
                        room.id = result.data!
                        return Result(success: true, data: room)
                    } else {
                        return Result(success: false, message: result.message)
                    }
                }
        } else {
            return Observable.just(Result(success: false, message: "account is nil!"))
        }
    }
    
    func join(room: Room) -> Observable<Result<Room>> {
        if let user = account {
            if (member == nil) {
                member = Member(id: "", isMuted: false, isSelfMuted: false, role: .listener, room: room, streamId: 0, user: user)
            }
            member?.isLocal = true
            guard let member = member else {
                return Observable.just(Result(success: false, message: "member is nil!"))
            }
            if (self.rtcServer.channel == room.id) {
                return Observable.just(Result(success: true, data: room))
            } else {
                return Observable.just(self.rtcServer.isJoinChannel)
                    .concatMap { joining -> Observable<Result<Void>> in
                        if (joining) {
                            return self.leave()
                        } else {
                            return Observable.just(Result(success: true))
                        }
                    }
                    .concatMap { result -> Observable<Result<Void>> in
                        return result.onSuccess {
                            // set default status when join room
                            member.isMuted = false
                            member.role = room.anchor.id == user.id ? .manager : .listener
                            member.isManager = room.anchor.id == user.id
                            member.isSelfMuted = false
                            //member.room = room
                            member.user = user
                            return Observable.just(result)
                        }
                    }
                    .concatMap { result -> Observable<Result<Room>> in
                        return result.onSuccess { Room.getRoom(by: room.id) }
                    }
                    .concatMap { result -> Observable<Result<Void>> in
                        return result.onSuccess { self.rtcServer.joinChannel(member: member, channel: room.id, setting: self.setting) }
                    }
                    .concatMap { result -> Observable<Result<Void>> in
                        member.room = room
                        return result.onSuccess { self.member!.join(streamId: self.rtcServer.uid) }
                    }
                    .concatMap { result -> Observable<Result<Room>> in
                        if (result.success) {
                            member.room = room
                            return Observable.just(Result(success: true, data: room))
                        } else {
                            self.member = nil
                            if (self.rtcServer.isJoinChannel) {
                                return self.rtcServer.leaveChannel().map { _ in
                                    return Result(success: false, message: result.message)
                                }
                            }
                            return Observable.just(Result(success: false, message: result.message))
                        }
                    }
                }
        } else {
            return Observable.just(Result(success: false, message: "account is nil!"))
        }
    }
    
    func leave() -> Observable<Result<Void>> {
        if let member = member {
            if (rtcServer.isJoinChannel) {
                return Observable.zip(
                    self.rtcServer.leaveChannel(),
                    member.leave(),
                    self.rtmServer.logout()
                ).map { result0, result1, result2 in
                    if (!result0.success || !result1.success || !result2.success) {
                        Logger.log(message: "leaveRoom error: \(result0.message ?? "") \(result1.message ?? "") \(result2.message ?? "")", level: .error)
                    }
                    return Result(success: true)
                }
            } else {
                return Observable.just(Result(success: true))
            }
        } else {
            return Observable.just(Result(success: true))
        }
    }
    
    func subscribeActions() -> Observable<Result<Action>> {
        if let member = member {
            return member.subscribeActions()
        } else {
            return Observable.just(Result(success: false, message: "member is nil!"))
        }
    }
    
    func subscribeMembers() -> Observable<Result<Array<Member>>> {
        guard let room = member?.room else {
            return Observable.just(Result(success: false, message: "room is nil!"))
        }
        return Observable.combineLatest(
                room.subscribeMembers(),
                rtcServer.onSpeakersChanged()
            )
            .filter { [unowned self] _ in
                self.rtcServer.isJoinChannel
            }
            .throttle(RxTimeInterval.milliseconds(20), latest: true, scheduler: scheduler)
            .map { [unowned self] (args) -> Result<Array<Member>> in
                let (result, _) = args
                if (result.success) {
                    if let list = result.data {
                        // sync local user status
                        let findCurrentUser = list.first { member in
                            return member.id == self.member?.id
                        }
                        if let me = findCurrentUser, let old = member {
                            me.isLocal = true
                            me.isSelfMuted = old.isSelfMuted
                            old.isMuted = me.isMuted
                            old.role = me.role
                            self.rtcServer.setClientRole(me.role != .listener ? .broadcaster : .audience, self.setting.audienceLatency)
                            self.rtcServer.muteLocalMicrophone(mute: me.isMuted || me.isSelfMuted)
                        }
//                        uids.forEach { speaker in
//                            let user = list.first { item in
//                                return item.streamId == speaker.key
//                            }
//                            user?.isSelfMuted = speaker.value
//                        }
                        return Result(success: true, data: list)
                    }
                }
                return result
            }
    }
    
    func inviteSpeaker(member: Member) -> Observable<Result<Void>> {
        if let user = self.member {
            if (rtcServer.isJoinChannel && user.isManager) {
                return user.inviteSpeaker(member: member)
            }
        }
        return Observable.just(Result(success: true))
    }
    
    func muteSpeaker(member: Member) -> Observable<Result<Void>> {
        if let user = self.member {
            if (rtcServer.isJoinChannel && user.isManager) {
                return member.mute(mute: true)
            }
        }
        return Observable.just(Result(success: true))
    }
    
    func unMuteSpeaker(member: Member) -> Observable<Result<Void>> {
        if let user = self.member {
            if (rtcServer.isJoinChannel && user.isManager) {
                return member.mute(mute: false)
            }
        }
        return Observable.just(Result(success: true))
    }
    
    func kickSpeaker(member: Member) -> Observable<Result<Void>> {
        if let user = self.member {
            if (rtcServer.isJoinChannel && user.isManager) {
                return member.asListener()
            }
        }
        return Observable.just(Result(success: true))
    }
    
    func process(request: Action, agree: Bool) -> Observable<Result<Void>> {
        switch request.action {
        case .requestLeft:
            return request.setLeftSpeaker(agree: agree)
        case .requestRight:
            return request.setRightSpeaker(agree: agree)
        default:
            return Observable.just(Result(success: true))
        }
    }
    
    func process(invitionLeft: Action, agree: Bool) -> Observable<Result<Void>> {
        return invitionLeft.setLeftInvition(agree: agree)
    }
    
    func process(invitionRight: Action, agree: Bool) -> Observable<Result<Void>> {
        return invitionRight.setRightInvition(agree: agree)
    }
    
    func handsUp(left: Bool?) -> Observable<Result<Void>> {
        if let member = member {
            if (rtcServer.isJoinChannel) {
                if let left = left {
                    if (left) {
                        return member.requestLeft()
                    } else {
                        return member.requestRight()
                    }
                } else {
                    return member.handsup()
                }
            }
        }
        return Observable.just(Result(success: true))
    }
    
    func closeMicrophone(close: Bool) -> Observable<Result<Void>> {
        if let member = member {
            member.isSelfMuted = close
            if (rtcServer.isJoinChannel) {
                rtcServer.muteLocalMicrophone(mute: close)
                return member.selfMute(mute: close)
            } else {
                return Observable.just(Result(success: true))
            }
        } else {
            return Observable.just(Result(success: true))
        }
    }
    
    func isMicrophoneClose() -> Bool {
        return rtcServer.muted
    }
    
    func bindLocalVideo(view: UIView?) {
        if let view = view {
            rtcServer.bindLocalVideo(view: view)
        } else {
            rtcServer.unbindLocalVideo()
        }
    }
    
    func bindRemoteVideo(view: UIView?, uid: UInt) {
        if let view = view {
            rtcServer.bindRemoteVideo(view: view, uid: uid)
        } else {
            rtcServer.unbindRemoteVideo(uid: uid)
        }
    }
    
    func subscribeMessages() -> Observable<Result<Message>> {
        if let member = member {
            return rtmServer.login(user: member.id)
                .concatMap { [unowned self] result in
                    return result.onSuccess {
                        return self.rtmServer.join(room: member.room.id)
                    }
                }
                .concatMap { [unowned self] result in
                    return result.onSuccess {
                        return self.rtmServer.subscribeMessages(room: member.room.id)
                    }
                }
        } else {
            return Observable.just(Result(success: false, message: "member is nil!"))
        }
    }
    
    func sendMessage(message: String) -> Observable<Result<Void>> {
        if let member = member {
            return rtmServer.sendMessage(room: member.room.id, message: message)
        } else {
            return Observable.just(Result(success: false, message: "member is nil!"))
        }
    }
    
    func enableBeauty(enable: Bool) {
        if enable {
            rtcServer.enableBeauty()
        } else {
            rtcServer.diableBeauty()
        }
    }
    
    func isEnableBeauty() -> Bool {
        return rtcServer.isEnableBeauty
    }
}
