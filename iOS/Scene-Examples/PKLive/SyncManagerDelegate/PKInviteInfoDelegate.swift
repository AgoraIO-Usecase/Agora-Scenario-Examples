//
//  PKInviteInfoDelegate.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit

/// pk开始回调
var pkLiveStartClosure: ((PKApplyInfoModel) -> Void)?
/// pk结束回调
var pkLiveEndClosure: ((PKApplyInfoModel) -> Void)?

class PKInviteInfoDelegate: ISyncManagerEventDelegate {
    private var vc: LivePlayerController
    init(vc: LivePlayerController) {
        self.vc = vc
    }
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated invite == \(String(describing: object.toJson()))", level: .info)
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated invite == \(String(describing: object.toJson()))", level: .info)
        guard var model = JSONObject.toModel(PKApplyInfoModel.self, value: object.toJson()) else { return }
        if model.status == .end {
            print("========== me end ==================")
            // 自己在对方的channel中移除
            vc.leaveChannel(uid: UserInfo.userId, channelName: model.roomId)
            vc.updateLiveLayout(postion: .full)
            
            // 删除PK邀请数据
            SyncUtil.deleteCollection(id: model.targetRoomId ?? "",
                                      className: SceneType.pkApply.rawValue,
                                      delegate: nil)
            
            pkLiveEndClosure?(model)
            
        } else if model.status == .accept {
            vc.updateLiveLayout(postion: .center)
            let userId: UInt = UInt(model.userId) ?? 0
            // 把自己加入到对方的channel
            vc.joinAudienceChannel(channelName: model.roomId, pkUid: userId)
            
            pkLiveStartClosure?(model)
            
            // 通知观众加入到pk的channel
            var pkInfo = PKInfoModel()
            pkInfo.status = model.status
            pkInfo.roomId = model.roomId
            pkInfo.userId = model.userId
            SyncUtil.addCollection(id: model.targetRoomId ?? "",
                                   className: SceneType.pkInfo.rawValue,
                                   params: JSONObject.toJson(pkInfo),
                                   delegate: PKInfoAddDataDelegate(vc: vc))
            
        } else if model.status == .invite && "\(UserInfo.userId)" != model.userId {
            vc.showAlert(title: vc.sceneType.alertTitle, message: "") {
                model.status = .refuse
                SyncUtil.updateCollection(id: model.targetRoomId ?? "",
                                          className: SceneType.pkApply.rawValue,
                                          objectId: model.objectId,
                                          params: JSONObject.toJson(model),
                                          delegate: nil)
                
            } confirm: {
                model.status = .accept
                SyncUtil.updateCollection(id: model.targetRoomId ?? "",
                                          className: SceneType.pkApply.rawValue,
                                          objectId: model.objectId,
                                          params: JSONObject.toJson(model),
                                          delegate: nil)
            }
        }
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted invite == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed invite", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError gift code ==\(code) msg == \(msg)", level: .error)
    }
}

class PKInviteInfoTargetDelegate: ISyncManagerEventDelegate {
    private var vc: LivePlayerController
    init(vc: LivePlayerController) {
        self.vc = vc
    }
    
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated target invite == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(PKApplyInfoModel.self, value: object.toJson()) else { return }
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated target invite == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(PKApplyInfoModel.self, value: object.toJson()) else { return }
        if model.status == .end {
            print("========== target end ==================")
            vc.updateLiveLayout(postion: .full)
            // 把对方在自己的channel中移除
            vc.leaveChannel(uid: UserInfo.userId, channelName: model.targetRoomId ?? "")
            
            // 删除pk 邀请数据
            SyncUtil.deleteCollection(id: model.roomId,
                                      className: SceneType.pkApply.rawValue,
                                      delegate: nil)
            
            pkLiveEndClosure?(model)
            
        } else if model.status == .accept {
            vc.updateLiveLayout(postion: .center)
            // 把对方加入自己的channel
            let targetUserId = UInt(model.targetUserId ?? "0") ?? 0
            vc.joinAudienceChannel(channelName: model.targetRoomId ?? "", pkUid: targetUserId)
            
            pkLiveStartClosure?(model)
            
            // 通知观众加入到pk的channel
            var pkInfo = PKInfoModel()
            pkInfo.status = model.status
            pkInfo.roomId = model.targetRoomId ?? ""
            pkInfo.userId = model.targetUserId ?? ""
            SyncUtil.addCollection(id: model.roomId,
                                   className: SceneType.pkInfo.rawValue,
                                   params: JSONObject.toJson(pkInfo),
                                   delegate: PKInfoAddDataDelegate(vc: vc))
            
        } else if model.status == .refuse && "\(UserInfo.userId)" == model.userId {
            vc.showAlert(title: "PK_Invite_Reject".localized, message: "")
        }
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted target invite == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed target invite", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError target invite code ==\(code) msg == \(msg)", level: .error)
    }
}

class PKInfoAddDataDelegate: IObjectDelegate {
    private var vc: LivePlayerController
    init(vc: LivePlayerController) {
        self.vc = vc
    }
    
    func onSuccess(result: IObject) {
        guard let model = JSONObject.toModel(PKInfoModel.self, value: result.toJson()) else { return }
        vc.pkInfoModel = model
    }
    
    func onFailed(code: Int, msg: String) {
        
    }
}
