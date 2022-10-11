//
//  BaseViewController.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/1.
//

import UIKit
import Agora_Scene_Utils

class BaseViewController: UIViewController {
    lazy var backButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage(systemName: "chevron.backward")?
                            .withTintColor(.white, renderingMode: .alwaysOriginal),
                        for: .normal)
        button.contentHorizontalAlignment = .left
        button.addTarget(self, action: #selector(onTapBackButton), for: .touchUpInside)
        return button
    }()
    override func viewDidLoad() {
        super.viewDidLoad()
        view.layer.contents = UIImage(named: "default_bg")?.cgImage
        setupNavigationBar()
        NotificationCenter.default.addObserver(self, selector: #selector(appBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillTerminate), name: UIApplication.willTerminateNotification, object: nil)
    }
    
    private func setupNavigationBar() {
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.navigationBar.barTintColor = UIColor(hex: "#0090f3")
//        navigationController?.navigationBar.setBackgroundImage(UIImage(), for: .any, barMetrics: .default)
//        navigationController?.navigationBar.shadowImage = UIImage()
        let viewControllers = navigationController?.viewControllers.count ?? 0
        guard viewControllers > 1 else { return }
        backButton.translatesAutoresizingMaskIntoConstraints = false
        backButton.widthAnchor.constraint(equalToConstant: 40).isActive = true
        backButton.heightAnchor.constraint(equalToConstant: 40).isActive = true
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: backButton)
        navigationController?.interactivePopGestureRecognizer?.delegate = self
    }
    
    func navigationTransparent(isTransparent: Bool, isHiddenNavBar: Bool = false) {
        let image = isTransparent ? UIImage() : nil
        navigationController?.navigationBar.setBackgroundImage(image, for: .any, barMetrics: .default)
        navigationController?.navigationBar.shadowImage = image
        navigationController?.navigationBar.barTintColor = isTransparent ? .clear : UIColor(hex: "#0090f3")
        navigationController?.navigationBar.isHidden = isHiddenNavBar
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
    func onTapBackButton() {
        navigationController?.popViewController(animated: true)
    }
    
    /** 程序进入前台 开始活跃 */
    @objc
    public func appBecomeActive() { }
    
    /** 程序进入后台 */
    @objc
    public func appEnterBackground() { }
    
    /** 程序被杀死 */
    @objc
    public func applicationWillTerminate() { }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
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
