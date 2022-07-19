//
//  SuperAppInvitationSheet.swift
//  Scene-Examples
//
//  Created by ZYP on 2021/12/31.
//

import Foundation

protocol CDNInvitationSheetManagerDelegate: NSObjectProtocol {
    func CDNInvitationSheetManagerDidFetch(infos: [CDNInvitationSheetManager.Info])
}

class CDNInvitationSheetManager: NSObject {
    typealias Info = CDNInvitationViewCell.Info
    private var syncUtil: CDNSyncUtil!
    weak var delegate: CDNInvitationSheetManagerDelegate?
    
    init(syncUtil: CDNSyncUtil) {
        self.syncUtil = syncUtil
        super.init()
    }
    
    func fetchInfos() {
        syncUtil.getMembers { [weak self](strings) in
            guard let `self` = self else { return }
            
            let localUserId = CDNStorageManager.uuid
            let decoder = JSONDecoder()
            var userInfos = strings.compactMap({ $0.data(using: .utf8) })
                .compactMap({ try? decoder.decode(CDNUserInfo.self, from: $0) })
                .filter({ $0.userId != localUserId })
            
            /// 查重
            var dict = [String : CDNUserInfo]()
            for userInfo in userInfos {
                dict[userInfo.userId] = userInfo
            }
            userInfos = dict.map({ $0.value })
            
            var infos = [Info]()
            for (index, userInfo) in userInfos.enumerated() {
                let info = Info(idnex: index,
                                title: userInfo.userName,
                                imageName: userInfo.userId.headImageName,
                                isInvited: false, userId: userInfo.userId)
                infos.append(info)
            }
            self.update(infos: infos)
        } fail: { [weak self](error) in
            LogUtils.log(message: error.localizedDescription, level: .error)
            self?.update(infos: [])
        }
    }
    
    private func update(infos: [Info]) {
        delegate?.CDNInvitationSheetManagerDidFetch(infos: infos)
    }
}
