//
//  GameCenterModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

enum GameCenterType: Codable {
    /// 你画我猜
    case you_draw_i_guess

    var gameUrl: String {
        switch self {
        case .you_draw_i_guess:
            return "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html"
        }
    }
}

enum GameBarrageType: Int, Codable, CaseIterable {
    /// 礼炮
    case salvo = 1
    /// 点赞
    case praise = 2
    /// 丢屎
    case shit = 3
}

struct GameCenterModel {
    var iconName: String = ""
    var title: String = ""
    var type: GameCenterType = .you_draw_i_guess
    
    static func createDatas() -> [GameCenterModel] {
        var dataArray = [GameCenterModel]()
        let model = GameCenterModel(iconName: "Game/draw",
                                    title: "你画我猜",
                                    type: .you_draw_i_guess)
        dataArray.append(model)
        
//        model = GameCenterModel(iconName: "gift-cake",
//                                    title: "你画我猜",
//                                    type: .you_draw_i_guess)
//        dataArray.append(model)
//
//        model = GameCenterModel(iconName: "gift-cake",
//                                    title: "你画我猜",
//                                    type: .you_draw_i_guess)
//        dataArray.append(model)
//
//        model = GameCenterModel(iconName: "gift-cake",
//                                    title: "你画我猜",
//                                    type: .you_draw_i_guess)
//        dataArray.append(model)
        
        return dataArray
    }
}
