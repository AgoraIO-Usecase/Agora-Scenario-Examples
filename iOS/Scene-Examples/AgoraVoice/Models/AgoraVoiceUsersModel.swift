//
//  AgoraVoiceUsersModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/27.
//

import UIKit

struct AgoraVoiceUsersModel: Codable {
    var userName: String = "User-\(UserInfo.uid)"
    var avatar: String = String(format: "portrait%02d", Int.random(in: 1...14))
    var userId: String = UserInfo.uid
    var status: PKApplyInfoStatus = .end
    var timestamp: String = "".timeStamp16
    var isEnableVideo: Bool? = false
    var isEnableAudio: Bool? = false
    var objectId: String?
}
