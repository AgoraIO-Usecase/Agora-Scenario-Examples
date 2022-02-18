//
//  MainModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

enum SceneType: String {
    /// 单直播
    case singleLive = "signleLive"
    /// 超级小班课
    case breakoutRoom = "BreakOutRoom"
    /// 音效
    case agoraVoice = "agoraVoice"
    /// 游戏
    case game = "interactiveGame"
    /// PKApply
    case pkApply = "pkApplyInfo"
    /// 同玩
    case playTogether = "sudPlayTogether"//"playTogether"
    /// 1v1
    case oneToOne = "oneToOne"
    
    var alertTitle: String {
        switch self {
        case .game: return "PK_Recieved_Game_Invite".localized
        case .pkApply: return "PK_Recieved_Invite".localized
        default: return ""
        }
    }
    
    var gameType: Int {
        switch self {
        case .game: return 3
        case .playTogether: return 2
        case .oneToOne: return 1
        default: return 0
        }
    }
}

struct MainModel {
    var title: String = ""
    var desc: String = ""
    var imageNmae: String = ""
    var sceneType: SceneType = .singleLive
    
    static func mainDatas() -> [MainModel] {
        var dataArray = [MainModel]()
//        var model = MainModel()
//        model.title = "单主播直播"
//        model.desc = "单主播直播"
//        model.imageNmae = "pic-single"
//        model.sceneType = .singleLive
//        dataArray.append(model)
//
//        model = MainModel()
//        model.title = "PK直播"
//        model.desc = "两个不同直播间的主播跨频道连麦PK, 引爆直播间"
//        model.imageNmae = "pic-PK"
//        model.sceneType = .pkApply
//        dataArray.append(model)
//
//        model = MainModel()
//        model.title = "超级小班课"
//        model.desc = "多人会议, 可建立小会议室讨论"
//        model.imageNmae = "pic-multiple"
//        model.sceneType = .breakoutRoom
//        dataArray.append(model)
//
//        model = MainModel()
//        model.title = "音效"
//        model.desc = "音效"
//        model.imageNmae = "pic-goods"
//        model.sceneType = .agoraVoice
//        dataArray.append(model)
//
//        model = MainModel()
//        model.title = "游戏PK直播"
//        model.desc = "你画我猜"
//        model.imageNmae = "pic-Virtual"
//        model.sceneType = .game
//        dataArray.append(model)
        
        var model = MainModel()
        model.title = "游戏同玩直播"
        model.desc = "你画我猜"
        model.imageNmae = "pic-Virtual"
        model.sceneType = .playTogether
        dataArray.append(model)
        
//        model = MainModel()
//        model.title = "游戏1V1直播"
//        model.desc = "你画我猜"
//        model.imageNmae = "pic-Virtual"
//        model.sceneType = .oneToOne
//        dataArray.append(model)
        
        return dataArray
    }
    
    static func sceneId(type: SceneType) -> String {
        type.rawValue
    }
}
