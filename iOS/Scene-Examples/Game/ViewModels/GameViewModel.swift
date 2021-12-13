//
//  GameViewModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/22.
//

import UIKit

class GameViewModel: NSObject {
    var channelName: String = ""
    private var ownerId: String = ""
    init(channleName: String, ownerId: String) {
        self.channelName = channleName
        self.ownerId = ownerId
    }
    
    /// 发送礼物
    func postGiftHandler(type: LiveGiftModel.GiftType) {
        var params: [String: Any] = ["user_id": UserInfo.userId,
                                     "app_id": Int(KeyCenter.gameAppId) ?? 0,
                                     "room_id": Int(channelName) ?? 0,
                                     "name": "User-\(UserInfo.userId)",
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "gift": type.rawValue,
                                     "count": 1,
                                     "player": ownerId,
                                     "nonce_str": "".timeStamp16]
        let sign = NetworkManager.shared.generateSignature(params: params,
                                                           token: KeyCenter.gameAppSecrets)
        params["sign"] = sign
        
        NetworkManager.shared.postRequest(urlString: "http://testgame.yuanqihuyu.com/guess/gift", params: params) { result in
            LogUtils.log(message: "gift == \(result)", level: .info)
        } failure: { error in
            LogUtils.log(message: "error == \(error)", level: .error)
        }
    }
    
    /// 发弹幕
    func postBarrage() {
        let barrage = GameBarrageType.allCases.randomElement() ?? .salvo
        let params: [String: Any] = ["user_id": "\(UserInfo.userId)",
                                     "app_id": KeyCenter.gameAppId,
                                     "room_id": channelName,
                                     "name": "User-\(UserInfo.userId)",
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "nonce_str": "".timeStamp,
                                     "barrage": barrage.rawValue,
                                     "count": 1,
                                     "player": ownerId,
                                     "sign": KeyCenter.gameAppSecrets]
        NetworkManager.shared.postRequest(urlString: "http://testgame.yuanqihuyu.com/guess/barrage", params: params, success: { result in
            LogUtils.log(message: "barrge == \(result)", level: .info)
        }, failure: { error in
            LogUtils.log(message: "error == \(error)", level: .error)
        })
    }
    
    /// 离开游戏
    func leaveGame(roleType: GameRoleType) {
        var params: [String: Any] = ["user_id": UserInfo.userId,
                                     "app_id": Int(KeyCenter.gameAppId) ?? 0,
                                     "identity": roleType.rawValue,
                                     "room_id": Int(channelName) ?? 0,
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "nonce_str": "".timeStamp16]
        let sign = NetworkManager.shared.generateSignature(params: params,
                                                           token: KeyCenter.gameAppSecrets)
        params["sign"] = sign
        
        NetworkManager.shared.postRequest(urlString: "http://testgame.yuanqihuyu.com/guess/leave", params: params) { result in
            print("result == \(result)")
        } failure: { error in
            print("error == \(error)")
        }
    }
}
