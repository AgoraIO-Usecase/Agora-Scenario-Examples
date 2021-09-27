//
//  PKSyncManager+Error.swift
//  LivePK
//
//  Created by ZYP on 2021/9/26.
//

import Foundation

class SyncError: Error, CustomStringConvertible {
    let code: Int
    let domain: Domain
    
    init(domain: Domain,
         code: Int) {
        self.code = code
        self.domain = domain
    }
    
    var description: String {
        return "\(domain.description) \(code)"
    }
}

extension SyncError {
    enum Domain: CustomStringConvertible {
        case login
        case join
        case updateAttributes
        case getAttributes
        
        var description: String {
            switch self {
            case .login:
                return "login"
            case .join:
                return "join"
            case .updateAttributes:
                return "updateAttributes"
            case .getAttributes:
                return "getAttributes"
            }
        }
    }
}
