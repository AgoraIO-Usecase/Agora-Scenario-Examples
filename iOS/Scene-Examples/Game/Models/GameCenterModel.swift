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
    
    
    /// 碰碰我最强
    case bumper = "1461227817776713818"
    /// 飞镖达人
    case knife = "1461228379255603251"
    /// 你画我猜
    case draw_and_guess = "1461228410184400899"
    /// 五子棋
    case gobang = "1461297734886621238"
    /// 飞行棋
    case ludo = "1468180338417074177"
    /// 黑白棋
    case reversi = "1461297789198663710"
    /// 短道速滑
    case skating = "1468090257126719572"
    /// 数字转轮
    case roll = "1468434637562912769"
    /// 石头剪刀布
    case rsp = "1468434723902660610"
    /// 数字炸弹
    case number_bomb = "1468091457989509190"
    /// 扫雷
    case mine = "1468434401847222273"
    /// 你说我猜
    case sayGuess = "1468434504892882946"
    
    case teenPatti = "1472142478505271298"
    case UMO = "1472142559912517633"
    case deminers = "1472142640866779138"
    case TWMahjong = "1472142695162044417"
    
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
            
        default:
            return UIImage(named: "Game/draw_bg")
        }
    }
    
    var title: String {
        switch self {
        case .guess: fallthrough
        case .guess_together: return "你画我猜"
        case .undercover: return "谁是卧底"
        case .dahuashai: return "大话骰"
        case .kingdom: return "王国激战"
            
        case .bumper: return "碰碰我最强"
        case .knife: return "飞镖达人"
        case .draw_and_guess: return "你画我猜"
        case .gobang: return "五子棋"
        case .ludo: return "飞行棋"
        case .reversi: return "黑白棋"
        case .skating: return "短道速滑"
        case .roll: return "数字转轮"
        case .rsp: return "石头剪刀布"
        case .number_bomb: return "数字炸弹"
        case .mine: return "扫雷"
        case .sayGuess: return "你说我猜"
        case .teenPatti: return "TeenPatti"
        case .UMO: return "UMO"
        case .deminers: return "排雷兵"
        case .TWMahjong: return "台湾麻将"
        }
    }
}

enum GameSourcesType: String, Codable {
    case yuanqi
    case sud
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
    var sources: GameSourcesType? = .yuanqi
    
    
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
