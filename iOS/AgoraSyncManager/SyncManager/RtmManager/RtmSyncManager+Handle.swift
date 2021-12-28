//
//  RtmSyncManager+Handle.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation
import AgoraRtmKit

extension RtmSyncManager {
    func notifyObserver(channel: AgoraRtmChannel, attributes: [AgoraRtmChannelAttribute]) {
        /// 1. defaultChannel 会有缓存  没有的话走update
        /// 2. collection 会有缓存
        /// 3. scene.id+key、scene.id 没有缓存 都走update
        
        if channels[channelName] == channel { /** 1. defaultChannel **/
            if let cache = self.cachedAttrs[channel] { /** cache 存在的情况下，计算需要回调的事件 **/
                guard let tempChannel = channels[sceneName] else {
                    return
                }
                let onCreateBlock = onCreateBlocks[tempChannel]
                let onUpdateBlock = onUpdatedBlocks[tempChannel]
                let onDeleteBlock = onDeletedBlocks[tempChannel]
                
                invokeEvent(cache: cache,
                            attributes: attributes,
                            onCreateBlock: onCreateBlock,
                            onUpdateBlock: onUpdateBlock,
                            onDeleteBlock: onDeleteBlock)
                cachedAttrs[channel] = attributes
                return
            }
            
            /** cache 不存在的情况下，onOpdate **/
            guard let tempChannel = channels[sceneName] else {
                return
            }
            if let onUpdateBlock = onUpdatedBlocks[tempChannel] {
                for arrt in attributes {
                    onUpdateBlock(arrt.toAttribute())
                }
            }
            cachedAttrs[channel] = attributes
            return
        }
        
        if let cache = self.cachedAttrs[channel] { /** 2. collection **/
            let onCreateBlock = onCreateBlocks[channel]
            let onUpdateBlock = onUpdatedBlocks[channel]
            let onDeleteBlock = onDeletedBlocks[channel]
            
            invokeEvent(cache: cache,
                        attributes: attributes,
                        onCreateBlock: onCreateBlock,
                        onUpdateBlock: onUpdateBlock,
                        onDeleteBlock: onDeleteBlock)
            cachedAttrs[channel] = attributes
            return
        }
        
        if let onUpdateBlock = onUpdatedBlocks[channel] { /** 3. scene.id or scene.id + key **/
            for arrt in attributes {
                onUpdateBlock(arrt.toAttribute())
            }
            return
        }
    }
    
    fileprivate func invokeEvent(cache: [AgoraRtmChannelAttribute],
                                 attributes: [AgoraRtmChannelAttribute],
                                 onCreateBlock: OnSubscribeBlock?,
                                 onUpdateBlock: OnSubscribeBlock?,
                                 onDeleteBlock: OnSubscribeBlock?) {
        var onlyA = [IObject]()
        var onlyB = [IObject]()
        var both = [IObject]()
        var temp = [String : AgoraRtmChannelAttribute]()
        
        for i in cache {
            temp[i.key] = i
        }
        
        for b in attributes {
            if let i = temp[b.key] {
                if b.value != i.value {
                    both.append(b.toAttribute())
                }
                temp.removeValue(forKey: b.key)
            }
            else{
                onlyB.append(b.toAttribute())
            }
        }
        
        for i in temp.values {
            onlyA.append(i.toAttribute())
        }
        
        if let onUpdateBlockTemp = onUpdateBlock {
            for i in both {
                onUpdateBlockTemp(i)
            }
        }
        
        if let onCreateBlockTemp = onCreateBlock {
            for i in onlyB {
                onCreateBlockTemp(i)
            }
        }
        
        if let onDeleteBlockTemp = onDeleteBlock {
            for i in onlyA {
                onDeleteBlockTemp(i)
            }
        }
    }
}


/// sceneRef.delete 没有回调事件
/// AgoraSyncManager.deleteScenes["id"] 删除房间列表的一个房间 没有回调事件
/// sceneRef.update 更新房间信息 subscribe后有回调事件
/// 
