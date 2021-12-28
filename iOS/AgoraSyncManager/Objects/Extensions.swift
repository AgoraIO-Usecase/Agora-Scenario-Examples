//
//  Extensions.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation
import AgoraRtmKit
import CommonCrypto

extension AgoraRtmChannelAttribute {
    func toAttribute() -> IObject {
        return Attribute(key: self.key, value: self.value)
    }
}

extension UUID {
    func uuid16string() -> String {
        return String(self.uuidString.md5)
    }
}

extension String {
    /* ################################################################## */
    /**
     - returns: the String, as an MD5 hash.
     */
    var md5: String {
        let str = self.cString(using: String.Encoding.utf8)
        let strLen = CUnsignedInt(self.lengthOfBytes(using: String.Encoding.utf8))
        let digestLen = 16
        let result = UnsafeMutablePointer<CUnsignedChar>.allocate(capacity: digestLen)
        CC_MD5(str!, strLen, result)

        let hash = NSMutableString()

        for i in 0..<digestLen {
            hash.appendFormat("%02x", result[i])
        }

        result.deallocate()
        return hash as String
    }
}
