//
//  RtmSyncManager+Info.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation
//import AgoraRtmKit
import AgoraRtcKit

extension RtmSyncManager {
    struct Config {
        let appId: String
        let appKey: String
        let channelName: String
        
        init(appId: String,
             appKey: String = "",
             channelName: String) {
            self.appId = appId
            self.channelName = channelName
            self.appKey = appKey
        }
    }
}
