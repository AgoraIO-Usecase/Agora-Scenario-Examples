//
//  AgoraVoiceBelCantoModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/26.
//

import UIKit
import AgoraRtcKit

enum AgoraVoiceBelCantoType: Int, CaseIterable {
    /// 音色美声
    case voice = 0
    /// 歌唱美声
    case sound = 1
    /// 音色变换
    case change = 2
    
    
    var dataArray: [AgoraVoiceBelCantoModel] {
        var tempArray = [AgoraVoiceBelCantoModel]()
        switch self {
        case .voice:
            var model = AgoraVoiceBelCantoModel(imageName: "icon-大叔磁性", title: "磁性(男)", voiceBeautifierPreset: .presetChatBeautifierMagnetic)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "icon-清新女", title: "清新(女)", voiceBeautifierPreset: .presetChatBeautifierFresh)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "icon-活力女", title: "活力(女)", voiceBeautifierPreset: .presetChatBeautifierVitality)
            tempArray.append(model)
        case .sound:
            var model = AgoraVoiceBelCantoModel(imageName: "", title: "男性", voiceBeautifierPreset: .presetSingingBeautifier)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "女性", voiceBeautifierPreset: .presetSingingBeautifier)
            tempArray.append(model)
            
        case .change:
            var model = AgoraVoiceBelCantoModel(imageName: "", title: "浑厚", voiceBeautifierPreset: .timbreTransformationVigorous)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "低沉", voiceBeautifierPreset: .timbreTransformationDeep)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "圆润", voiceBeautifierPreset: .timbreTransformationMellow)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "假音", voiceBeautifierPreset: .timbreTransformationFalsetto)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "饱满", voiceBeautifierPreset: .timbreTransformationFull)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "清澈", voiceBeautifierPreset: .timbreTransformationClear)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "高亢", voiceBeautifierPreset: .timbreTransformationResounding)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "嘹亮", voiceBeautifierPreset: .timbreTransformatRinging)
            tempArray.append(model)
        }
        return tempArray
    }
    
    var title: String {
        switch self {
        case .voice: return "语聊美声".localized
        case .sound: return "歌唱美声".localized
        case .change: return "音色变换".localized
        }
    }
    
    var edges: UIEdgeInsets {
        switch self {
        case .voice: return UIEdgeInsets(top: 0, left: 50.fit, bottom: 0, right: 50.fit)
        case .sound: fallthrough
        case .change:
            return UIEdgeInsets(top: 0, left: 15.fit, bottom: 0, right: 15.fit)
        }
    }
    
    var minInteritemSpacing: CGFloat {
        switch self {
        case .voice: return 60.fit
        case .sound: fallthrough
        case .change: return 20.fit
        }
    }
    
    var minLineSpacing: CGFloat {
        switch self {
        case .voice: return 50.fit
        case .sound: fallthrough
        case .change: return 15.fit
        }
    }
    
    var layout: CGSize {
        switch self {
        case .voice:
            let w = (Screen.width - 50.fit * 2 -  60.fit * 2) / 3
            return CGSize(width: w, height: 100)
            
        case .sound: fallthrough
        case .change:
            let w = (Screen.width - 30.fit * 5) / 4
            return CGSize(width: w, height: 40)
        }
    }
}

struct AgoraVoiceBelCantoModel {
    var imageName: String = ""
    var title: String = ""
    var voiceBeautifierPreset: AgoraVoiceBeautifierPreset = .presetChatBeautifierMagnetic
}
