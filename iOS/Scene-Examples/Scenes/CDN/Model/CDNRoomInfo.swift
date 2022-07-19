//
//  RoomItem.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/29.
//

import Foundation

struct CDNRoomInfo: Codable {
    let roomId: String
    let roomName: String
    let liveMode: LiveMode
    
    static func create(jsonString: String) -> CDNRoomInfo? {
        let decoder = JSONDecoder()
        guard let data = jsonString.data(using: .utf8) else {
            return nil
        }
        
        do {
            let item = try decoder.decode(CDNRoomInfo.self, from: data)
            return item
        } catch let error {
            LogUtils.log(message: error.localizedDescription, level: .error)
            return nil
        }
    }
    
    var dict: [String : String] {
        return ["roomId" : roomId,
                "roomName" : roomName,
                "liveMode" : liveMode.rawValue]
    }
}

enum LiveMode: String, Codable {
    case push = "1"
    case byPassPush = "2"
}
