//
//  GameModeModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

enum GameModeType {
    /// 连麦pk
    case game_pk
    /// 同玩
    case together
}

struct GameModeModel {
    var iconName: String = ""
    var title: String = ""
    var bgColor: UIColor = .white
    var type: GameModeType = .game_pk
    
    static func createDatas() -> [GameModeModel] {
        var dataArray = [GameModeModel]()
        var model = GameModeModel(iconName: "Game/pkgame-icon",
                                  title: "even_michael_PK".localized,
                                  bgColor: .init(hex: "#F5A623"),
                                  type: .game_pk)
        dataArray.append(model)
        
        model = GameModeModel(iconName: "Game/fansgame",
                              title: "fan_interactive_games".localized,
                              bgColor: .init(hex: "#4A90E2"),
                              type: .together)
        dataArray.append(model)
//        
//        model = GameModeModel(iconName: "Game/pkgame-icon",
//                              title: "连麦PK",
//                              bgColor: .init(hex: "#F5A623"),
//                              type: .game_pk)
//        dataArray.append(model)
//        
        return dataArray
    }
}
