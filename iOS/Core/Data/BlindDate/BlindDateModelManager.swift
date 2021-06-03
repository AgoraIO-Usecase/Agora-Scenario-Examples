//
//  BlindDateModelManager.swift
//  Core
//
//  Created by XC on 2021/6/1.
//

import Foundation
import RxSwift

extension BlindDateRoom {
    public static let TABLE: String = "ROOM_MARRY"
    public static let ANCHOR_ID: String = "anchorId"
    public static let CHANNEL_NAME: String = "channelName"
}

extension BlindDateMember {
    public static let TABLE: String = "MEMBER_MARRY"
    public static let MUTED: String = "isMuted"
    public static let SELF_MUTED: String = "isSelfMuted"
    public static let ROLE: String = "role"
    public static let ROOM: String = "roomId"
    public static let STREAM_ID = "streamId"
    public static let USER = "userId"
}

extension BlindDateAction {
    public static let TABLE: String = "ACTION_MARRY"
    public static let ACTION: String = "action"
    public static let MEMBER: String = "memberId"
    public static let ROOM: String = "roomId"
    public static let STATUS: String = "status"
}

public protocol IBlindDateModelManagerProxy {
    func create(room: BlindDateRoom) -> Observable<Result<String>>
    func delete(room: BlindDateRoom) -> Observable<Result<Void>>
    func getRooms() -> Observable<Result<Array<BlindDateRoom>>>
    func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>>
    func update(room: BlindDateRoom) -> Observable<Result<String>>
    func getMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>>
    func getCoverSpeakers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>>
    func subscribeMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>>
    
    func join(member: BlindDateMember, streamId: UInt) -> Observable<Result<Void>>
    func mute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>>
    func selfMute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>>
    func asListener(member: BlindDateMember) -> Observable<Result<Void>>
    func asLeftSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>>
    func asRightSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>>
    func leave(member: BlindDateMember) -> Observable<Result<Void>>
    func subscribeActions(member: BlindDateMember) -> Observable<Result<BlindDateAction>>
    func handsup(member: BlindDateMember) -> Observable<Result<Void>>
    func requestLeft(member: BlindDateMember) -> Observable<Result<Void>>
    func requestRight(member: BlindDateMember) -> Observable<Result<Void>>
    func inviteSpeaker(master: BlindDateMember, member: BlindDateMember) -> Observable<Result<Void>>
    func rejectInvition(member: BlindDateMember) -> Observable<Result<Void>>
}

public class BlindDateModelManager {
    private var proxy: IBlindDateModelManagerProxy!
    public static var shared: BlindDateModelManager = BlindDateModelManager()
    
    private init() {}
    
    public func setProxy(_ proxy: IBlindDateModelManagerProxy) {
        self.proxy = proxy
    }
    
    public func create(room: BlindDateRoom) -> Observable<Result<String>> {
        return proxy.create(room: room)
    }
    
    public func delete(room: BlindDateRoom) -> Observable<Result<Void>> {
        return proxy.delete(room: room)
    }
    
    public func getRooms() -> Observable<Result<Array<BlindDateRoom>>> {
        return proxy.getRooms()
    }
    
    public func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>> {
        return proxy.getRoom(by: objectId)
    }
    
    public func update(room: BlindDateRoom) -> Observable<Result<String>> {
        return proxy.update(room: room)
    }
    
    public func getMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return proxy.getMembers(room: room)
    }
    
    public func getCoverSpeakers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return proxy.getCoverSpeakers(room: room)
    }
    
    public func subscribeMembers(room: BlindDateRoom) -> Observable<Result<Array<BlindDateMember>>> {
        return proxy.subscribeMembers(room: room)
    }
    
    public func join(member: BlindDateMember, streamId: UInt) -> Observable<Result<Void>> {
        return proxy.join(member: member, streamId: streamId)
    }
    
    public func mute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>> {
        return proxy.mute(member: member, mute: mute)
    }
    
    public func selfMute(member: BlindDateMember, mute: Bool) -> Observable<Result<Void>> {
        return proxy.selfMute(member: member, mute: mute)
    }
    
    public func asListener(member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.asListener(member: member)
    }
    
    public func asLeftSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>> {
        return proxy.asLeftSpeaker(member: member, agree: agree)
    }
    
    public func asRightSpeaker(member: BlindDateMember, agree: Bool) -> Observable<Result<Void>> {
        return proxy.asRightSpeaker(member: member, agree: agree)
    }
    
    public func leave(member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.leave(member: member)
    }
    
    public func subscribeActions(member: BlindDateMember) -> Observable<Result<BlindDateAction>> {
        return proxy.subscribeActions(member: member)
    }
    
    public func handsup(member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.handsup(member: member)
    }
    
    public func requestLeft(member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.requestLeft(member: member)
    }
    
    public func requestRight(member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.requestRight(member: member)
    }
    
    public func inviteSpeaker(master: BlindDateMember, member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.inviteSpeaker(master: master, member: member)
    }
    
    public func rejectInvition(member: BlindDateMember) -> Observable<Result<Void>> {
        return proxy.rejectInvition(member: member)
    }
}
