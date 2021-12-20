//
//  RtmSyncManager+Info.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation
import AgoraRtmKit

extension RtmSyncManager {
    struct Config {
        let appId: String
        let channelName: String
        
        init(appId: String,
             channelName: String) {
            self.appId = appId
            self.channelName = channelName
        }
    }
}
