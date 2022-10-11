//
//  AgoraClubProgramModel.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/4/22.
//

import UIKit

enum AgoraClubProgramType: String {
    case miracle
    case playing
    
    var videoUrl: String {
        switch self {
        case .miracle: return "https://webdemo-pull-hdl.agora.io/lbhd/sample1.flv"
        case .playing: return "https://webdemo-pull-hdl.agora.io/lbhd/sample2.flv"
        }
    }
}

class AgoraClubProgramModel: NSObject {
    
    var type: AgoraClubProgramType = .miracle
    
    
    static func createData() -> [AgoraClubProgramModel] {
        var tempArray = [AgoraClubProgramModel]()
        var model = AgoraClubProgramModel()
        model.type = .miracle
        tempArray.append(model)
        model = AgoraClubProgramModel()
        model.type = .playing
        tempArray.append(model)
        return tempArray
    }
}
