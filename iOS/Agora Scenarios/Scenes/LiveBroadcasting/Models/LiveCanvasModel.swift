//
//  LiveCanvasModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit
import AgoraRtcKit

class LiveCanvasModel {
    /// 显示直播的画布
    var canvas: AgoraRtcVideoCanvas?
    
    var userId: UInt = UserInfo.userId
    
    static func createCanvas(uid: UInt, channelName: String) -> AgoraRtcVideoCanvas {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.renderMode = .hidden
        canvas.channelId = channelName
        return canvas
    }
    
    required init() { }
}
