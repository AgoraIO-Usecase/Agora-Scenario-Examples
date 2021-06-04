//  Created by XUCH on 2021/3/3.
//

import Foundation

public enum LogLevel {
    case info, warning, error
    var description: String {
        switch self {
        case .info:    return "Info"
        case .warning: return "Warning"
        case .error:   return "Error"
        }
    }
}

public class Logger {
    public static func log(message: String, level: LogLevel) {
        #if !DEBUG
            if level != .error {
                return
            }
        #endif
        print("\(level.description): \(message)")
    }
}
