//
//  SuperAppSyncUtil.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/28.
//

import Foundation

protocol SuperAppSyncUtilDelegate: NSObjectProtocol {
    /// 自己上麦了
    func superAppSyncUtilDidPkAcceptForMe(util: SuperAppSyncUtil, userIdPK: String)
    /// 自己下麦了
    func superAppSyncUtilDidPkCancleForMe(util: SuperAppSyncUtil)
    /// 别人上麦了
    func superAppSyncUtilDidPkAcceptForOther(util: SuperAppSyncUtil)
    /// 别人下麦了
    func superAppSyncUtilDidPkCancleForOther(util: SuperAppSyncUtil)
    /// 房间关闭了
    func superAppSyncUtilDidSceneClose(util: SuperAppSyncUtil)
}

class SuperAppSyncUtil {
    private let appId: String
    /// 场景所在的默认房间id
    private let defaultScenelId = "PKByCDN"
    /// 房间id
    private let sceneId: String
    /// 房间名称 用于显示
    private let sceneName: String
    private let userId: String
    private let userName: String
    private var sceneRef: SceneReference?
    private var manager: AgoraSyncManager?
    private var currentMemberId: String!
    fileprivate var lastUserIdPKValue = ""
    let queue = DispatchQueue(label: "queue.SuperAppSyncUtil")
    
    typealias CompltedBlock = (LocalizedError?) -> ()
    typealias SuccessBlockStrings = ([String]) -> ()
    typealias SuccessBlockString = (String) -> ()
    typealias FailBlockLocalizedError = (LocalizedError) -> ()
    weak var delegate: SuperAppSyncUtilDelegate?
    
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
    
    func joinByAudience(roomItem: SuperAppRoomInfo,
                        complted: CompltedBlock? = nil) {
        queue.async { [weak self] in
            do {
                try self?.joinScene(roomInfo: roomItem)
                try self?.addMember()
                complted?(nil)
            } catch let error {
                let e = error as! LocalizedError
                complted?(e)
            }
        }
    }
    
    func joinByHost(roomInfo: SuperAppRoomInfo,
                    complted: CompltedBlock? = nil) {
        queue.async { [weak self] in
            do {
                try self?.joinScene(roomInfo: roomInfo)
                self?.resetPKInfo()
                try self?.addMember()
                complted?(nil)
            } catch let error {
                let e = error as! LocalizedError
                complted?(e)
            }
        }
    }
    
    private func joinScene(roomInfo: SuperAppRoomInfo) throws {
        let semp = DispatchSemaphore(value: 0)
        var error: Error?
        
        /// create
        let config = AgoraSyncManager.RtmConfig(appId: appId, channelName: defaultScenelId)
        manager = AgoraSyncManager(config: config, complete: { code in
            if code != 0 {
                let msg = "AgoraSyncManager init fail \(code)"
                LogUtils.log(message: msg, level: .error)
                error = SyncError(message: msg, code: code)
            }
            semp.signal()
        })
        semp.wait()
        
        /// join
        let property = roomInfo.dict
        let scene = Scene(id: sceneId,
                          userId: userId,
                          property: property)
        manager?.createScene(scene: scene) {
            LogUtils.log(message: "joinScene success", level: .info)
            semp.signal()
        } fail: { e in
            error = e
            semp.signal()
            let msg = "joinScene fail \(e.description)"
            LogUtils.log(message: msg, level: .error)
        }
        semp.wait()
        
        if let e = error {
            throw e
        }
    }
    
    private func addMember() throws { /** 把本地用户添加到人员列表 **/
        let userInfo = SuperAppUserInfo(userId: userId,
                                        userName: userName)
        let semp = DispatchSemaphore(value: 0)
        var error: SyncError?
        sceneRef?.collection(className: "member")
            .add(data: userInfo.dict) { [weak self](obj) in
                LogUtils.log(message: "addMember success", level: .info)
                let id = obj.getId()
                self?.currentMemberId = id
                semp.signal()
            } fail: { (e) in
                let msg = "addMember fail: \(e.errorDescription ?? "")"
                LogUtils.log(message: msg, level: .error)
                error = e
                semp.signal()
            }
        semp.wait()
        if let e = error {
            throw e
        }
    }
    
    func getMembers(success: @escaping SuccessBlockStrings,
                    fail: FailBlockLocalizedError?) {
        sceneRef?.collection(className: "member")
            .get(success: { objs in
                let members = objs.compactMap({ $0.toJson() })
                success(members)
            }, fail: fail)
    }
    
    func subscribePKInfo() {
        sceneRef?.subscribe(key: "",
                           onUpdated: onPkInfoUpdated(object:),
                           onDeleted: onPkInfoDeleted(object:),
                           fail: { error in
            let msg = "subscribePKInfo fail: \(error.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        })
    }
    
    func unsubscribePKInfo() {
        sceneRef?.unsubscribe(key: "")
    }
    
    func updatePKInfo(userIdPK: String) { /** only host can invoke pk **/
        if userIdPK.count == 0 {
            fatalError("muts no empty string")
        }
        
        let property = SuperAppPKInfo(userIdPK: userIdPK).dict
        sceneRef?.update(key: "",
                        data: property) { obj in
            LogUtils.log(message: "updatePKInfo success)", level: .info)
        } fail: { error in
            let msg = "updatePKInfo fail: \(error.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        }
    }
    
    func resetPKInfo() { /** host and audience can invoke cancle pk **/
        let property = SuperAppPKInfo(userIdPK: "").dict
        sceneRef?.update(key: "", data: property) { _ in
            LogUtils.log(message: "resetPKInfo success)", level: .info)
        } fail: { error in
            let msg = "resetPKInfo fail: \(error.errorDescription ?? "")"
            LogUtils.log(message: msg, level: .error)
        }
    }
    
    func getPKInfo(success: @escaping SuccessBlockString,
                     fail: @escaping FailBlockLocalizedError) {
        sceneRef?.get(key: "", success: {  obj in
            if let data = obj?.toJson()?.data(using: .utf8) {
                let decoder = JSONDecoder()
                do {
                    let roomInfo = try decoder.decode(SuperAppPKInfo.self, from: data)
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
            sceneRef?.collection(className: "member")
                .document(id: id)
                .delete(success: nil, fail: nil)
        }
        resetPKInfo()
    }
    
    func leaveByHost() {
        unsubscribePKInfo()
        sceneRef?.delete(success: nil, fail: nil)
        if let id = currentMemberId {
            sceneRef?.collection(className: "member")
                .document(id: id)
                .delete(success: nil, fail: nil)
        }
        resetPKInfo()
    }
}

extension SuperAppSyncUtil {
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

extension SuperAppSyncUtil {
    func invokeDidPkAcceptForMe(userIdPK: String) {
        if Thread.isMainThread {
            delegate?.superAppSyncUtilDidPkAcceptForMe(util: self,
                                                  userIdPK: userIdPK)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.superAppSyncUtilDidPkAcceptForMe(util: self,
                                                       userIdPK: userIdPK)
        }
    }
    
    func invokeDidPkCancleForMe() {
        if Thread.isMainThread {
            delegate?.superAppSyncUtilDidPkCancleForMe(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.superAppSyncUtilDidPkCancleForMe(util: self)
        }
    }
    
    func invokeDidPkAcceptForOther() {
        if Thread.isMainThread {
            delegate?.superAppSyncUtilDidPkAcceptForOther(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.superAppSyncUtilDidPkAcceptForOther(util: self)
        }
    }
    
    func invokeDidPkCancleForOther() {
        if Thread.isMainThread {
            delegate?.superAppSyncUtilDidPkCancleForOther(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.superAppSyncUtilDidPkCancleForMe(util: self)
        }
    }
    
    func invokeDidSceneClose() {
        if Thread.isMainThread {
            delegate?.superAppSyncUtilDidSceneClose(util: self)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.superAppSyncUtilDidSceneClose(util: self)
        }
    }
}

extension String {
    fileprivate static var defaultLogTag: String {
        return "SuperAppSyncUtil"
    }
}
