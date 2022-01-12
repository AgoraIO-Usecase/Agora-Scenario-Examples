//
//  GameView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit
import WebKit

enum GameRoleType: Int, CaseIterable {
    ///  房主
    case broadcast = 1
    /// 玩家
    case player = 2
    /// 观众
    case audience = 3
}

class GameWebView: UIView {
    var onMuteAudioClosure: ((Bool) -> Void)?
    var onLeaveGameClosure: (() -> Void)?
    var onChangeGameRoleClosure: ((_ oldRole: GameRoleType, _ newRole: GameRoleType) -> Void)?
    
    private(set) lazy var webView: WKWebView = {
        let config = WKWebViewConfiguration()
        config.allowsAirPlayForMediaPlayback = true
        config.allowsInlineMediaPlayback = true
        config.allowsInlineMediaPlayback = true
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }()
    private lazy var methodNames: [String] = []
    private lazy var viewModel = GameViewModel(channleName: "", ownerId: "")
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        injectJsBridge(methodName: "agoraJSBridge_enableAudio")
        injectJsBridge(methodName: "agoraJSBridge_leave")
        injectJsBridge(methodName: "agoraJSBridge_setRole")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func loadUrl(gameId: String, roomId: String, roleType: GameRoleType) {
        let avatarUrl = "https://gimg2.baidu.com/image_search/src=http://c-ssl.duitang.com/uploads/blog/202011/17/20201117105437_45d41.thumb.1000_0.jpeg"
        viewModel.joinGame(gameId: gameId, roomId: roomId, identity: "\(roleType.rawValue)", avatar: avatarUrl) { [weak self] url in
            guard let url = URL(string: url) else { return }
            let request = URLRequest(url: url)
            self?.webView.load(request)
        }
    }
    
    func reset() {
        guard let url = URL(string: "http://resetwebview.com") else { return }
        let request = URLRequest(url: url)
        webView.load(request)
    }
    
    private func injectJsBridge(methodName: String) {
        webView.configuration.userContentController.add(WeakScriptMessageDelegate(self), name: methodName)
        methodNames.append(methodName)
    }
    
    private func setupUI() {
        webView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(webView)
        webView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        webView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    deinit {
        methodNames.forEach({
            webView.configuration.userContentController.removeScriptMessageHandler(forName: $0)
        })
        print("WKWebViewController is deinit")
    }
}

extension GameWebView: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        LogUtils.log(message: "messageName == \(message.name)", level: .info)
        let params = message.body as? [String: Any]
        LogUtils.log(message: "messageBody == \(message.body)", level: .info)
        if message.name == "agoraJSBridge_enableAudio" {
            let state = params?["state"] as? Int ?? 1
            onMuteAudioClosure?(state == 1)
        } else if message.name == "agoraJSBridge_leave" {
            onLeaveGameClosure?()
        } else if message.name == "agoraJSBridge_setRole" {
            let oldRole = params?["oldRole"] as? Int ?? 2
            let newRole = params?["newRole"] as? Int ?? 2
            onChangeGameRoleClosure?(GameRoleType(rawValue: oldRole) ?? .audience,
                                     GameRoleType(rawValue: newRole) ?? .player)
        }
    }
}

class WeakScriptMessageDelegate: NSObject, WKScriptMessageHandler {
    weak var scriptDelegate: WKScriptMessageHandler?
    
    init(_ scriptDelegate: WKScriptMessageHandler) {
        self.scriptDelegate = scriptDelegate
        super.init()
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        scriptDelegate?.userContentController(userContentController, didReceive: message)
    }
    
    deinit {
        print("WeakScriptMessageDelegate is deinit")
    }
}
