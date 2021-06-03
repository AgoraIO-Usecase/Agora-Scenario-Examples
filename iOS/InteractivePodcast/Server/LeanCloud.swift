//
//  Database.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/7.
//
#if test
import Foundation
import RxSwift
import Core

extension PodcastRoom {
    
    static func create(room: PodcastRoom) -> Observable<Result<String>> {
        return PodcastModelManager.shared.create(room: room)
    }
    
    func delete() -> Observable<Result<Void>> {
        return PodcastModelManager.shared.delete(room: self)
    }
    
    static func getRooms() -> Observable<Result<Array<PodcastRoom>>> {
        return PodcastModelManager.shared.getRooms()
    }
    
    static func getRoom(by objectId: String) -> Observable<Result<PodcastRoom>> {
        return PodcastModelManager.shared.getRoom(by: objectId)
    }
    
    static func update(room: PodcastRoom) -> Observable<Result<String>> {
        return PodcastModelManager.shared.update(room: room)
    }
    
    func getMembers() -> Observable<Result<Array<PodcastMember>>> {
        return PodcastModelManager.shared.getMembers(room: self)
    }
    
    func getCoverSpeakers() -> Observable<Result<Array<PodcastMember>>> {
        return PodcastModelManager.shared.getCoverSpeakers(room: self)
    }
    
    func subscribeMembers() -> Observable<Result<Array<PodcastMember>>> {
        return PodcastModelManager.shared.subscribeMembers(room: self)
    }
}

extension PodcastMember {
    
    func join(streamId: UInt) -> Observable<Result<Void>>{
        return PodcastModelManager.shared.join(member: self, streamId: streamId)
    }
    
    func mute(mute: Bool) -> Observable<Result<Void>> {
        return PodcastModelManager.shared.mute(member: self, mute: mute)
    }
    
    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return PodcastModelManager.shared.selfMute(member: self, mute: mute)
    }
    
    func asSpeaker(agree: Bool) -> Observable<Result<Void>> {
        return PodcastModelManager.shared.asSpeaker(member: self, agree: agree)
    }
    
    func leave() -> Observable<Result<Void>> {
        return PodcastModelManager.shared.leave(member: self)
    }
    
    func subscribeActions() -> Observable<Result<PodcastAction>> {
        return PodcastModelManager.shared.subscribeActions(member: self)
    }
    
    func handsup() -> Observable<Result<Void>> {
        return PodcastModelManager.shared.handsup(member: self)
    }
    
    func inviteSpeaker(member: PodcastMember) -> Observable<Result<Void>> {
        return PodcastModelManager.shared.inviteSpeaker(master: self, member: member)
    }
    
    func rejectInvition() -> Observable<Result<Void>> {
        return PodcastModelManager.shared.rejectInvition(member: self)
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

#endif
