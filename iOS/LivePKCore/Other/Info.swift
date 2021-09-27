//
//  Info.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import Foundation

struct LoginInfo {
    let role: Role
    let roomName: String
}

enum Role {
    case broadcaster
    case audience
}

struct RenderInfo {
    let isLocal: Bool
    let uid: UInt
    let roomName: String
    let type: RenderType
}

enum RenderType {
    case rtc
    case cdn
}

