//
//  Error.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation

public class SyncError: NSObject, LocalizedError {
    public let message: String
    public let code: Int

    override public var description: String {
        return message + "code: (\(code))"
    }

    public var errorDescription: String? {
        return message
    }

    public init(message: String,
                code: Int) {
        self.message = message
        self.code = code
    }
}
