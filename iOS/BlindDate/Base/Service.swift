//
//  Service.swift
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

protocol Service {
    var account: User? { get set }
    var member: Member? { get set }
    var setting: LocalSetting { get set }
    func updateSetting()
    
    func getAccount() -> Observable<Result<User>>
    func getRooms() -> Observable<Result<Array<Room>>>
    func create(room: Room) -> Observable<Result<Room>>
    func join(room: Room) -> Observable<Result<Room>>
    func leave() -> Observable<Result<Void>>
    
    func closeMicrophone(close: Bool) -> Observable<Result<Void>>
    func isMicrophoneClose() -> Bool
    
    func enableBeauty(enable: Bool)
    func isEnableBeauty() -> Bool
    
    func subscribeMembers() -> Observable<Result<Array<Member>>>
    func subscribeActions() -> Observable<Result<Action>>
    
    func inviteSpeaker(member: Member) -> Observable<Result<Void>>
    func muteSpeaker(member: Member) -> Observable<Result<Void>>
    func unMuteSpeaker(member: Member) -> Observable<Result<Void>>
    func kickSpeaker(member: Member) -> Observable<Result<Void>>
    
    func process(request: Action, agree: Bool) -> Observable<Result<Void>>
    func process(invitionLeft: Action, agree: Bool) -> Observable<Result<Void>>
    func process(invitionRight: Action, agree: Bool) -> Observable<Result<Void>>
    
    func handsUp(left: Bool?) -> Observable<Result<Void>>
    
    func bindLocalVideo(view: UIView?)
    func bindRemoteVideo(view: UIView?, uid: UInt)
    
    func subscribeMessages() -> Observable<Result<Message>>
    func sendMessage(message: String) -> Observable<Result<Void>>
}

protocol ErrorDescription {
    associatedtype Item
    static func toErrorString(type: Item, code: Int32) -> String
}
