//
//  DocumentReference.swift
//  SyncManager
//
//  Created by ZYP on 2021/12/16.
//

import Foundation

public class SceneReference: DocumentReference {
    override public var className: String {
        id
    }
    
    init(manager: AgoraSyncManager, id: String) {
        super.init(manager: manager, parent: nil, id: id)
    }
    
    /// 创建一个CollectionReference实体
    /// - Parameter className: CollectionReference 的 id
    public func collection(className: String) -> CollectionReference {
        CollectionReference(manager: manager,
                            parent: self,
                            className: className)
    }
    
    /// delete current scene
    public override func delete(success: SuccessBlock? = nil,
                                fail: FailBlock? = nil) {
        manager.delete(documentRef: self,
                       success: success,
                       fail: fail)
        manager.deleteScenes(sceneIds: [id], success: {
            Log.info(text: "deleteScenes success", tag: "SceneReference")
        }, fail: { error in
            Log.error(error: error, tag: "SceneReference")
        })
    }
}

public class DocumentReference {
    public let id: String
    public let parent: CollectionReference?
    let manager: AgoraSyncManager
    
    public var className: String {
        return parent!.className
    }
    
    init(manager: AgoraSyncManager, parent: CollectionReference?, id: String) {
        self.manager = manager
        self.parent = parent
        self.id = id
    }
    
    /// 获取指定属性值
    /// - Parameters:
    ///   - key: 键值 为nil或空字符串时，使用scene作为保存。非空字符串时候使用scene的子集保存。
    public func get(key: String? = nil,
                    success: SuccessBlockObjOptional? = nil,
                    fail: FailBlock? = nil) {
        manager.get(documentRef: self,
                    key: key,
                    success: success,
                    fail: fail)
    }
    
    /// 更新指定属性值
    /// - Parameters:
    ///   - key: 键值 为nil或空字符串时，使用scene作为保存。非空字符串时候使用scene的子集保存。
    ///   - data: value
    public func update(key: String? = nil,
                       data: [String : Any?],
                       success: SuccessBlock? = nil,
                       fail: FailBlock? = nil) {
        manager.update(reference: self,
                       key: key,
                       data: data,
                       success: success,
                       fail: fail)
    }
    
    /// 删除房间
    public func delete(success: SuccessBlock? = nil,
                       fail: FailBlock? = nil) {
        manager.delete(documentRef: self,
                       success: success,
                       fail: fail)
    }
    
    /// 订阅属性更新事件
    /// - Parameters:
    ///   - key: 键值 为nil或空字符串时，使用scene作为保存。非空字符串时候使用scene的子集保存。
    public func subscribe(key: String? = nil,
                          onCreated: OnSubscribeBlock? = nil,
                          onUpdated: OnSubscribeBlock? = nil,
                          onDeleted: OnSubscribeBlock? = nil,
                          onSubscribed: OnSubscribeBlockVoid? = nil,
                          fail: FailBlock? = nil) {
        manager.subscribe(reference: self,
                          key: key,
                          onCreated: onCreated,
                          onUpdated: onUpdated,
                          onDeleted: onDeleted,
                          onSubscribed: onSubscribed,
                          fail: fail)
    }
    
    /// 取消订阅
    /// - Parameter key: 键值 为nil或空字符串时，使用scene作为保存。非空字符串时候使用scene的子集保存。
    public func unsubscribe(key: String? = nil) {
        manager.unsubscribe(reference: self, key: key)
    }
}

