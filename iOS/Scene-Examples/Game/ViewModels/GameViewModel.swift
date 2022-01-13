//
//  GameViewModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/22.
//

import UIKit

class GameViewModel: NSObject {
    static let shared = GameViewModel(channleName: "", ownerId: "")
    var channelName: String = ""
    
    private var ownerId: String = ""
    private var gameList: [GameCenterModel]?
    init(channleName: String, ownerId: String) {
        self.channelName = channleName
        self.ownerId = ownerId
    }
    
    func getGameList(sceneType: SceneType, success: @escaping ([GameCenterModel]?) -> Void) {
        if gameList != nil {
            success(gameList)
            return
        }
        NetworkManager.shared.postRequest(urlString: "getGames", params: ["type": sceneType.gameType]) { response in
            let result = response["result"] as? [[String: Any]]
            let list = result?.compactMap({ JSONObject.toModel(GameCenterModel.self, value: $0) })
            success(list)
            self.gameList = list
        } failure: { error in
            LogUtils.log(message: "error == \(error)", level: .error)
        }
    }
    
    func joinGame(gameId: String, roomId: String, identity: String, avatar: String, success: @escaping (String) -> Void) {
        let params: [String: Any] = ["user_id": UserInfo.userId,
                                     "app_id": gameId,
                                     "room_id": roomId,
                                     "identity": identity,
                                     "token": KeyCenter.gameToken,
                                     "name": "User-\(UserInfo.uid)",
                                     "avatar": avatar]
        NetworkManager.shared.postRequest(urlString: "getJoinUrl", params: params) { response in
            let result = response["result"] as? String
            success(result ?? "")
        } failure: { error in
            LogUtils.log(message: "error == \(error)", level: .error)
        }
    }
    
    /// 发送礼物
    func postGiftHandler(gameId: String, giftType: LiveGiftModel.GiftType) {
        postGiftHandler(gameId: gameId, giftType: giftType, playerId: ownerId)
    }
    func postGiftHandler(gameId: String, giftType: LiveGiftModel.GiftType, playerId: String) {
        var params: [String: Any] = ["user_id": UserInfo.userId,
                                     "app_id": gameId,
                                     "room_id": Int(channelName) ?? 0,
                                     "name": "User-\(UserInfo.userId)",
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "gift": giftType.rawValue,
                                     "count": 1,
                                     "player": Int(playerId) ?? 0,
                                     "nonce_str": "".timeStamp16]
        let sign = NetworkManager.shared.generateSignature(params: params,
                                                           token: KeyCenter.gameAppSecrets)
        params["sign"] = sign
        
        NetworkManager.shared.postRequest(urlString: "gift", params: params) { result in
            LogUtils.log(message: "gift == \(result)", level: .info)
        } failure: { error in
            LogUtils.log(message: "error == \(error)", level: .error)
        }
    }
    
    /// 发弹幕
    func postBarrage(gameId: String) {
        postBarrage(gameId: gameId, playerId: ownerId)
    }
    func postBarrage(gameId: String, playerId: String) {
        let barrage = GameBarrageType.allCases.randomElement() ?? .salvo
        var params: [String: Any] = ["user_id": "\(UserInfo.userId)",
                                     "app_id": gameId,
                                     "room_id": channelName,
                                     "name": "User-\(UserInfo.userId)",
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "nonce_str": "".timeStamp,
                                     "barrage": barrage.rawValue,
                                     "count": 1,
                                     "player": playerId]
        let sign = NetworkManager.shared.generateSignature(params: params,
                                                           token: KeyCenter.gameAppSecrets)
        params["sign"] = sign
        NetworkManager.shared.postRequest(urlString: "barrage", params: params, success: { result in
            LogUtils.log(message: "barrge == \(result)", level: .info)
        }, failure: { error in
            LogUtils.log(message: "error == \(error)", level: .error)
        })
    }
    
    /// 离开游戏
    func leaveGame(gameId: String, roleType: GameRoleType) {
        var params: [String: Any] = ["user_id": UserInfo.userId,
                                     "app_id": gameId,
                                     "identity": roleType.rawValue,
                                     "room_id": channelName,
                                     "token": KeyCenter.gameToken,
                                     "timestamp": "".timeStamp,
                                     "nonce_str": "".timeStamp16]
        let sign = NetworkManager.shared.generateSignature(params: params,
                                                           token: KeyCenter.gameAppSecrets)
        params["sign"] = sign
        
        NetworkManager.shared.postRequest(urlString: "leaveGame", params: params) { result in
            print("result == \(result)")
        } failure: { error in
            print("error == \(error)")
        }
    }
    
    func changeRole(gameId: String, oldRole: GameRoleType, newRole: GameRoleType) {
        let params: [String: Any] = ["user_id": UserInfo.userId,
                                     "app_id": gameId,
                                     "room_id": channelName,
                                     "oldRole": oldRole.rawValue,
                                     "newRole": newRole.rawValue]
    
        NetworkManager.shared.postRequest(urlString: "changeRole", params: params) { result in
            print("result == \(result)")
        } failure: { error in
            print("error == \(error)")
        }
    }
}
