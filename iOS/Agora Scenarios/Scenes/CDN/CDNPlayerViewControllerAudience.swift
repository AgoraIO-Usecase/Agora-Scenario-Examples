//
//  SuperAppPlayerViewControllerAudience.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/28.
//

import UIKit
import AgoraRtcKit

class CDNPlayerViewControllerAudience: BaseViewController {
    let mainView = CDNMainView()
    var syncUtil: CDNSyncUtil!
    var pushUrlString: String!
    var pullUrlString: String!
    var agoraKit: AgoraRtcEngineKit!
    var mediaPlayer: AgoraRtcMediaPlayerProtocol!
    var config: Config!
    var mode: Mode = .pull
    
    public init(config: Config) {
        super.init(nibName: nil, bundle: nil)
        self.config = config
        self.pushUrlString = "rtmp://examplepush.agoramde.agoraio.cn/live/" + config.sceneId
        self.pullUrlString = "http://examplepull.agoramde.agoraio.cn/live/\(config.sceneId).flv"
        
        let userId = CDNStorageManager.uuid
        let userName = CDNStorageManager.userName
        self.syncUtil = CDNSyncUtil(appId: config.appId,
                                         sceneId: config.sceneId,
                                         sceneName: config.sceneName,
                                         userId: userId,
                                         userName: userName)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        LogUtils.log(message: "deinit", level: .info)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        
        syncUtil.delegate = self
        syncUtil.joinByAudience(roomItem: config.roomInfo) { [weak self](error) in /** 1. join sync scene **/
            guard let `self`  = self else { return }
            if let e = error {
                let msg = "joinByAudience fail: \(e.errorDescription ?? "")"
                LogUtils.log(message: msg, level: .error)
                return
            }
            LogUtils.log(message: "joinByAudience success", level: .info)
            
            self.syncUtil.getPKInfo { [weak self](userPkId) in
                
                if userPkId == "", self?.config.roomInfo.liveMode == .push { /** no pk, live mode can be change **/
                    self?.initMediaPlayer(useAgoraCDN: true)
                    self?.syncUtil.subscribePKInfo()
                    return
                }
                
                if userPkId == "", self?.config.roomInfo.liveMode == .byPassPush { /** no pk, live mode can not be change **/
                    self?.initMediaPlayer(useAgoraCDN: false)
                    self?.syncUtil.subscribePKInfo()
                    return
                }
                
                if userPkId != CDNStorageManager.uuid { /** pk no me **/
                    self?.initMediaPlayer(useAgoraCDN: false)
                    self?.syncUtil.subscribePKInfo()
                    return
                }
                
                if userPkId == CDNStorageManager.uuid { /** pk me **/
                    self?.mode = .rtc
                    self?.joinRtc()
                    self?.syncUtil.subscribePKInfo()
                    return
                }
            } fail: { [weak self](error) in
                self?.show(error.localizedDescription)
                self?.showOpeFailAlert()
            }
        }
    }
    
    private func setupUI() {
        mainView.setPersonViewHidden(hidden: true)
        view.addSubview(mainView)
        mainView.frame = view.bounds
        mainView.delegate = self
        let imageName = CDNStorageManager.uuid.headImageName
        let info = CDNMainView.Info(title: config.sceneName + "(\(config.sceneId))",
                                 imageName: imageName,
                                 userCount: 0)
        mainView.update(info: info)
    }
    
    func changeToRtc() { /** 切换rtc模式 **/
        LogUtils.log(message: "切换到rtc模式", level: .info)
        mediaPlayer.stop()
        agoraKit.destroyMediaPlayer(mediaPlayer)
        mediaPlayer = nil
        mode = .rtc
        
        joinRtc()
    }
    
    func changeToPull() { /** 切换到拉流模式 **/
        LogUtils.log(message: "切换到拉流模式", level: .info)
        mode = .pull
        leaveRtc()
        
        if config.roomInfo.liveMode == .byPassPush {
            initMediaPlayer(useAgoraCDN: false)
        }
        else {
            initMediaPlayer(useAgoraCDN: true)
        }
        mainView.setRemoteViewHidden(hidden: true)
    }
    
    func showCloseAlert() {
        let vc = UIAlertController(title: "提示", message: "房间已关闭", preferredStyle: .alert)
        vc.addAction(.init(title: "确定", style: .default, handler: { [unowned self](_) in
            self.destroy()
            self.dismiss(animated: true, completion: nil)
        }))
        present(vc, animated: true, completion: nil)
    }
    
    func showOpeFailAlert() {
        let vc = UIAlertController(title: "提示", message: "打开失败，请重试", preferredStyle: .alert)
        vc.addAction(.init(title: "确定", style: .default, handler: { [unowned self](_) in
            self.destroy()
            self.dismiss(animated: true, completion: nil)
        }))
        present(vc, animated: true, completion: nil)
    }
    
    private func destroy() {
        syncUtil.leaveByAudience()
        destroyRtc()
    }
}

// MRK: - SuperAppSyncUtilDelegate
extension CDNPlayerViewControllerAudience: CDNSyncUtilDelegate {
    func CDNSyncUtilDidPkAcceptForMe(util: CDNSyncUtil, userIdPK: String) {
        LogUtils.log(message: "收到上麦申请", level: .info)
        changeToRtc()
    }
    
    func CDNSyncUtilDidPkCancleForMe(util: CDNSyncUtil) {
        LogUtils.log(message: "下麦", level: .info)
        changeToPull()
    }
    
    func CDNSyncUtilDidPkAcceptForOther(util: CDNSyncUtil) {
        if config.roomInfo.liveMode == .push {
            initMediaPlayer(useAgoraCDN: false)
        }
    }
    
    func CDNSyncUtilDidPkCancleForOther(util: CDNSyncUtil) {
        if config.roomInfo.liveMode == .push {
            initMediaPlayer(useAgoraCDN: true)
        }
    }
    
    func CDNSyncUtilDidSceneClose(util: CDNSyncUtil) { /** scene was delete **/
        showCloseAlert()
    }
}

// MARK: - UI Event MainViewDelegate
extension CDNPlayerViewControllerAudience: SuperAppMainViewDelegate {
    func mainView(_ view: CDNMainView, didTap action: CDNMainView.Action) {
        switch action {
        case .close:
            destroy()
            dismiss(animated: true, completion: nil)
            return
        case .closeRemote:
            syncUtil.resetPKInfo()
            return
        case .member, .more:
            break
        }
    }
}

// MARK: - Data Struct
extension CDNPlayerViewControllerAudience {
    struct Config {
        let appId: String
        let roomInfo: CDNRoomInfo
        
        var sceneName: String {
            return roomInfo.roomName
        }
        
        var sceneId: String {
            return roomInfo.roomId
        }
    }
    
    enum Mode {
        /// 拉流模式
        case pull
        /// rtc模式
        case rtc
    }
}
