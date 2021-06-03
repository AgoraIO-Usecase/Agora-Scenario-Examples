//
//  Model.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import RxSwift
import Core

extension BlindDateRoom {
    static func create(room: BlindDateRoom) -> Observable<Result<String>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).create(room: room)
    }
    
    func delete() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).delete(room: self)
    }
    
    static func getRooms() -> Observable<Result<Array<BlindDateRoom>>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).getRooms()
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<BlindDateRoom>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).getRoom(by: objectId)
    }
    
    static func update(room: BlindDateRoom) -> Observable<Result<String>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).update(room: room)
    }
    
    func getMembers() -> Observable<Result<Array<BlindDateMember>>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).getMembers(room: self)
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<BlindDateMember>>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).getCoverSpeakers(room: self)
    }
    
    func subscribeMembers() -> Observable<Result<Array<BlindDateMember>>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).subscribeMembers(room: self)
    }
}

extension BlindDateMember {
    func join(streamId: UInt) -> Observable<Result<Void>>{
        return InjectionService.shared.resolve(IBlindDateModelManager.self).join(member: self, streamId: streamId)
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).mute(member: self, mute: mute)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).selfMute(member: self, mute: mute)
    }
    
    func asListener() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).asListener(member: self)
    }
    
    func asLeftSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).asLeftSpeaker(member: self, agree: agree)
    }
    
    func asRightSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).asRightSpeaker(member: self, agree: agree)
    }
    
    func leave() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).leave(member: self)
    }
    
    func subscribeActions() -> Observable<Result<BlindDateAction>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).subscribeActions(member: self)
    }
    
    func handsup() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).handsup(member: self)
    }
    
    func requestLeft() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).requestLeft(member: self)
    }
    
    func requestRight() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).requestRight(member: self)
    }
    
    func inviteSpeaker(member: BlindDateMember) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).inviteSpeaker(master: self, member: member)
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IBlindDateModelManager.self).rejectInvition(member: self)
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
