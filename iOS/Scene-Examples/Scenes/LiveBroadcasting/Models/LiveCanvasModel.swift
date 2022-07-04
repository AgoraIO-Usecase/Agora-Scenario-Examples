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
    /// 频道名
    var channelName: String = ""
    /// 加入channel
    var connection: AgoraRtcConnection?
    
    
    static func createCanvas(uid: UInt) -> AgoraRtcVideoCanvas {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.renderMode = .hidden
        return canvas
    }
    
    static func createConnection(channelName: String, uid: UInt) -> AgoraRtcConnection {
        let connection = AgoraRtcConnection()
        connection.channelId = channelName
        connection.localUid = uid
        return connection
    }
    
    required init() { }
}
