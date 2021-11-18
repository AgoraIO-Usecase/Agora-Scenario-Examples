//
//  BaseViewController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/1.
//

import UIKit

class BaseViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        setupNavigationBar()
    }

    private func setupNavigationBar() {
        let viewControllers = navigationController?.viewControllers.count ?? 0
        guard viewControllers > 1 else { return }
        navigationItem.leftBarButtonItem = UIBarButtonItem(image: UIImage(systemName: "chevron.backward")?.withTintColor(.black, renderingMode: .alwaysOriginal), style: .plain, target: self, action: #selector(clickBackButton))
        navigationController?.interactivePopGestureRecognizer?.delegate = self
    }
    
    func navigationTransparent(isTransparent: Bool) {
        let image = isTransparent ? UIImage() : nil
        navigationController?.navigationBar.setBackgroundImage(image, for: .default)
        navigationController?.navigationBar.shadowImage = image
    }

    func showAlert(title: String? = nil, message: String) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let action = UIAlertAction(title: "OK", style: .cancel, handler: nil)
        alertController.addAction(action)
        present(alertController, animated: true, completion: nil)
    }
    func showAlert(title: String? = nil, message: String, confirm: @escaping () -> Void) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let action = UIAlertAction(title: "Confirm".localized, style: .default) { _ in
            confirm()
        }
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(action)
        alertController.addAction(cancel)
        present(alertController, animated: true, completion: nil)
    }
    func showAlert(title: String? = nil, message: String, cancel: (() -> Void)?, confirm: @escaping () -> Void) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let action = UIAlertAction(title: "Confirm".localized, style: .default) { _ in
            confirm()
        }
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
            cancel?()
        }
        alertController.addAction(action)
        alertController.addAction(cancel)
        present(alertController, animated: true, completion: nil)
    }
    func showTextFieldAlert(title: String? = nil, message: String, confirm: @escaping (String) -> Void) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alertController.addTextField { field in
            field.placeholder = "Please enter text"
        }
        let action = UIAlertAction(title: "OK", style: .default) { _ in
            let text = alertController.textFields?.first?.text ?? ""
            confirm(text)
        }
        let cancel = UIAlertAction(title: "cancel", style: .cancel, handler: nil)
        alertController.addAction(action)
        alertController.addAction(cancel)
        present(alertController, animated: true, completion: nil)
    }
    
    @objc
    private func clickBackButton() {
        navigationController?.popViewController(animated: true)
    }
}

extension BaseViewController: UIGestureRecognizerDelegate {
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        true
    }
}

enum LogLevel {
    case info, warning, error
    
    var description: String {
        switch self {
        case .info:    return "Info"
        case .warning: return "Warning"
        case .error:   return "Error"
        }
    }
}

struct LogItem {
    var message:String
    var level:LogLevel
    var dateTime:Date
}

class LogUtils {
    static var logs:[LogItem] = []
    static var appLogPath:String = "\(logFolder())/app-\(Date().getFormattedDate(format: "yyyy-MM-dd")).log"
    
    static func log(message: String, level: LogLevel) {
        LogUtils.logs.append(LogItem(message: message, level: level, dateTime: Date()))
        print("\(level.description): \(message)")
    }
    
    static func logFolder() -> String {
        let folder = "\(NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0])/logs"
        try? FileManager.default.createDirectory(atPath: folder, withIntermediateDirectories: true, attributes: nil)
        return folder
    }
    static func sdkLogPath() -> String {
        let logPath = "\(logFolder())/agorasdk.log"
        return logPath
    }
    
    static func removeAll() {
        LogUtils.logs.removeAll()
    }
    
    static func writeAppLogsToDisk() {
        if let outputStream = OutputStream(url: URL(fileURLWithPath: LogUtils.appLogPath), append: true) {
            outputStream.open()
            for log in LogUtils.logs {
                let msg = "\(log.level.description) \(log.dateTime.getFormattedDate(format: "yyyy-MM-dd HH:mm:ss")) \(log.message)\n"
                let bytesWritten = outputStream.write(msg)
                if bytesWritten < 0 { print("write failure") }
            }
            outputStream.close()
            LogUtils.removeAll()
        } else {
            print("Unable to open file")
        }
    }
    
    static func cleanUp() {
        try? FileManager.default.removeItem(at: URL(fileURLWithPath: LogUtils.logFolder(), isDirectory: true))
    }
}
