//
//  PKApplyInfoModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit

enum PKApplyInfoStatus: Int, Codable {
    /// 邀请中
    case invite = 1
    ///  已接受
    case accept = 2
    /// 已拒绝
    case refuse = 3
    /// 已结束
    case end = 4
}

struct PKApplyInfoModel: Codable {
    var objectId: String = ""
    /// 用户id
    var userId: String = "\(UserInfo.userId)"
    /// 对方的UserID
    var targetUserId: String?
    /// 用户名
    var userName: String = "User-\(UserInfo.userId)"
    /// pk邀请状态
    var status: PKApplyInfoStatus = .invite
    /// 自己的房间ID
    var roomId: String = ""
    /// 对方直播间的roomId
    var targetRoomId: String?
    
    var timestamp: String? = "".timeStamp16
}

struct PKInfoModel: Codable {
    var objectId: String = ""
    /// pk邀请状态
    var status: PKApplyInfoStatus = .invite
    /// 发起pk的主播房间id, 对应rtc的channel
    var roomId: String = ""
    /// 用户id
    var userId: String = "\(UserInfo.userId)"
    
    var timestamp: String? = "".timeStamp16
}
