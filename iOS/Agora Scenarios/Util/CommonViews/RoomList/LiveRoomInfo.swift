//
//  LiveRoomInfo.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit

struct LiveRoomInfo: Codable {
    var roomName: String = ""
    var roomId: String = "\(arc4random_uniform(899999) + 100000)"
    var userId: String = "\(UserInfo.userId)"
    var backgroundId: String = String(format: "portrait%02d", Int.random(in: 1...2))
    var objectId: String?
    var videoUrl: String? = "https://webdemo-pull-hdl.agora.io/lbhd/sample1.flv"
}
