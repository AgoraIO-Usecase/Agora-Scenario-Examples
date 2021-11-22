//
//  GameInfoDelegate.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/22.
//

import UIKit

class GameInfoDelegate: ISyncManagerEventDelegate {
    private var vc: GameLiveController
    init(vc: GameLiveController) {
        self.vc = vc
    }
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated game == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
        self.vc.gameInfoModel = model
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated game == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
        if model.status == .no_start {
            vc.updatePKUIStatus(isStart: false)
        } else if model.status == .playing {
            vc.updatePKUIStatus(isStart: true)
        } else {
            vc.updatePKUIStatus(isStart: false)
            SyncUtil.deleteCollection(id: vc.targetChannelName,
                                      className: SceneType.game.rawValue,
                                      delegate: nil)
        }
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted game == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed game", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError game code ==\(code) msg == \(msg)", level: .error)
    }
}