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
            let channelName = model.targetUserId == "\(UserInfo.userId)" ? model.roomId : model.targetRoomId
            vc.leaveChannel(uid: UserInfo.userId, channelName: channelName ?? "")
            vc.updateLiveLayout(postion: .full)
            
            // 删除PK邀请数据
            SyncUtil.deleteCollection(id: model.targetRoomId ?? "",
                                      className: vc.sceneType.rawValue,
                                      delegate: nil)
            
            pkLiveEndClosure?(model)
            
            guard var pkInfoModel = vc.pkInfoModel else {
                return
            }
            pkInfoModel.status = .end
            SyncUtil.updateCollection(id: vc.channleName,
                                      className: SYNC_MANAGER_PK_INFO,
                                      objectId: pkInfoModel.objectId,
                                      params: JSONObject.toJson(pkInfoModel),
                                      delegate: nil)
            
        } else if model.status == .accept {
            vc.updateLiveLayout(postion: .center)
            // 把自己加入到对方的channel
            let channelName = model.targetUserId == "\(UserInfo.userId)" ? model.roomId : model.targetRoomId
            let userId = model.userId == vc.currentUserId ? model.targetUserId : model.userId
            vc.joinAudienceChannel(channelName: channelName ?? "", pkUid: UInt(userId ?? "0") ?? 0)
            
            pkLiveStartClosure?(model)
            
            // 通知观众加入到pk的channel
            var pkInfo = PKInfoModel()
            pkInfo.status = model.status
            pkInfo.roomId = channelName ?? ""
            pkInfo.userId = model.userId
            SyncUtil.addCollection(id: vc.channleName,
                                   className: SYNC_MANAGER_PK_INFO,
                                   params: JSONObject.toJson(pkInfo),
                                   delegate: PKInfoAddDataDelegate(vc: vc))
            
        } else if model.status == .invite && "\(UserInfo.userId)" != model.userId {
            let message = vc.sceneType == .game ? "您的好友\(model.userName)邀请\n您加入\(model.gameId.title)游戏" : ""
            vc.showAlert(title: vc.sceneType.alertTitle, message: message) { [weak self] in
                guard let self = self else { return }
                model.status = .refuse
                SyncUtil.updateCollection(id: model.targetRoomId ?? "",
                                          className: self.vc.sceneType.rawValue,
                                          objectId: model.objectId,
                                          params: JSONObject.toJson(model),
                                          delegate: nil)
                
            } confirm: { [weak self] in
                guard let self = self else { return }
                model.status = .accept
                SyncUtil.updateCollection(id: model.targetRoomId ?? "",
                                          className: self.vc.sceneType.rawValue,
                                          objectId: model.objectId,
                                          params: JSONObject.toJson(model),
                                          delegate: nil)
            }
        } else if model.status == .refuse && "\(UserInfo.userId)" == model.userId {
            vc.showAlert(title: "PK_Invite_Reject".localized, message: "")
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
