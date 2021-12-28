//
//  Blocks.swift
//  SyncManager
//
//  Created by ZYP on 2021/11/15.
//

import Foundation

public typealias SuccessBlockInt = (Int) -> ()
public typealias SuccessBlock = ([IObject]) -> ()
public typealias SuccessBlockVoid = () -> ()
public typealias SuccessBlockObj = (IObject) -> ()
public typealias SuccessBlockObjOptional = (IObject?) -> ()
public typealias FailBlock = (SyncError) -> ()

public typealias OnSubscribeBlock = SuccessBlockObj
public typealias OnSubscribeBlockVoid = SuccessBlockVoid

