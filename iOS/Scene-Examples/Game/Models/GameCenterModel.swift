//
//  GameCenterModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

enum GameCenterType: String, Codable {
    /// 你画我猜
    case guess = "a55d92e338f542b29735c153c08806c6"
    /// 你画我猜同玩版本
    case guess_together = "CF04342AF9E769D62AA16B8B81DF2F9D"
    /// 谁是卧底
    case undercover = "9d217a75aba542ea989bb8b98e3364ad"
    /// 大话骰
    case dahuashai = "50927449262156b6a142fe8e5cfc01b5"
    /// 王国激战
    case kingdom = "b234547ca08d684fa327c90e174dcc93"
    
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

struct GameCenterModel: Codable {
    var iconName: String?
    var gameName: String = ""
    var gameId: GameCenterType = .guess
    var gameDesc: String?
    var vendorId: String?
    var display: GameDisplayModel?
    var playPattern: GamePlayPattern?
    var billing: GameBillingModel?
    var updateTime: String?
    
    
    static func createDatas(sceneType: SceneType) -> [GameCenterModel] {
        var dataArray = [GameCenterModel]()
        
        if sceneType == .playTogether {
            var model = GameCenterModel(iconName: "Game/draw",
                                        gameName: "你画我猜",
                                        gameId: .guess_together)
            dataArray.append(model)
            model = GameCenterModel(iconName: "Game/draw",
                                    gameName: "谁是卧底",
                                    gameId: .undercover)
            dataArray.append(model)
            
            model = GameCenterModel(iconName: "Game/draw",
                                    gameName: "大话骰",
                                    gameId: .dahuashai)
            dataArray.append(model)
            model = GameCenterModel(iconName: "Game/draw",
                                    gameName: "王国激战",
                                    gameId: .kingdom)
            dataArray.append(model)
            return dataArray
        }
        
        var model = GameCenterModel(iconName: "Game/draw",
                                    gameName: "你画我猜",
                                    gameId: .guess)
        dataArray.append(model)
        
        model = GameCenterModel(iconName: "Game/draw",
                                gameName: "谁是卧底",
                                gameId: .undercover)
        dataArray.append(model)
        
        model = GameCenterModel(iconName: "Game/draw",
                                gameName: "大话骰",
                                gameId: .dahuashai)
        dataArray.append(model)
        
        model = GameCenterModel(iconName: "Game/draw",
                                gameName: "王国激战",
                                gameId: .kingdom)
        dataArray.append(model)
        
        return dataArray
    }
}

struct GameDisplayModel: Codable {
    var suggestResolution: String?
    var isFullScreen: Bool = false
    var layout: String?
}

struct GamePlayPattern: Codable {
    var maxPlayerNum: Int = 0
    var type: Double = 0
}

struct GameBillingModel: Codable {
    var mode: Double = 0
    var unitPrice: Double = 0
    var miniCost: Double = 0
}
