//
//  GroupViewModel.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/3.
//

import Foundation
import RxSwift
import RxRelay
import RxCocoa
import IGListKit
import Core

class HomeViewModel {
    let activityIndicator = ActivityIndicator()
    let showMiniRoom: PublishRelay<Bool>
    var roomList: [PodcastRoom] = []
    
    private var scheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "io")

    init() {
        showMiniRoom = PublishRelay<Bool>()
    }

    func setup() -> Observable<Result<Void>> {
        return RoomManager.shared().getAccount().map { $0.transform() }
            // UIRefreshControl bug?
            .delay(DispatchTimeInterval.microseconds(200), scheduler: scheduler)
    }
    
    func account() -> User? {
        return RoomManager.shared().account
    }

    func dataSource() -> Observable<Result<Array<PodcastRoom>>> {
        return RoomManager.shared().getRooms()
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
    
    func createRoom(with name: String) -> Observable<Result<PodcastRoom>> {
        let account = self.account()
        if let anchor = account {
            return RoomManager.shared().create(room: PodcastRoom(id: "", channelName: name, anchor: anchor))
        } else {
            return Observable.just(Result(success: false, message: "account is nil!"))
        }
    }
    
    func join(room: PodcastRoom) -> Observable<Result<PodcastRoom>> {
        return RoomManager.shared().join(room: room)
    }
}

extension PodcastRoom: ListDiffable {
    public func diffIdentifier() -> NSObjectProtocol {
        return id as NSObjectProtocol
    }
    
    public func isEqual(toDiffableObject object: ListDiffable?) -> Bool {
        guard self !== object else { return true }
        guard let object = object as? PodcastRoom else { return false }
        return id == object.id &&
            channelName == object.channelName &&
            total == object.total &&
            speakersTotal == object.speakersTotal
    }
}
