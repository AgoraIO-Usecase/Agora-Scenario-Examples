//
//  MainVM+Handle.swift
//  LivePK
//
//  Created by ZYP on 2021/9/26.
//

import Foundation

extension MainVM {
    func isPking(attributes: [PKSyncManager.Attribute]) -> Bool {
        return attributes.contains(where: { $0.key == "PK"})
    }
}
