//
//  GameView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit
import WebKit

enum GameRoleType: Int {
    ///  房主
    case broadcast = 1
    /// 观众
    case audience = 2
}

class GameWebView: UIView {
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
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func loadUrl(urlString: String, roomId: String, roleType: GameRoleType) {
        let avatarUrl = "https://gimg2.baidu.com/image_search/src=http://c-ssl.duitang.com/uploads/blog/202011/17/20201117105437_45d41.thumb.1000_0.jpeg"
        let string = urlString + "?user_id=\(UserInfo.userId)&app_id=\(KeyCenter.gameAppId)&room_id=\(roomId)&identity=\(roleType.rawValue)&token=\(KeyCenter.gameToken)&name=User-\(UserInfo.userId)&avatar=\(avatarUrl)"
        guard let url = URL(string: string) else { return }
        let request = URLRequest(url: url)
        webView.load(request)
    }
    
    func reset() {
        webView.loadHTMLString("https://", baseURL: nil)
    }
    
    private func setupUI() {
        webView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(webView)
        webView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        webView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        webView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        webView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}
