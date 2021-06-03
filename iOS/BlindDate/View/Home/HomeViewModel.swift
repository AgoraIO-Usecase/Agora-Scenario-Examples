//
//  HomeViewModel.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import Foundation
import RxSwift
import RxRelay
import RxCocoa
import IGListKit
import Core

class HomeViewModel {
    let activityIndicator = ActivityIndicator()
    var roomList: [BlindDateRoom] = []
    private var scheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "io")
    
    func setup() -> Observable<Result<Void>> {
        return Server.shared().getAccount().map { $0.transform() }
            // UIRefreshControl bug?
            .delay(DispatchTimeInterval.microseconds(200), scheduler: scheduler)
    }
    
    func account() -> User? {
        return Server.shared().account
    }

    func dataSource() -> Observable<Result<Array<BlindDateRoom>>> {
        return Server.shared().getRooms()
            .map { [unowned self] data in
                if (data.success) {
                    self.roomList.removeAll()
                    self.roomList.append(contentsOf: data.data ?? [])
                }
                return data
            }
            .trackActivity(activityIndicator)
            .subscribe(on: scheduler)
    }
    
//    func _dataSource() -> Observable<Result<Array<Room>>> {
//        return Observable.just(Result(success: true, data: [
//            Room(id: "u01", channelName: "Post vday 互相表白大会", anchor: account()!),
//            Room(id: "u02", channelName: "Inez Garcia", anchor: account()!),
//            Room(id: "u03", channelName: "Top 3 Greatet Rappers of All Time", anchor: account()!)
//        ])).delay(DispatchTimeInterval.seconds(3), scheduler: scheduler)
//    }
    
    func createRoom(with name: String) -> Observable<Result<BlindDateRoom>> {
        let account = self.account()
        if let anchor = account {
            return Server.shared().create(room: BlindDateRoom(id: "", channelName: name, anchor: anchor))
        } else {
            return Observable.just(Result(success: false, message: "account is nil!"))
        }
    }
    
    func join(room: BlindDateRoom) -> Observable<Result<BlindDateRoom>> {
        return Server.shared().join(room: room)
    }
}

extension BlindDateRoom: ListDiffable {
    public func diffIdentifier() -> NSObjectProtocol {
        return id as NSObjectProtocol
    }
    
    public func isEqual(toDiffableObject object: ListDiffable?) -> Bool {
        guard self !== object else { return true }
        guard let object = object as? BlindDateRoom else { return false }
        return id == object.id &&
            channelName == object.channelName &&
            total == object.total &&
            speakersTotal == object.speakersTotal
    }
}
