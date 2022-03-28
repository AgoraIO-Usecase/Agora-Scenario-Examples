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
    /// 夜店
    case agoraClub = "agoraClub"
    /// 游戏
    case game = "interactiveGame"
    /// PKApply
    case pkApply = "pkApplyInfo"
    /// 同玩
    case playTogether = "playTogether"
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
        var model = MainModel()
        model.title = "Single_Broadcaster".localized
        model.desc = "Single_Broadcaster".localized
        model.imageNmae = "pic-single"
        model.sceneType = .singleLive
        dataArray.append(model)

        model = MainModel()
        model.title = "PK_Live".localized
        model.desc = "anchors_of_two_different_live_broadcast_rooms".localized
        model.imageNmae = "pic-PK"
        model.sceneType = .pkApply
        dataArray.append(model)

        model = MainModel()
        model.title = "breakoutroom".localized
        model.desc = "person_meetings_small_conference_rooms".localized
        model.imageNmae = "pic-multiple"
        model.sceneType = .breakoutRoom
        dataArray.append(model)

        model = MainModel()
        model.title = "sound_effect".localized
        model.desc = "sound_effect".localized
        model.imageNmae = "pic-goods"
        model.sceneType = .agoraVoice
        dataArray.append(model)
        
        model = MainModel()
        model.title = "agoraClub".localized
        model.desc = "agoraClub".localized
        model.imageNmae = "pic-Blind-date"
        model.sceneType = .agoraClub
        dataArray.append(model)

        model = MainModel()
        model.title = "game_PK_live_broadcast".localized
        model.desc = "draw_guess".localized
        model.imageNmae = "pic-Virtual"
        model.sceneType = .game
        dataArray.append(model)
        
        model = MainModel()
        model.title = "play_live_with_the_game".localized
        model.desc = "draw_guess".localized
        model.imageNmae = "pic-Virtual"
        model.sceneType = .playTogether
        dataArray.append(model)
        
        model = MainModel()
        model.title = "game_live_on_V1".localized
        model.desc = "draw_guess".localized
        model.imageNmae = "pic-Virtual"
        model.sceneType = .oneToOne
        dataArray.append(model)
        
        return dataArray
    }
    
    static func sceneId(type: SceneType) -> String {
        type.rawValue
    }
}
