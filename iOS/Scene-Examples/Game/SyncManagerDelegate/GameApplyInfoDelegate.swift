//
//  GameApplyInfoDelegate.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/22.
//

import UIKit

class GameApplyInfoDelegate: ISyncManagerEventDelegate {
    private var vc: GameLiveController
    init(vc: GameLiveController) {
        self.vc = vc
    }
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated applyGameInfo == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(GameApplyInfoModel.self, value: object.toJson()) else { return }
        vc.gameApplyInfoModel = model
        onUpdated(object: object)
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated applyGameInfo == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(GameApplyInfoModel.self, value: object.toJson()) else { return }
        vc.gameApplyInfoModel = model
        
        var gameInfoModel = GameInfoModel()
        gameInfoModel.status = model.status
        gameInfoModel.gameUid = "\(vc.screenUserID)"
        
        if model.status == .no_start {
            vc.updatePKUIStatus(isStart: false)
        } else if model.status == .playing {
            vc.updatePKUIStatus(isStart: true)
            // 通知观众拉取屏幕流
            SyncUtil.update(id: vc.channleName,
                            key: SYNC_MANAGER_GAME_INFO,
                            params: JSONObject.toJson(gameInfoModel),
                            delegate: nil)
        } else {
            vc.updatePKUIStatus(isStart: false)
            // 更新观众状态
            SyncUtil.update(id: vc.channleName,
                            key: SYNC_MANAGER_GAME_INFO,
                            params: JSONObject.toJson(gameInfoModel),
                            delegate: nil)
        }

        guard vc.getRole(uid: "\(UserInfo.userId)") == .broadcaster else { return }
        vc.stopBroadcastButton.isHidden = model.status != .end
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted applyGameInfo == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed applyGameInfo", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError applyGameInfo code ==\(code) msg == \(msg)", level: .error)
    }
}

/// 观众游戏状态
class GameInfoDelegate: ISyncManagerEventDelegate {
    private var vc: GameLiveController
    init(vc: GameLiveController) {
        self.vc = vc
    }
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated game info == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
        vc.gameInfoModel = model
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated game info == \(String(describing: object.toJson()))", level: .info)
        guard vc.getRole(uid: "\(UserInfo.userId)") == .audience,
              let model = JSONObject.toModel(GameInfoModel.self, value: object.toJson()) else { return }
        vc.gameInfoModel = model
        if model.status == .no_start {
            vc.updatePKUIStatus(isStart: false)
        } else if model.status == .playing {
            vc.updatePKUIStatus(isStart: true)
        } else {
            vc.updatePKUIStatus(isStart: false)
        }
        vc.stopBroadcastButton.isHidden = true
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted game info == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed game info", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError game info code ==\(code) msg == \(msg)", level: .error)
    }
}
