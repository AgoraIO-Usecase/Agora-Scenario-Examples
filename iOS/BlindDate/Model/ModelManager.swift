//
//  ModelManager.swift
//  BlindDate
//
//  Created by XC on 2021/6/4.
//

import Foundation
import RxSwift
import Core

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

public protocol IBlindDateModelManager {
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
