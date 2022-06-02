//
//  AgoraJson+Extension.swift
//  AgoraSyncManager
//
//  Created by ZYP on 2022/2/7.
//

import Foundation
import AgoraSyncKit

extension AgoraJson {
    /// 嵌套json的String
    func getJsonString(field: String) -> String? {
        var str: NSString = ""
        var json = AgoraJson()
        var ret = getField(field, agoraJson: &json)
        if ret != .noError {
            Log.errorText(text: "getString error \(ret.rawValue) \(str)", tag: "AgoraJson.getJsonString")
            return nil
        }
        ret = json.getString(&str)
        if ret != .noError {
            Log.errorText(text: "getString error \(ret.rawValue) \(str)", tag: "AgoraJson.getJsonString")
            return nil
        }
        return str as String
    }
    
    func getStringValue() -> String? {
        var str: NSString = ""
        let ret = getString(&str)
        if ret != .noError {
            Log.errorText(text: "getString error \(ret.rawValue) \(str)", tag: "AgoraJson.getStringValue")
            return nil
        }
        return str as String
    }
}
