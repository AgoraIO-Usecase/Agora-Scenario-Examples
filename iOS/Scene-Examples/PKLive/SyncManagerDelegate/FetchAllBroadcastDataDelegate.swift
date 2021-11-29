//
//  FetchPKinfoDataDelegate.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/15.
//

import UIKit

class FetchAllBroadcastDataDelegate: IObjectListDelegate {
    var onSuccess: (([IObject]) -> Void)?
    func onSuccess(result: [IObject]) {
        onSuccess?(result)
    }
    
    func onFailed(code: Int, msg: String) {
        LogUtils.log(message: "code == \(code) msg == \(msg)", level: .info)
    }
}
