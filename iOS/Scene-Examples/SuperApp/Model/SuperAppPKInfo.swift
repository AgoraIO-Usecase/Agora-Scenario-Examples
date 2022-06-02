//
//  RoomInfo.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/16.
//

import Foundation

struct SuperAppPKInfo: Codable {
    /// 当前房间正在pk的用户id
    let userIdPK: String
    
    var dict: [String : String] {
        return ["userIdPK" : userIdPK]
    }
    
    init(userIdPK: String) {
        self.userIdPK = userIdPK
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.userIdPK = try container.decode(String.self, forKey: .userIdPK)
    }
}

