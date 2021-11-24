//
//  PKInfoDelegate.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/16.
//

import UIKit

class PKInfoDelegate: ISyncManagerEventDelegate {
    private var vc: LivePlayerController
    init(vc: LivePlayerController) {
        self.vc = vc
    }
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated pkInfo == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(PKInfoModel.self, value: object.toJson()) else { return }
        if model.userId == "\(UserInfo.userId)" { return }
        vc.pkInfoModel = model
        vc.joinAudienceChannel(channelName: model.roomId, pkUid:  UInt(model.userId) ?? 0)
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated pkInfo == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(PKInfoModel.self, value: object.toJson()) else { return }
        if model.userId == "\(UserInfo.userId)" { return }
        vc.pkInfoModel = model
        if model.status == .end {
            vc.leaveChannel(uid: UserInfo.userId, channelName: model.roomId)
            // 删除PKInfo数据
            SyncUtil.deleteCollection(id: vc.channleName,
                                      className: SYNC_MANAGER_PK_INFO,
                                      delegate: nil)
        } else {
            vc.joinAudienceChannel(channelName: model.roomId, pkUid:  UInt(model.userId) ?? 0)
        }
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted pkInfo == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed pkInfo", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError pkInfo code ==\(code) msg == \(msg)", level: .error)
    }
}
