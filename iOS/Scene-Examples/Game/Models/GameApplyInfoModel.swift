//
//  GameApplyInfoModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/29.
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

struct GameApplyInfoModel: Codable {
    var objectId: String = ""
    
    var status: GameStatus = .no_start
    
    var gameId: GameCenterType = .guess
}
