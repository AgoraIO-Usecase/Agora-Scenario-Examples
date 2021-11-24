//
//  GameInfoModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/22.
//

import UIKit

enum GameStatus: Int, Codable {
    /// 未开始
    case no_start = 1
    /// 进行中
    case playing = 2
    /// 已结束
    case end = 3
}

struct GameInfoModel: Codable {
    var objectId: String = ""
    var status: GameStatus = .no_start
    
    var gameId: GameCenterType = .you_draw_i_guess
}
