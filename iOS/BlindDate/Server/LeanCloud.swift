//
//  LeanCloud.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

#if test
import Foundation
import RxSwift
import Core

extension BlindDateRoom {

    static func create(room: BlindDateRoom) -> Observable<Result<String>> {
        return BlindDateModelManager.shared.create(room: room)
    }
    
    func delete() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.delete(room: self)
    }
    
    static func getRooms() -> Observable<Result<Array<BlindDateRoom>>> {
        return BlindDateModelManager.shared.getRooms()
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>> {
        return BlindDateModelManager.shared.getRoom(by: objectId)
    }
    
    static func update(room: BlindDateRoom) -> Observable<Result<String>> {
        return BlindDateModelManager.shared.update(room: room)
    }
    
    func getMembers() -> Observable<Result<Array<BlindDateMember>>> {
        return BlindDateModelManager.shared.getMembers(room: self)
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<BlindDateMember>>> {
        return BlindDateModelManager.shared.getCoverSpeakers(room: self)
    }
    
    func subscribeMembers() -> Observable<Result<Array<BlindDateMember>>> {
        return BlindDateModelManager.shared.subscribeMembers(room: self)
    }
}

extension BlindDateMember {
    
    func join(streamId: UInt) -> Observable<Result<Void>>{
        return BlindDateModelManager.shared.join(member: self, streamId: streamId)
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.mute(member: self, mute: mute)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.selfMute(member: self, mute: mute)
    }
    
    func asListener() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.asListener(member: self)
    }
    
    func asLeftSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.asLeftSpeaker(member: self, agree: agree)
    }
    
    func asRightSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.asRightSpeaker(member: self, agree: agree)
    }
    
    func leave() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.leave(member: self)
    }
    
    func subscribeActions() -> Observable<Result<BlindDateAction>> {
        return BlindDateModelManager.shared.subscribeActions(member: self)
    }
    
    func handsup() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.handsup(member: self)
    }
    
    func requestLeft() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.requestLeft(member: self)
    }
    
    func requestRight() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.requestRight(member: self)
    }
    
    func inviteSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.inviteSpeaker(master: self, member: member)
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        return BlindDateModelManager.shared.rejectInvition(member: self)
    }
}

extension BlindDateAction {
    
    func setLeftSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return member.asLeftSpeaker(agree: agree)
    }
    
    func setRightSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return member.asRightSpeaker(agree: agree)
    }
    
    func setLeftInvition(agree: Bool) -> Observable<Result<Void>> {
        if (agree) {
            return member.asLeftSpeaker(agree: agree)
        } else {
            return member.rejectInvition()
        }
    }
    
    func setRightInvition(agree: Bool) -> Observable<Result<Void>> {
        if (agree) {
            return member.asRightSpeaker(agree: agree)
        } else {
            return member.rejectInvition()
        }
    }
}

#endif
