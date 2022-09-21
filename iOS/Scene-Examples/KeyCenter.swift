//
//  KeyCenter.swift
//  OpenLive
//
//  Created by GongYuhua on 6/25/16.
//  Copyright © 2016 Agora. All rights reserved.
//

struct KeyCenter {
    static let AppId: String = <#YOUR APPID#>

    // assign token to nil if you have not enabled app certificate
    static let Certificate: String? = <#YOUR Certificate#>
    
    static var Token: String? = nil
    
    // FastBoard Configration
    static let BOARD_APP_ID: String = ""
    static let BOARD_ROOM_UUID: String = ""
    static let BOARD_ROOM_TOKEN: String = ""
}
