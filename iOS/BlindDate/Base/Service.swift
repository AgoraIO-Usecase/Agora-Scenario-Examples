//
//  Service.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import RxSwift
import Core

protocol Service {
    var account: User? { get set }
    var member: BlindDateMember? { get set }
    var setting: LocalSetting { get set }
    func updateSetting()
    
    func getAccount() -> Observable<Result<User>>
    func getRooms() -> Observable<Result<Array<BlindDateRoom>>>
    func create(room: BlindDateRoom) -> Observable<Result<BlindDateRoom>>
    func join(room: BlindDateRoom) -> Observable<Result<BlindDateRoom>>
    func leave() -> Observable<Result<Void>>
    
    func closeMicrophone(close: Bool) -> Observable<Result<Void>>
    func isMicrophoneClose() -> Bool
    
    func enableBeauty(enable: Bool)
    func isEnableBeauty() -> Bool
    
    func subscribeMembers() -> Observable<Result<Array<BlindDateMember>>>
    func subscribeActions() -> Observable<Result<BlindDateAction>>
    
    func inviteSpeaker(member: BlindDateMember) -> Observable<Result<Void>>
    func muteSpeaker(member: BlindDateMember) -> Observable<Result<Void>>
    func unMuteSpeaker(member: BlindDateMember) -> Observable<Result<Void>>
    func kickSpeaker(member: BlindDateMember) -> Observable<Result<Void>>
    
    func process(request: BlindDateAction, agree: Bool) -> Observable<Result<Void>>
    func process(invitionLeft: BlindDateAction, agree: Bool) -> Observable<Result<Void>>
    func process(invitionRight: BlindDateAction, agree: Bool) -> Observable<Result<Void>>
    
    func handsUp(left: Bool?) -> Observable<Result<Void>>
    
    func bindLocalVideo(view: UIView?)
    func bindRemoteVideo(view: UIView?, uid: UInt)
    
    func subscribeMessages() -> Observable<Result<BlindDateMessage>>
    func sendMessage(message: String) -> Observable<Result<Void>>
    
    func destory()
}

protocol ErrorDescription {
    associatedtype Item
    static func toErrorString(type: Item, code: Int32) -> String
}
