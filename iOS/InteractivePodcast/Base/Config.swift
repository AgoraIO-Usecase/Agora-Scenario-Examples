//
//  BuildConfig.swift
//  InteractivePodcast
//
//  Created by XC on 2021/4/20.
//

import Foundation
import AgoraRtcKit
#if LEANCLOUD
import Core_LeanCloud
#elseif FIREBASE
import Core_Firebase
#endif

extension BuildConfig {
    static var PrivacyPolicy: String {
        if (Utils.getCurrentLanguage() == "cn") {
            return "https://www.agora.io/cn/privacy-policy/"
        } else {
            return "https://www.agora.io/en/privacy-policy/"
        }
    }
    static var SignupUrl: String {
        if (Utils.getCurrentLanguage() == "cn") {
            return "https://sso.agora.io/cn/v3/signup"
        } else {
            return "https://sso.agora.io/en/v3/signup"
        }
    }
    static let PublishTime = "2021.XX.XX"
    static let SdkVersion = AgoraRtcEngineKit.getSdkVersion()
    static var AppVersion: String? {
        return Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String
    }
}
