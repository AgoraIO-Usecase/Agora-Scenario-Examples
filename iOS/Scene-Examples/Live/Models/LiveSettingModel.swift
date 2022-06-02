//
//  LiveSettingModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit

struct LiveSettingModel {
    enum LiveSettingType {
        case resolution, frameRate, bitRate
    }
    var title: String = ""
    var desc: String = ""
    var sliderValue: Float = 0
    var settingType: LiveSettingType = .resolution
    
    var resolutionTitle: CGSize {
        switch desc {
        case "240 X 240": return AgoraVideoDimension240x240
        case "360 X 360": return AgoraVideoDimension360x360
        case "480 X 840": return AgoraVideoDimension840x480
        case "720 X 1280": return AgoraVideoDimension1280x720
        default: return AgoraVideoDimension240x240
        }
    }
    var frameRate: AgoraVideoFrameRate {
        switch desc {
        case "15": return AgoraVideoFrameRate.fps15
        case "24": return AgoraVideoFrameRate.fps24
        case "30": return AgoraVideoFrameRate.fps30
        default: return AgoraVideoFrameRate.fps15
        }
    }
    
    static func settingsData() -> [LiveSettingModel] {
        var dataArray = [LiveSettingModel]()
        var model = LiveSettingModel(title: "Resolution".localized, desc: "240 X 240", settingType: .resolution)
        dataArray.append(model)
        model = LiveSettingModel(title: "FrameRate".localized, desc: "15", settingType: .frameRate)
        dataArray.append(model)
        model = LiveSettingModel(title: "BitRate".localized, desc: "200kbps", sliderValue: 0.1, settingType: .bitRate)
        dataArray.append(model)
        return dataArray
    }
    
    static func resolutionData() -> [LiveSettingModel] {
        var dataArray = [LiveSettingModel]()
        var model = LiveSettingModel(title: "", desc: "240 X 240", settingType: .resolution)
        dataArray.append(model)
        model = LiveSettingModel(title: "", desc: "360 X 360", settingType: .resolution)
        dataArray.append(model)
        model = LiveSettingModel(title: "", desc: "480 X 840", settingType: .resolution)
        dataArray.append(model)
        model = LiveSettingModel(title: "", desc: "720 X 1280", settingType: .resolution)
        dataArray.append(model)
        return dataArray
    }
    
    static func frameRateData() -> [LiveSettingModel] {
        var dataArray = [LiveSettingModel]()
        var model = LiveSettingModel(title: "", desc: "15", settingType: .frameRate)
        dataArray.append(model)
        model = LiveSettingModel(title: "", desc: "24", settingType: .frameRate)
        dataArray.append(model)
        model = LiveSettingModel(title: "", desc: "30", settingType: .frameRate)
        dataArray.append(model)
        return dataArray
    }
}

class LiveSettingUseData {
    var sliderValue: Int = 0
    var resolution: CGSize = .zero
    var framedate: AgoraVideoFrameRate = .fps15
}
