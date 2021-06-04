//
//  RtmServer.swift
//  BlindDate
//
//  Created by XC on 2021/4/25.
//

import Foundation
import RxSwift
import RxRelay
import AgoraRtmKit
import Core

class RtmServer: NSObject {
    private var rtmKit = AgoraRtmKit(appId: BuildConfig.AppId, delegate: nil)!
    private var userId: String?
    private var channelId: String?
    private var channel: AgoraRtmChannel?
    
    private var messageReceivedRelay: BehaviorRelay<BlindDateMessage?> = BehaviorRelay(value: nil)
    
    override init() {
        super.init()
        rtmKit.agoraRtmDelegate = self
    }
    
    func login(user: String) -> Observable<Result<Void>> {
        Logger.log(message: "userId:\(String(describing: userId)) user:\(user)", level: .info)
        if (userId == user) {
            return Observable.just(Result(success: true))
        } else {
            return Observable.just(userId != nil && userId != user)
                .concatMap { need -> Observable<Result<Void>> in
                    if need {
                        return self.logout()
                    } else {
                        return Observable.just(Result(success: true))
                    }
                }
                .concatMap { result -> Observable<Result<Void>> in
                    return result.onSuccess { () -> Observable<Result<Void>> in
                        return Single.create { single in
                            self.rtmKit.login(byToken: BuildConfig.RtmToken, user: user) { code in
                                if code == .ok {
                                    self.userId = user
                                    Logger.log(message: "login success", level: .info)
                                    single(.success(Result(success: true)))
                                } else {
                                    single(.success(Result(success: false, message: "login fail:\(code.rawValue)")))
                                }
                            }
                            return Disposables.create()
                        }.asObservable()
                    }
                }
        }
    }
    
    func logout() -> Observable<Result<Void>> {
        if (userId == nil) {
            return Observable.just(Result(success: true))
        } else {
            return leave().concatMap { result in
                return result.onSuccess {
                    return Single.create { single in
                        self.rtmKit.logout { code in
                            guard code == .ok else {
                                single(.success(Result(success: false, message: "logout fail: \(code.rawValue)")))
                                return
                            }
                            self.userId = nil
                            single(.success(Result(success: true)))
                        }
                        return Disposables.create()
                    }.asObservable()
                }
            }
        }
    }
    
    func join(room: String) -> Observable<Result<Void>> {
        if (userId == nil) {
            return Observable.just(Result(success: false, message: "login first!"))
        } else {
            return Observable.just(channelId != nil && channelId != room)
                .concatMap { leave -> Observable<Result<Void>> in
                    if leave {
                        return self.leave()
                    } else {
                        return Observable.just(Result(success: true))
                    }
                }
                .concatMap { result -> Observable<Result<Void>> in
                    return result.onSuccess { () -> Observable<Result<Void>> in
                        return Single.create { single in
                            self.channel = self.rtmKit.createChannel(withId: room, delegate: self)
                            if let channel = self.channel {
                                channel.join { code in
                                    guard code == .channelErrorOk else {
                                        single(.success(Result(success: false, message: "join fail: \(code.rawValue)")))
                                        return
                                    }
                                    self.channelId = room
                                    Logger.log(message: "join success", level: .info)
                                    single(.success(Result(success: true)))
                                }
                            } else {
                                single(.success(Result(success: false, message: "join fail: create channel is nil")))
                            }
                            return Disposables.create()
                        }.asObservable()
                    }
                }
        }
    }
    
    func leave() -> Observable<Result<Void>> {
        return Single.create { single in
            if let channel = self.channel {
                channel.leave { code in
                    if code == .ok {
                        self.channel = nil
                        self.channelId = nil
                        single(.success(Result(success: true)))
                    } else {
                        single(.success(Result(success: false, message: "leave error: \(code.rawValue)")))
                    }
                }
            } else {
                single(.success(Result(success: true)))
            }
            return Disposables.create()
        }.asObservable()
    }
    
    func subscribeMessages(room: String) -> Observable<Result<BlindDateMessage>> {
        if (userId == nil) {
            return Observable.just(Result(success: false, message: "login first!"))
        } else {
            return messageReceivedRelay.filter { message -> Bool in
                return message?.channelId == room
            }.map { message -> Result<BlindDateMessage> in
                Result(success: true, data: message)
            }
        }
    }
    
    func sendMessage(room: String, message: String) -> Observable<Result<Void>> {
        if (userId == nil) {
            return Observable.just(Result(success: false, message: "login first!"))
        } else if (channelId != room) {
            return Observable.just(Result(success: false, message: "join channel first!"))
        } else if let channel = self.channel {
            return Single.create { single in
                channel.send(AgoraRtmMessage(text: message)) { code in
                    guard code == .errorOk else {
                        single(.success(Result(success: false, message: "send message error: \(code.rawValue)")))
                        return
                    }
                    single(.success(Result(success: true)))
                    if let userId = self.userId {
                        self.messageReceivedRelay.accept(BlindDateMessage(channelId: room, userId: userId, value: message))
                    }
                    Logger.log(message: "send message: \(message) success", level: .info)
                }
                return Disposables.create()
            }.asObservable()
        } else {
            return Observable.just(Result(success: false, message: "join channel first!"))
        }
    }
}

extension RtmServer: AgoraRtmDelegate {
    
}

extension RtmServer: AgoraRtmChannelDelegate {
    func channel(_ channel: AgoraRtmChannel, messageReceived message: AgoraRtmMessage, from member: AgoraRtmMember) {
        Logger.log(message: "messageReceived: \(message.text)", level: .info)
        if (channel == self.channel) {
            if let id = channelId {
                messageReceivedRelay.accept(BlindDateMessage(channelId: id, userId: member.userId, value: message.text))
            }
        }
    }
}
