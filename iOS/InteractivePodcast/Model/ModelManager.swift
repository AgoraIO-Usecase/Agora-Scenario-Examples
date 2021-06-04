//
//  ModelManager.swift
//  InteractivePodcast
//
//  Created by XC on 2021/6/4.
//

import Foundation
import RxSwift
import Core

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

public protocol IPodcastModelManager {
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
