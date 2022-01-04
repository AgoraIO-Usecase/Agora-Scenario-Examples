//
//  GameCenterModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

enum GameCenterType: Int, Codable {
    /// 你画我猜
    case guess = 1
    /// 你画我猜同玩版本
    case guess_together = 2
    /// 谁是卧底
    case undercover = 3
    /// 大话骰
    case dahuashai = 4
    /// 王国激战
    case kingdom = 5
    
    var gameUrl: String {
        switch self {
        case .guess: return "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html"
        case .guess_together: return "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess_More/index.html"
        case .undercover: return "https://imgsecond.yuanqiyouxi.com/test/spy/index.html"
        case .dahuashai: return "https://imgsecond.yuanqiyouxi.com/test/Dice_ShengWang/index.html"
        case .kingdom: return "https://imgsecond.yuanqiyouxi.com/test/War/web-mobile/index.html"
        }
    }
    
    var bgImage: UIImage? {
        switch self {
        case .guess:
            return UIImage(named: "Game/draw_bg")
        case .guess_together:
            return UIImage(named: "Game/draw_bg")
        case .undercover:
            return UIImage(named: "Game/draw_bg")
        case .dahuashai:
            return UIImage(named: "Game/draw_bg")
        case .kingdom:
            return UIImage(named: "Game/draw_bg")
        }
    }
    
    var requestParams: String {
        switch self {
        case .guess: return "guess"
        case .guess_together: return "guess"
        case .undercover: return ""
        case .dahuashai: return ""
        case .kingdom: return "war"
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
    var type: GameCenterType = .guess
    
    static func createDatas(sceneType: SceneType) -> [GameCenterModel] {
        var dataArray = [GameCenterModel]()
        
        if sceneType == .playTogether {
            let model = GameCenterModel(iconName: "Game/draw",
                                        title: "你画我猜",
                                        type: .guess_together)
            dataArray.append(model)
            return dataArray
        }
        
        var model = GameCenterModel(iconName: "Game/draw",
                                    title: "你画我猜",
                                    type: .guess)
        dataArray.append(model)
        
        model = GameCenterModel(iconName: "Game/draw",
                                title: "谁是卧底",
                                type: .undercover)
        dataArray.append(model)
        
        model = GameCenterModel(iconName: "Game/draw",
                                title: "大话骰",
                                type: .dahuashai)
        dataArray.append(model)
        
        model = GameCenterModel(iconName: "Game/draw",
                                title: "王国激战",
                                type: .kingdom)
        dataArray.append(model)
        
        return dataArray
    }
}
