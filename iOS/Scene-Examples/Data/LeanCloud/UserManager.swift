//
//  UserManagerProxy.swift
//  Core
//
//  Created by XC on 2021/6/1.
//
#if LEANCLOUD
import Foundation
import Core
import LeanCloud
import RxSwift

class LeanCloudUserManager: IUserManager {
    
    static func from(object: LCObject) throws -> User {
        let name: String = object.get(User.NAME)!.stringValue!
        let avatar: String = object.get(User.AVATAR)!.stringValue!
        return User(id: object.objectId!.stringValue!, name: name, avatar: avatar)
    }
    
    func randomUser() -> Observable<Result<User>> {
        let user = User(id: "", name: Utils.randomString(length: 8), avatar: Utils.randomAvatar())
        return create(user: user).map { result in
            if (result.success) {
                user.id = result.data!
                return Result(success: true, data: user)
            } else {
                return Result(success: false, message: result.message)
            }
        }
    }
    
    func create(user: User) -> Observable<Result<String>> {
        return Database.save {
            let object = LCObject(className: User.TABLE)
            try object.set(User.NAME, value: user.name)
            try object.set(User.AVATAR, value: user.avatar)
            return object
        }
    }
    
    func getUser(by objectId: String) -> Observable<Result<User>> {
        return Database.query(
            className: User.TABLE,
            objectId: objectId,
            queryWhere: nil,
            transform: { (data: LCObject) -> User in
                return try LeanCloudUserManager.from(object: data)
            }
        )
    }
    
    func update(user: User, name: String) -> Observable<Result<Void>> {
        return Database.save {
            let object = LCObject(className: User.TABLE, objectId: user.id)
            try object.set(User.NAME, value: name)
            return object
        }
        .map { result in
            if (result.success) {
                user.name = name
            }
            return Result(success: result.success, message: result.message)
        }
    }
}

#endif
