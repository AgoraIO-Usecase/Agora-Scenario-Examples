//
//  LogProvider.swift
//  AFNetworking
//
//  Created by ZYP on 2021/5/28.
//

import Foundation
import AgoraLog

class LogProvider {
    static let `default` = LogProvider()
    private static let folderPath = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true).first!.appending("/Logs")
    private let logger = AgoraLogger(folderPath: folderPath, filePrefix: "PKLive", maximumNumberOfFiles: 5)
    
    init() {
        logger.setPrintOnConsoleType(.all)
    }
    
    func error(error: Error?,
               tag: String?,
               domainName: String) {
        guard let e = error else {
            return
        }
        var text = "<can not get error info>"
        if e.localizedDescription.count > 1 {
            text = e.localizedDescription
        }
        
        let err = e as CustomStringConvertible
        if err.description.count > 1 {
            text = err.description
        }
        
        errorText(text: text,
                  tag: tag,
                  domainName: domainName)
    }
    
    func errorNs(nsError: NSError?,
                 tag: String?,
                 domainName: String) {
        guard let text = nsError?.localizedDescription else { return }
        errorText(text: text,
                  tag: tag,
                  domainName: domainName)
    }
    
    func errorText(text: String,
                   tag: String?,
                   domainName: String) {
        log(type: .error,
            text: text,
            tag: tag,
            domainName: domainName)
    }
    
    func info(text: String,
              tag: String?,
              domainName: String) {
        log(type: .info,
            text: text,
            tag: tag,
            domainName: domainName)
    }
    
    func warning(text: String,
                 tag: String?,
                 domainName: String) {
        log(type: .warning,
            text: text,
            tag: tag,
            domainName: domainName)
    }
    
    func debug(text: String,
               tag: String?,
               domainName: String) {
        log(type: .debug,
            text: text,
            tag: tag,
            domainName: domainName)
    }
    
    func log(type: AgoraLogType,
             text: String,
             tag: String?,
             domainName: String) {
        let levelName = type.name
        let string = getString(text: text,
                               tag: tag,
                               levelName: levelName,
                               domainName: domainName)
        logger.log(string,
                   type: type)
    }
    
    func getString(text: String,
                   tag: String?,
                   levelName: String,
                   domainName: String) -> String {
        if let `tag` = tag {
            return "[\(domainName)][\(levelName)][\(tag)]: " + text
        }
        return "[\(domainName)][\(levelName)]: " + text
    }
}

extension AgoraLogType {
    var name: String {
        switch self {
        case .debug:
            return "Debug"
        case .info:
            return "Info"
        case .error:
            return "Error"
        case .warning:
            return "Warning"
        default:
            return "none"
        }
    }
}

class Log {
    static func errorText(text: String,
                          tag: String? = nil) {
        LogProvider.default.errorText(text: text,
                                      tag: tag,
                                      domainName: "pkLive")
    }
    
    static func error(error: CustomStringConvertible,
                      tag: String? = nil) {
        LogProvider.default.errorText(text: error.description,
                                      tag: tag,
                                      domainName: "pkLive")
    }
    
    static func info(text: String,
                     tag: String? = nil) {
        LogProvider.default.info(text: text,
                                 tag: tag,
                                 domainName: "pkLive")
    }
    
    static func debug(text: String,
                      tag: String? = nil) {
        LogProvider.default.debug(text: text,
                                  tag: tag,
                                  domainName: "pkLive")
    }
}


