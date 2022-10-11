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
            var model = AgoraVoiceBelCantoModel(imageName: "icon-大叔磁性", title: "magnetism_male".localized, voiceBeautifierPreset: .chatBeautifierMagnetic)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "icon-清新女", title: "fresh_female".localized, voiceBeautifierPreset: .chatBeautifierFresh)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "icon-活力女", title: "energy_female".localized, voiceBeautifierPreset: .chatBeautifierVitality)
            tempArray.append(model)
        case .sound:
            var model = AgoraVoiceBelCantoModel(imageName: "", title: "men".localized, voiceBeautifierPreset: .singingBeautifier)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "women".localized, voiceBeautifierPreset: .singingBeautifier)
            tempArray.append(model)
            
        case .change:
            var model = AgoraVoiceBelCantoModel(imageName: "", title: "vigorous".localized, voiceBeautifierPreset: .timbreTransformationVigorous)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "a_low".localized, voiceBeautifierPreset: .timbreTransformationDeep)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "round".localized, voiceBeautifierPreset: .timbreTransformationMellow)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "falsetto".localized, voiceBeautifierPreset: .timbreTransformationFalsetto)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "full".localized, voiceBeautifierPreset: .timbreTransformationFull)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "clear".localized, voiceBeautifierPreset: .timbreTransformationClear)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "high".localized, voiceBeautifierPreset: .timbreTransformationResounding)
            tempArray.append(model)
            model = AgoraVoiceBelCantoModel(imageName: "", title: "loud".localized, voiceBeautifierPreset: .timbreTransformationRinging)
            tempArray.append(model)
        }
        return tempArray
    }
    
    var title: String {
        switch self {
        case .voice: return "bel_canto_language_chat".localized
        case .sound: return "bel_canto_singing".localized
        case .change: return "timbre_transformation".localized
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
    var voiceBeautifierPreset: AgoraVoiceBeautifierPreset = .voiceBeautifierOff
}
