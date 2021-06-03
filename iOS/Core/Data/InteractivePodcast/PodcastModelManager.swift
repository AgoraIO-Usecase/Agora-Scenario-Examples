//
//  InteractivePodcastModelManager.swift
//  Core
//
//  Created by XC on 2021/6/1.
//

import Foundation
import RxSwift

extension PodcastRoom {
    public static let TABLE: String = "ROOM"
    public static let ANCHOR_ID: String = "anchorId"
    public static let CHANNEL_NAME: String = "channelName"
}

extension PodcastMember {
    public static let TABLE: String = "MEMBER"
    public static let MUTED: String = "isMuted"
    public static let SELF_MUTED: String = "isSelfMuted"
    public static let IS_SPEAKER: String = "isSpeaker"
    public static let ROOM: String = "roomId"
    public static let STREAM_ID = "streamId"
    public static let USER = "userId"
}

extension PodcastAction {
    public static let TABLE: String = "ACTION"
    public static let ACTION: String = "action"
    public static let MEMBER: String = "memberId"
    public static let ROOM: String = "roomId"
    public static let STATUS: String = "status"
}

public protocol IPodcastModelManagerProxy {
    func create(room: PodcastRoom) -> Observable<Result<String>>
    func delete(room: PodcastRoom) -> Observable<Result<Void>>
    func getRooms() -> Observable<Result<Array<PodcastRoom>>>
    func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>>
    func update(room: PodcastRoom) -> Observable<Result<String>>
    func getMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>>
    func getCoverSpeakers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>>
    func subscribeMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>>
    
    func join(member: PodcastMember, streamId: UInt) -> Observable<Result<Void>>
    func mute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>>
    func selfMute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>>
    func asSpeaker(member: PodcastMember, agree: Bool) -> Observable<Result<Void>>
    func leave(member: PodcastMember) -> Observable<Result<Void>>
    func subscribeActions(member: PodcastMember) -> Observable<Result<PodcastAction>>
    func handsup(member: PodcastMember) -> Observable<Result<Void>>
    func inviteSpeaker(master: PodcastMember, member: PodcastMember) -> Observable<Result<Void>>
    func rejectInvition(member: PodcastMember) -> Observable<Result<Void>>
}

public class PodcastModelManager {
    private var proxy: IPodcastModelManagerProxy!
    public static var shared: PodcastModelManager = PodcastModelManager()
    
    private init() {}
    
    public func setProxy(_ proxy: IPodcastModelManagerProxy) {
        self.proxy = proxy
    }
    
    public func create(room: PodcastRoom) -> Observable<Result<String>> {
        return proxy.create(room: room)
    }
    
    public func delete(room: PodcastRoom) -> Observable<Result<Void>> {
        return proxy.delete(room: room)
    }
    
    public func getRooms() -> Observable<Result<Array<PodcastRoom>>> {
        return proxy.getRooms()
    }
    
    public func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>> {
        return proxy.getRoom(by: objectId)
    }
    
    public func update(room: PodcastRoom) -> Observable<Result<String>> {
        return proxy.update(room: room)
    }
    
    public func getMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return proxy.getMembers(room: room)
    }
    
    public func getCoverSpeakers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return proxy.getCoverSpeakers(room: room)
    }
    
    public func subscribeMembers(room: PodcastRoom) -> Observable<Result<Array<PodcastMember>>> {
        return proxy.subscribeMembers(room: room)
    }
    
    public func join(member: PodcastMember, streamId: UInt) -> Observable<Result<Void>> {
        return proxy.join(member: member, streamId: streamId)
    }
    
    public func mute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>> {
        return proxy.mute(member: member, mute: mute)
    }
    
    public func selfMute(member: PodcastMember, mute: Bool) -> Observable<Result<Void>> {
        return proxy.selfMute(member: member, mute: mute)
    }
    
    public func asSpeaker(member: PodcastMember, agree: Bool) -> Observable<Result<Void>> {
        return proxy.asSpeaker(member: member, agree: agree)
    }
    
    public func leave(member: PodcastMember) -> Observable<Result<Void>> {
        return proxy.leave(member: member)
    }
    
    public func subscribeActions(member: PodcastMember) -> Observable<Result<PodcastAction>> {
        return proxy.subscribeActions(member: member)
    }
    
    public func handsup(member: PodcastMember) -> Observable<Result<Void>> {
        return proxy.handsup(member: member)
    }
    
    public func inviteSpeaker(master: PodcastMember, member: PodcastMember) -> Observable<Result<Void>> {
        return proxy.inviteSpeaker(master: master, member: member)
    }
    
    public func rejectInvition(member: PodcastMember) -> Observable<Result<Void>> {
        return proxy.rejectInvition(member: member)
    }
}
