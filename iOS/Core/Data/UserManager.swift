//
//  UserManager.swift
//  Core
//
//  Created by XC on 2021/6/1.
//

import Foundation
import RxSwift

extension User {
    public static let TABLE: String = "USER"
    public static let NAME: String = "name"
    public static let AVATAR: String = "avatar"
    
    public func getLocalAvatar() -> String {
        switch avatar {
        case "1":
            return "default"
        case "2":
            return "portrait02"
        case "3":
            return "portrait03"
        case "4":
            return "portrait04"
        case "5":
            return "portrait05"
        case "6":
            return "portrait06"
        case "7":
            return "portrait07"
        case "8":
            return "portrait08"
        case "9":
            return "portrait09"
        case "10":
            return "portrait10"
        case "11":
            return "portrait11"
        case "12":
            return "portrait12"
        case "13":
            return "portrait13"
        case "14":
            return "portrait14"
        default:
            return "default"
        }
    }
}

public protocol IUserManager {
    func randomUser() -> Observable<Result<User>>
    func create(user: User) -> Observable<Result<String>>
    func getUser(by objectId: String) -> Observable<Result<User>>
    func update(user: User, name: String) -> Observable<Result<Void>>
}

extension User {
    public static func create(user: User) -> Observable<Result<String>> {
        return InjectionService.shared.resolve(IUserManager.self).create(user: user)
    }
    
    public static func getUser(by objectId: String) -> Observable<Result<User>> {
        return InjectionService.shared.resolve(IUserManager.self).getUser(by: objectId)
    }
    
    public static func randomUser() -> Observable<Result<User>>  {
        return InjectionService.shared.resolve(IUserManager.self).randomUser()
    }
    
    public func update(name: String) -> Observable<Result<Void>> {
        return InjectionService.shared.resolve(IUserManager.self).update(user: self, name: name)
    }
}
