//
//  AgoraVoiceSoundEffectModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/27.
//

import UIKit
import AgoraRtcKit

enum SoundEffectType: Int, CaseIterable {
    case space = 0
    case voiceChangerEffect = 1
    case styleTransformation = 2
    case pitchCorrection = 3
    case magicTone = 4

    var dataArray: [AgoraVoiceSoundEffectModel] {
        var tempArray = [AgoraVoiceSoundEffectModel]()
        switch self {
        case .space:
            var model = AgoraVoiceSoundEffectModel(imageName: "icon-KTV", title: "KTV", effectPreset: .roomAcousticsKTV)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-演唱会", title: "vocal_concert".localized, effectPreset: .roomAcousticsVocalConcert)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-录音棚", title: "recording_studio".localized, effectPreset: .roomAcousticsStudio)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-留声机", title: "phonograph".localized, effectPreset: .roomAcousticsPhonograph)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-虚拟立体声", title: "virtual_stereo".localized, effectPreset: .roomAcousticsVirtualStereo)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-空旷", title: "empty".localized, effectPreset: .roomAcousticsSpacial)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-空灵", title: "empty_spirit".localized, effectPreset: .roomAcousticsEthereal)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-3D人声", title: "three_d_human".localized, effectPreset: .roomAcoustics3DVoice)
            tempArray.append(model)
            
        case .voiceChangerEffect:
            var model = AgoraVoiceSoundEffectModel(imageName: "icon-大叔磁性", title: "uncle".localized, effectPreset: .voiceChangerEffectUncle)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-老年人", title: "old_man".localized, effectPreset: .voiceChangerEffectOldMan)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-小男孩", title: "little_boy".localized, effectPreset: .voiceChangerEffectBoy)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-小姐姐", title: "little_sister".localized, effectPreset: .voiceChangerEffectSister)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-小女孩", title: "little_girl".localized, effectPreset: .voiceChangerEffectGirl)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-猪八戒", title: "pigsy".localized, effectPreset: .voiceChangerEffectPigKing)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-绿巨人", title: "hulk".localized, effectPreset: .voiceChangerEffectHulk)
            tempArray.append(model)
            
        case .styleTransformation:
            var model = AgoraVoiceSoundEffectModel(imageName: "icon-R&B", title: "R&B", effectPreset: .styleTransformationRnB)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(imageName: "icon-流行", title: "popular".localized, effectPreset: .styleTransformationPopular)
            tempArray.append(model)

        case .pitchCorrection:
            var model = AgoraVoiceSoundEffectModel(title: "A", effectPreset: .pitchCorrection, pitchCorrectionValue: 1)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "Ab", effectPreset: .pitchCorrection, pitchCorrectionValue: 2)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "B", effectPreset: .pitchCorrection, pitchCorrectionValue: 3)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "C", effectPreset: .pitchCorrection, pitchCorrectionValue: 4)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "Cb", effectPreset: .pitchCorrection, pitchCorrectionValue: 5)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "D", effectPreset: .pitchCorrection, pitchCorrectionValue: 6)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "Db", effectPreset: .pitchCorrection, pitchCorrectionValue: 7)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "E", effectPreset: .pitchCorrection, pitchCorrectionValue: 8)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "F", effectPreset: .pitchCorrection, pitchCorrectionValue: 9)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "Fb", effectPreset: .pitchCorrection, pitchCorrectionValue: 10)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "G", effectPreset: .pitchCorrection, pitchCorrectionValue: 11)
            tempArray.append(model)
            model = AgoraVoiceSoundEffectModel(title: "Gb", effectPreset: .pitchCorrection, pitchCorrectionValue: 12)
            tempArray.append(model)
            
        case .magicTone: break
        }
        return tempArray
    }
    
    var title: String {
        switch self {
        case .space:               return "space_shape".localized
        case .voiceChangerEffect:  return "voice_sound".localized
        case .styleTransformation: return "style_sound".localized
        case .pitchCorrection:     return "electronic_music_sound".localized
        case .magicTone:           return "magic_scale".localized
        }
    }
    
    var edges: UIEdgeInsets {
        switch self {
        case .voiceChangerEffect: fallthrough
        case .styleTransformation: fallthrough
        case .space: return UIEdgeInsets(top: 0, left: 15.fit, bottom: 0, right: 15.fit)
        case .pitchCorrection: return UIEdgeInsets(top: 0, left: 15.fit, bottom: 0, right: 15.fit)
        case .magicTone: return .zero
        }
    }
    
    var minInteritemSpacing: CGFloat {
        switch self {
        case .voiceChangerEffect: fallthrough
        case .styleTransformation: fallthrough
        case .space: return 20.fit
        case .pitchCorrection: return 15.fit
        case .magicTone: return 0
        }
    }
    
    var minLineSpacing: CGFloat {
        switch self {
        case .voiceChangerEffect: fallthrough
        case .styleTransformation: fallthrough
        case .space: return 20.fit
        case .pitchCorrection: return 20.fit
        case .magicTone: return 0
        }
    }
    
    var layout: CGSize {
        switch self {
        case .voiceChangerEffect: fallthrough
        case .styleTransformation: fallthrough
        case .space:
            let w = (Screen.width - 30.fit * 2 -  20.fit * 3) / 4
            return CGSize(width: w, height: 100)
            
        case .pitchCorrection:
            let w = (Screen.width - 30.fit * 2 - 20.fit * 3) / 4
            return CGSize(width: w, height: 40)
            
        case .magicTone: return .zero
        }
    }
}


struct AgoraVoiceSoundEffectModel {
    var imageName: String = ""
    var title: String = ""
    var effectPreset: AgoraAudioEffectPreset = .roomAcoustics3DVoice
    var pitchCorrectionValue: Int = 0
}
