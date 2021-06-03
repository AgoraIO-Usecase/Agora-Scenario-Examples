//
//  Service.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/3.
//

import Foundation
import RxSwift
import Core

protocol Service {
    var account: User? { get set }
    var member: PodcastMember? { get set }
    var setting: LocalSetting { get set }
    func updateSetting()
    
    func getAccount() -> Observable<Result<User>>
    func getRooms() -> Observable<Result<Array<PodcastRoom>>>
    func create(room: PodcastRoom) -> Observable<Result<PodcastRoom>>
    func join(room: PodcastRoom) -> Observable<Result<PodcastRoom>>
    func leave() -> Observable<Result<Void>>
    func closeMicrophone(close: Bool) -> Observable<Result<Void>>
    func isMicrophoneClose() -> Bool
    
    func subscribeMembers() -> Observable<Result<Array<PodcastMember>>>
    func subscribeActions() -> Observable<Result<PodcastAction>>
    
    func inviteSpeaker(member: PodcastMember) -> Observable<Result<Void>>
    func muteSpeaker(member: PodcastMember) -> Observable<Result<Void>>
    func unMuteSpeaker(member: PodcastMember) -> Observable<Result<Void>>
    func kickSpeaker(member: PodcastMember) -> Observable<Result<Void>>
    func process(handsup: PodcastAction, agree: Bool) -> Observable<Result<Void>>
    
    func process(invition: PodcastAction, agree: Bool) -> Observable<Result<Void>>
    func handsUp() -> Observable<Result<Void>>
    
    func destory()
}

protocol ErrorDescription {
    associatedtype Item
    static func toErrorString(type: Item, code: Int32) -> String
}
