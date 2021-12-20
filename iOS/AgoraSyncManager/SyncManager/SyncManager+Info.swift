//
//  SyncManager+Info.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation

public protocol ConfigProtocol {}

extension AgoraSyncManager {
    public struct RtmConfig: ConfigProtocol {
        let appId: String
        let channelName: String
        /// init for RtmConfig
        /// - Parameters:
        ///   - appId: appId
        ///   - channelName: channelName
        public init(appId: String,
                    channelName: String) {
            self.channelName = channelName
            self.appId = appId
        }
    }
    
    // public struct FireBaseConfig: ConfigProtocol {}
    // public struct LearnCloudConfig: ConfigProtocol {}
}
