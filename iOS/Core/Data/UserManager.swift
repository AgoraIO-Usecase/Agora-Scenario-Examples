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

public protocol IUserManagerProxy {
    func randomUser() -> Observable<Result<User>>
    func create(user: User) -> Observable<Result<String>>
    func getUser(by objectId: String) -> Observable<Result<User>>
    func update(user: User, name: String) -> Observable<Result<Void>>
}

public class UserManager {
    private var proxy: IUserManagerProxy!
    public static var shared: UserManager = UserManager()
    
    private init() {}
    
    public func setProxy(_ proxy: IUserManagerProxy) {
        self.proxy = proxy
    }
    
    public func randomUser() -> Observable<Result<User>> {
        return proxy.randomUser()
    }
    
    public func create(user: User) -> Observable<Result<String>> {
        return proxy.create(user: user)
    }
    
    public func getUser(by objectId: String) -> Observable<Result<User>> {
        return proxy.getUser(by: objectId)
    }
    
    public func update(user: User, name: String) -> Observable<Result<Void>> {
        return proxy.update(user: user, name: name)
    }
}

extension User {
    public static func create(user: User) -> Observable<Result<String>> {
        return UserManager.shared.create(user: user)
    }
    
    public static func getUser(by objectId: String) -> Observable<Result<User>> {
        return UserManager.shared.getUser(by: objectId)
    }
    
    public static func randomUser() -> Observable<Result<User>>  {
        return UserManager.shared.randomUser()
    }
    
    public func update(name: String) -> Observable<Result<Void>> {
        return UserManager.shared.update(user: self, name: name)
    }
}
