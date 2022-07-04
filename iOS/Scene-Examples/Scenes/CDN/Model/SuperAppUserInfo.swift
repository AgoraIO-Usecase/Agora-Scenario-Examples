//
//  SuperAppUserInfo.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/16.
//

import Foundation

struct SuperAppUserInfo: Codable {
    let userId: String
    let userName: String
    
    var dict: [String : String] {
        return ["userId" : userId,
                "userName" : userName]
    }
    
    init(userId: String,
         userName: String) {
        self.userId = userId
        self.userName = userName
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.userId = try container.decode(String.self, forKey: .userId)
        self.userName = try container.decode(String.self, forKey: .userName)
    }
}
