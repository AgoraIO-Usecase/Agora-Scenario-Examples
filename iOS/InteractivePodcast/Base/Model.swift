//
//  Model.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/7.
//

import Foundation
import RxSwift
import Core

extension PodcastRoom {
    static func create(room: PodcastRoom) -> Observable<Result<String>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).create(room: room)
    }
    
    func delete() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).delete(room: self)
    }
    
    static func getRooms() -> Observable<Result<Array<PodcastRoom>>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).getRooms()
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).getRoom(by: objectId)
    }
    
    static func update(room: PodcastRoom) -> Observable<Result<String>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).update(room: room)
    }
    
    func getMembers() -> Observable<Result<Array<PodcastMember>>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).getMembers(room: self)
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<PodcastMember>>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).getCoverSpeakers(room: self)
    }
    
    func subscribeMembers() -> Observable<Result<Array<PodcastMember>>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).subscribeMembers(room: self)
    }
}

extension PodcastMember {
    func join(streamId: UInt) -> Observable<Result<Void>>{
        return InjectionService.shared.resolve(IPodcastModelManager.self).join(member: self, streamId: streamId)
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).mute(member: self, mute: mute)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).selfMute(member: self, mute: mute)
    }
    
    func asSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).asSpeaker(member: self, agree: agree)
    }
    
    func leave() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).leave(member: self)
    }
    
    func subscribeActions() -> Observable<Result<PodcastAction>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).subscribeActions(member: self)
    }
    
    func handsup() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).handsup(member: self)
    }
    
    func inviteSpeaker(member: PodcastMember) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).inviteSpeaker(master: self, member: member)
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IPodcastModelManager.self).rejectInvition(member: self)
    }
}

extension PodcastAction {
    func setSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return member.asSpeaker(agree: agree)
    }
    
    func setInvition(agree: Bool) -> Observable<Result<Void>> {
        if (agree) {
            return member.asSpeaker(agree: agree)
        } else {
            return member.rejectInvition()
        }
    }
}
