//
//  SuperAppSyncUtil.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/28.
//

import Foundation

protocol CDNSyncUtilDelegate: NSObjectProtocol {
    /// 自己上麦了
    func CDNSyncUtilDidPkAcceptForMe(util: CDNSyncUtil, userIdPK: String)
    /// 自己下麦了
    func CDNSyncUtilDidPkCancleForMe(util: CDNSyncUtil)
    /// 别人上麦了
    func CDNSyncUtilDidPkAcceptForOther(util: CDNSyncUtil)
    /// 别人下麦了
    func CDNSyncUtilDidPkCancleForOther(util: CDNSyncUtil)
    /// 房间关闭了
    func CDNSyncUtilDidSceneClose(util: CDNSyncUtil)
}

class CDNSyncUtil {
    private let appId: String
    /// 场景所在的默认房间id
    private let defaultScenelId = "PKByCDN"
    /// 房间id
    private let sceneId: String
    /// 房间名称 用于显示
    private let sceneName: String
    private let userId: String
    private let userName: String
    private var currentMemberId: String!
    fileprivate var lastUserIdPKValue = ""
    let queue = DispatchQueue(label: "queue.SuperAppSyncUtil")
    
    typealias CompltedBlock = (LocalizedError?) -> ()
    typealias SuccessBlockStrings = ([String]) -> ()
    typealias SuccessBlockString = (String) -> ()
    typealias FailBlockLocalizedError = (LocalizedError) -> ()
    weak var delegate: CDNSyncUtilDelegate?
    
    init(appId: String,
         sceneId: String,
         sceneName: String,
         userId: String,
         userName: String) {
        self.appId = appId
        self.sceneId = sceneId
        self.sceneName = sceneName
        self.userId = userId
        self.userName = userName
    }
    
    func joinByAudience(roomItem: CDNRoomInfo,
                        complted: CompltedBlock? = nil) {
        queue.async { [weak self] in
            self?.joinScene(roomInfo: roomItem, completion: {
                self?.addMember()
                complted?(nil)
            })
        }
    }
    
    func joinByHost(roomInfo: CDNRoomInfo,
                    complted: CompltedBlock? = nil) {
        queue.async { [weak self] in
            self?.joinScene(roomInfo: roomInfo, completion: {
                self?.resetPKInfo()
                self?.addMember()
                complted?(nil)
            })
        }
    }
    
    private func joinScene(roomInfo: CDNRoomInfo, completion: @escaping () -> Void) {
        SyncUtil.joinScene(id: sceneId, userId: userId, property: roomInfo.dict) { _ in
            completion()
        } fail: { e in
            let msg = "join Scene fail: \(e.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        }
    }
    
    private func addMember() { /** 把本地用户添加到人员列表 **/
        let userInfo = CDNUserInfo(userId: userId,
                                        userName: userName)
        SyncUtil.scene(id: sceneId)?.collection(className: "member").add(data: userInfo.dict, success: { obj in
            LogUtils.log(message: "addMember success", level: .info)
            let id = obj.getId()
            self.currentMemberId = id
        }, fail: { e in
            let msg = "addMember fail: \(e.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        })
    }
    
    func getMembers(success: @escaping SuccessBlockStrings,
                    fail: FailBlockLocalizedError?) {
        SyncUtil.scene(id: sceneId)?.collection(className: "member").get(success: { objs in
            let members = objs.compactMap({ $0.toJson() })
            success(members)
        }, fail: fail)
    }
    
    func subscribePKInfo() {
        SyncUtil.scene(id: sceneId)?.subscribe(key: "", onUpdated: onPkInfoUpdated(object:), onDeleted: onPkInfoDeleted(object:), fail: { error in
            let msg = "subscribePKInfo fail: \(error.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        })
    }
    
    func unsubscribePKInfo() {
        SyncUtil.scene(id: sceneId)?.unsubscribe(key: "")
    }
    
    func updatePKInfo(userIdPK: String) { /** only host can invoke pk **/
        if userIdPK.count == 0 {
            fatalError("muts no empty string")
        }
        
        let property = CDNPKInfo(userIdPK: userIdPK).dict
        SyncUtil.scene(id: sceneId)?.update(key: "", data: property, success: { obj in
            LogUtils.log(message: "updatePKInfo success)", level: .info)
        }, fail: { error in
            let msg = "updatePKInfo fail: \(error.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        })
    }
    
    func resetPKInfo() { /** host and audience can invoke cancle pk **/
        let property = CDNPKInfo(userIdPK: "").dict
        SyncUtil.scene(id: sceneId)?.update(key: "", data: property, success: { _ in
            LogUtils.log(message: "resetPKInfo success)", level: .info)
        }, fail: { error in
            let msg = "resetPKInfo fail: \(error.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        })
    }
    
    func getPKInfo(success: @escaping SuccessBlockString,
                     fail: @escaping FailBlockLocalizedError) {
        SyncUtil.scene(id: sceneId)?.get(key: "", success: { obj in
            if let data = obj?.toJson()?.data(using: .utf8) {
                let decoder = JSONDecoder()
                do {
                    let roomInfo = try decoder.decode(CDNPKInfo.self, from: data)
                    success(roomInfo.userIdPK)
                } catch let error {
                    fail(error as! LocalizedError)
                }
            }
        }, fail: fail)
    }
    
    func leaveByAudience() {
        unsubscribePKInfo()
        if let id = currentMemberId {
            SyncUtil.scene(id: sceneId)?.collection(className: "member").document(id: id).delete(success: nil, fail: nil)
        }
        resetPKInfo()
    }
    
    func leaveByHost() {
        unsubscribePKInfo()
        SyncUtil.scene(id: sceneId)?.deleteScenes()
        if let id = currentMemberId {
            SyncUtil.scene(id: sceneId)?.collection(className: "member").document(id: id).delete(success: nil, fail: nil)
        }
        resetPKInfo()
    }
}

extension CDNSyncUtil {
    func onPkInfoUpdated(object: IObject) {
        if let userIdPK = object.getPropertyWith(key: "userIdPK", type: String.self) as? String {
            guard lastUserIdPKValue != userIdPK else { /** filter same **/
                return
            }
            
            if userIdPK.count > 0, userIdPK == userId {
                lastUserIdPKValue = userIdPK
                invokeDidPkAcceptForMe(userIdPK: userIdPK)
                return
            }
            
            if userIdPK.count > 0, userIdPK != userId {
                lastUserIdPKValue = userIdPK
                invokeDidPkAcceptForOther()
                return
            }
            
            if userIdPK.count == 0, lastUserIdPKValue == userId {
                lastUserIdPKValue = userIdPK
                invokeDidPkCancleForMe()
                return
            }
            
            if userIdPK.count == 0, lastUserIdPKValue != userId {
                lastUserIdPKValue = userIdPK
                invokeDidPkCancleForOther()
                return
            }
        }
    }
    
    func onPkInfoDeleted(object: IObject) {
        if object.getId() == sceneId {
            invokeDidSceneClose()
        }
    }
}

extension CDNSyncUtil {
    func invokeDidPkAcceptForMe(userIdPK: String) {
        if Thread.isMainThread {
            delegate?.CDNSyncUtilDidPkAcceptForMe(util: self,
                                                  userIdPK: userIdPK)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.CDNSyncUtilDidPkAcceptForMe(util: self,
                                                       userIdPK: userIdPK)
        }
    }
    
    func invokeDidPkCancleForMe() {
        if Thread.isMainThread {
            delegate?.CDNSyncUtilDidPkCancleForMe(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.CDNSyncUtilDidPkCancleForMe(util: self)
        }
    }
    
    func invokeDidPkAcceptForOther() {
        if Thread.isMainThread {
            delegate?.CDNSyncUtilDidPkAcceptForOther(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.CDNSyncUtilDidPkAcceptForOther(util: self)
        }
    }
    
    func invokeDidPkCancleForOther() {
        if Thread.isMainThread {
            delegate?.CDNSyncUtilDidPkCancleForOther(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.CDNSyncUtilDidPkCancleForMe(util: self)
        }
    }
    
    func invokeDidSceneClose() {
        if Thread.isMainThread {
            delegate?.CDNSyncUtilDidSceneClose(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.CDNSyncUtilDidSceneClose(util: self)
        }
    }
}

extension String {
    fileprivate static var defaultLogTag: String {
        return "SuperAppSyncUtil"
    }
}
