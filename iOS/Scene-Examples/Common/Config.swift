//
//  Config.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/4.
//

import Foundation

let SYNC_MANAGER_PARAM_KEY_ID = "defaultChannel"
/// 子房间名
let SYNC_COLLECTION_SUB_ROOM = "SubRoom"

let SYNC_MANAGER_PARAM_KEY_APPID = "appId"
/// 礼物
let SYNC_MANAGER_GIFT_INFO = "giftInfo"
/// PK游戏信息
let SYNC_MANAGER_GAME_APPLY_INFO = "GameApplyInfo"
/// 观众游戏信息
let SYNC_MANAGER_GAME_INFO = "GameInfo"
/// pk信息
let SYNC_MANAGER_PK_INFO = "PKInfo"

struct UserInfo {
    static var userId: UInt {
        let id = UserDefaults.standard.integer(forKey: "UserId")
        if id > 0 {
            return UInt(id)
        }
        let user = UInt(arc4random_uniform(8999999) + 1000000)
        UserDefaults.standard.set(user, forKey: "UserId")
        UserDefaults.standard.synchronize()
        return user
    }
}
