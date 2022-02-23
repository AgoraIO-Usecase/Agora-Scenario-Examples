//
//  GameInfoModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/22.
//

import UIKit

struct GameInfoModel: Codable {
    var objectId: String? = ""
    
    var status: GameStatus = .no_start
    
    var gameUid: String? = ""
    
    var roomId: String? = ""
    
    var gameId: String? = ""//GameCenterType? = .guess
    
    var timestamp: String? = "".timeStamp16
    
    var sources: GameSourcesType? = .yuanqi
}
