//
//  LeancloudManager+Internal.swift
//  AgoraSyncManager
//
//  Created by zhaoyongqiang on 2022/4/28.
//

import UIKit
import LeanCloud

extension LeancloudManager {
    /// add room item in room list
    /// - Parameters:
    ///   - id: scene id
    ///   - data: scene data
    /// - Returns: result
    func addScene(id: String, data: String) -> String {
        let object = LCObject(className: defaultChannelName)
        let acl = LCACL()
        acl.setAccess([.read, .write], allowed: true)
        try? object.set("data", value: data)
        object.ACL = acl
        object.save { result in
            switch result {
            case .success:
                Log.info(text: "add scene success ", tag: "scene")
                
            case let .failure(error: error):
                let error = SyncError(message: error.localizedDescription,
                                      code: error.errorCode)
                print("error == \(error)")
            }
        }
        return object.objectId?.value ?? ""
    }
}
