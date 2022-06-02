//
//  SupperAppStorageManager.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/15.
//

import Foundation

class SupperAppStorageManager {
    
    fileprivate static let userNameKey = "superApp-userName"
    fileprivate static let roomNameKey = "superApp-roomName"
    fileprivate static let uuidKey = "superApp-uuid"
    
    static var userName: String {
        set {
            UserDefaults.standard.setValue(newValue,
                                           forKey: userNameKey)
        }
        get {
            if let name = (UserDefaults.standard.value(forKey: userNameKey) as? String) {
                return name
            }
            let str: String = .randomUserName
            UserDefaults.standard.setValue(str,
                                           forKey: userNameKey)
            return str
        }
    }
    
    static var roomName: String {
        set {
            UserDefaults.standard.setValue(newValue,
                                           forKey: roomNameKey)
        }
        get {
            return (UserDefaults.standard.value(forKey: roomNameKey) as? String) ?? ""
        }
    }
    
    static var uuid: String {
        set {
            UserDefaults.standard.setValue(newValue,
                                           forKey: uuidKey)
        }
        get {
            var value = (UserDefaults.standard.value(forKey: uuidKey) as? String)
            if value == nil {
                value = UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
                value = value?.subString(to: 16)
                UserDefaults.standard.setValue(value,
                                               forKey: uuidKey)
            }
            return value!
        }
    }
}
