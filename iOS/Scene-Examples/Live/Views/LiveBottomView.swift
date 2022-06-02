//
//  LiveBottomView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit

class LiveBottomView: UIView {
    var clickBottomButtonTypeClosure: ((LiveBottomType) -> Void)?
    var clickChatButtonClosure: ((String) -> Void)?
    
    enum LiveBottomType: Int {
        case tool = 1
        case close = 2
        case gift = 3
        case pk = 4
        case game = 5
        case exitgame = 6
        /// 美声
        case belcanto = 7
        /// 音效
        case effect = 8
        /// 音效工具
        case effect_tool = 9
        var imageName: String {
            switch self {
            case .tool: return "icon-more-gray"
            case .close: return "icon-close-gray"
            case .gift: return "icon-gift"
            case .pk: return "PK/pic-PK"
            case .game: return "Game/gameicon"
            case .exitgame: return ""
            case .belcanto: return "icon-美声"
            case .effect: return "icon-音效"
            case .effect_tool: return "icon-more-gray"
            }
        }
        
        var title: String {
            switch self {
            case .exitgame: return "quit_the_game".localized
            default: return ""
            }
        }
        
        var size: CGSize {
            switch self {
            case .exitgame: return CGSize(width: 80.fit, height: 38)
            default:
                return CGSize(width: 38, height: 38)
            }
        }
    }
    
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .horizontal
        stackView.distribution = .fill
        stackView.spacing = 15.fit
        stackView.setContentHuggingPriority(.defaultHigh, for: .horizontal)
        stackView.setContentCompressionResistancePriority(.defaultHigh, for: .horizontal)
        return stackView
    }()
    private lazy var chatButton: UIButton = {
        let button = UIButton()
        button.backgroundColor = UIColor(hex: "#000000", alpha: 0.6)
        button.setTitle("Live_Text_Input_Placeholder".localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14.fit)
        button.layer.cornerRadius = 19
        button.layer.masksToBounds = true
        button.setContentHuggingPriority(.defaultLow, for: .horizontal)
        button.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        button.addTarget(self, action: #selector(clickChatButton), for: .touchUpInside)
        button.contentHorizontalAlignment = .leading
        if #available(iOS 15.0, *) {
            var conf = UIButton.Configuration.borderedTinted()
            conf.baseBackgroundColor = UIColor(hex: "#000000", alpha: 0.6)
            conf.contentInsets = NSDirectionalEdgeInsets(top: 0, leading: 15, bottom: 0, trailing: 0)
            button.configuration = conf
        } else {
            button.titleEdgeInsets = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 0)
        }
        return button
    }()
    private var type: [LiveBottomType] = []
    
    init(type: [LiveBottomType]) {
        super.init(frame: .zero)
        self.type = type
        setupUI()
        createBottomButton()
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateButtonType(type: [LiveBottomType]) {
        self.type = type
        statckView.subviews.forEach({
            $0.removeFromSuperview()
        })
        createBottomButton()
    }
    
    private func setupUI() {
        chatButton.translatesAutoresizingMaskIntoConstraints = false
        statckView.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(chatButton)
        addSubview(statckView)
        chatButton.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        chatButton.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -15).isActive = true
        chatButton.heightAnchor.constraint(equalToConstant: 38).isActive = true
        chatButton.topAnchor.constraint(equalTo: topAnchor).isActive = true
//        chatButton.widthAnchor.constraint(equalToConstant: 100.fit).isActive = true
        
        statckView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        statckView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        statckView.heightAnchor.constraint(equalToConstant: 38).isActive = true
        statckView.leadingAnchor.constraint(equalTo: chatButton.trailingAnchor, constant: 15).isActive = true
    }
    
    private func createBottomButton() {
        guard !type.isEmpty else { return }
        type.forEach({
            let button = UIButton()
            button.layer.cornerRadius = 19
            button.layer.masksToBounds = true
            button.backgroundColor = UIColor(hex: "#000000", alpha: 0.6)
            button.setImage(UIImage(named: $0.imageName), for: .normal)
            button.setTitle($0.title, for: .normal)
            button.setTitleColor(.white, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 14)
            button.tag = $0.rawValue
            button.addTarget(self, action: #selector(clickButtonHandler(sender:)), for: .touchUpInside)
            statckView.addArrangedSubview(button)
            button.translatesAutoresizingMaskIntoConstraints = false
            button.widthAnchor.constraint(equalToConstant: $0.size.width).isActive = true
            button.heightAnchor.constraint(equalToConstant: $0.size.height).isActive = true
        })
    }
    
    @objc
    private func clickChatButton() {
//        ToastView.show(text: "没实现")
        let chatMessageView = LiveChatMessageView()
        chatMessageView.clickKeyboardSendClosure = clickChatButtonClosure
        AlertManager.show(view: chatMessageView, alertPostion: .bottom)
    }
    
    @objc
    private func clickButtonHandler(sender: UIButton) {
        guard let buttonType = LiveBottomType(rawValue: sender.tag) else { return }
        LogUtils.log(message: "button == \(buttonType)", level: .info)
        clickBottomButtonTypeClosure?(buttonType)
    }
}
