//
//  AskSyncManager+Internal.swift
//  AgoraSyncManager
//
//  Created by ZYP on 2022/2/10.
//

import Foundation
import AgoraSyncKit

extension AskSyncManager {
    /// add room item in room list
    /// - Parameters:
    ///   - id: scene id
    ///   - data: scene data
    /// - Returns: result
    func addScene(id: String, data: String) -> Result<AgoraSyncDocument, SyncError> {
        let roomString = data
        let json = AgoraJson()
        json.setString(roomString)
        let roomJson = AgoraJson()
        roomJson.setObject()
        roomJson.setField("scene", agoraJson: json)
        let semp = DispatchSemaphore(value: 0)
        var targetDocument: AgoraSyncDocument?
        var error: SyncError?
        targetDocument = roomsCollection.createDocument(withName: id)
        targetDocument?.set("", json: roomJson, completion: { errorCode in
            if errorCode == .codeNoError {
                semp.signal()
            }
            else {
                let e = SyncError.ask(message: "addRoom fail ", code: errorCode.rawValue)
                error = e
                semp.signal()
            }
        })
        
        semp.wait()
        
        if let e = error {
            Log.errorText(text: e.description, tag: "AskSyncManager.addRoom")
            return .failure(e)
        }
        
        guard let document = targetDocument else {
            fatalError("never call this")
        }
        
        return .success(document)
    }
}
