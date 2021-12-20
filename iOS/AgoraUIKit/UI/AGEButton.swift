//
//  AGEButton.swift
//  AgoraUIKit
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

// MARK: ButtonStyle
public enum AGEButtonStyle {
    case filled(backgroundColor: UIColor?)
    case outline(borderColor: UIColor?)
    case createLive
    case startLive
    case switchCamera(imageColor: UIColor?)
    case setting
    case delete(imageColor: UIColor?)
    case mic(imageColor: UIColor?)
    case muteMic(imageColor: UIColor?)
    case play(imageColor: UIColor?)
    case pause(imageColor: UIColor?)
    case none
    
    var cornerRadius: CGFloat {
        switch self {
        case .filled, .outline:
            return 8
            
        case .startLive: return 20
        case .none: return 10
            
        default: return 0
        }
    }
}


public class AGEButton: UIButton {
    public enum ImagePosition {
        case top
        case left
        case bottom
        case right
    }
    
    public var onClickButtonClosure: ((UIButton) -> Void)?
    
    public var buttonStyle: AGEButtonStyle = .none {
        didSet {
            update()
        }
    }
    public var colorStyle: AGETextColorStyle? {
        didSet {
            updateTextColor()
        }
    }
    public var fontStyle: AGETextFontStyle? {
        didSet {
            updateTextFont()
        }
    }
    
    /// 图片大小, 默认取图片大小
    public var imageSize: CGSize?
    /// 设置圆角
    public var cornerRadius: CGFloat = 0 {
        didSet {
            layer.cornerRadius = cornerRadius
            layer.masksToBounds = true
        }
    }
    /// 设置某个角圆角
    public var maskedCorners: CACornerMask? {
        didSet {
            guard let corners = maskedCorners else { return }
            layer.maskedCorners = corners
        }
    }
    public var borderWidth: CGFloat = 0 {
        didSet {
            layer.borderWidth = borderWidth
        }
    }
    public var borderColor: UIColor = .clear {
        didSet {
            layer.borderColor = borderColor.cgColor
        }
    }
    public var shadowPath: CGPath? {
        didSet {
            layer.shadowPath = shadowPath
        }
    }
    public var shadowColor: UIColor = .clear {
        didSet {
            layer.shadowColor = shadowColor.cgColor
        }
    }
    public var shadowOffset: CGSize = .zero {
        didSet {
            layer.shadowOffset = shadowOffset
        }
    }
    public var shadowRadius: CGFloat = 0 {
        didSet {
            layer.shadowRadius = shadowRadius
        }
    }
    public var shadowOpacity: Float = 0 {
        didSet {
            layer.shadowOpacity = shadowOpacity
        }
    }
    
    /// 图片和文字之间的间距
    private var spacing: CGFloat = 5
    /// 图片位置
    private var position: ImagePosition = .left
    
    init(style: AGEButtonStyle = .none,
         colorStyle: AGETextColorStyle? = nil,
         fontStyle: AGETextFontStyle? = nil) {
        super.init(frame: .zero)
        buttonStyle = style
        self.colorStyle = colorStyle
        self.fontStyle = fontStyle
        setupUI()
        update()
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        update()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        setContentCompressionResistancePriority(.required, for: .horizontal)
        setContentCompressionResistancePriority(.required, for: .vertical)
        setContentHuggingPriority(.required, for: .horizontal)
        setContentHuggingPriority(.required, for: .vertical)
        
        setTitle("按钮", for: .normal)
        setTitleColor(colorStyle?.color ?? .white, for: .normal)
        titleLabel?.font = fontStyle?.font ?? .systemFont(ofSize: 14)
        addTarget(self,
                  action: #selector(onClickButton(sender:)),
                  for: .touchUpInside)
    }
    public func setImage(_ image: UIImage?,
                         for state: UIControl.State,
                         postion: ImagePosition,
                         spacing: CGFloat = 5) {
        self.position = postion
        self.spacing = spacing
        setImage(image, for: state)
        titleLabel?.textAlignment = .center
    }
    
    private func update() {
        cornerRadius = buttonStyle.cornerRadius
        backgroundColor = .clear
        setTitleColor(.clear, for: .normal)
        titleLabel?.font = fontStyle?.font ?? .systemFont(ofSize: 14)
        borderWidth = 0
        borderColor = .clear
        switch buttonStyle {
        case .filled(let bgColor):
            backgroundColor = bgColor ?? .blueColor
            setTitleColor(colorStyle?.color ?? .white, for: .normal)
            borderWidth = 0
            borderColor = .clear
            
        case .outline(let boardColor):
            backgroundColor = .clear
            setTitleColor(colorStyle?.color ?? .blueColor, for: .normal)
            borderWidth = 0.5
            borderColor = boardColor ?? .blueColor
            
        case .startLive:
            backgroundColor = .blueColor
            setTitle("开始直播", for: .normal)
            setTitleColor(colorStyle?.color ?? .white, for: .normal)
            translatesAutoresizingMaskIntoConstraints = false
            widthAnchor.constraint(equalToConstant: 100).isActive = true
            heightAnchor.constraint(equalToConstant: 40).isActive = true
            
        case .setting:
            setImage(UIImage(named: "ImagesBundle.bundle/setting_icon"), for: .normal)
            
        case .createLive:
            setImage(UIImage(named: "ImagesBundle.bundle/create_room"), for: .normal)
            
        case .switchCamera(let imageColor):
            setImage(UIImage(systemName: "camera")?.withTintColor(imageColor ?? .blueColor, renderingMode: .alwaysOriginal), for: .normal)
            
        case .delete(let imageColor):
            setImage(UIImage(systemName: "trash.circle")?.withTintColor(imageColor ?? .blueColor, renderingMode: .alwaysOriginal), for: .normal)
            
        case .mic(let imageColor):
            setImage(UIImage(systemName: "mic")?.withTintColor(imageColor ?? .blueColor, renderingMode: .alwaysOriginal), for: .normal)

        case .muteMic(let imageColor):
            setImage(UIImage(systemName: "mic.fill")?.withTintColor(imageColor ?? .blueColor, renderingMode: .alwaysOriginal), for: .normal)
        
        case .play(let imageColor):
            setImage(UIImage(systemName: "play.circle")?.withTintColor(imageColor ?? .blueColor, renderingMode: .alwaysOriginal), for: .normal)
            
        case .pause(let imageColor):
            setImage(UIImage(systemName: "pause.circle")?.withTintColor(imageColor ?? .blueColor, renderingMode: .alwaysOriginal), for: .normal)
        
        case .none:
            backgroundColor = .clear
            setTitleColor(colorStyle?.color ?? .blueColor, for: .normal)
            borderWidth = 0
            borderColor = .clear
        }
    }
    
    private func updateTextColor() {
        setTitleColor(colorStyle?.color, for: .normal)
    }
    private func updateTextFont() {
        titleLabel?.font = fontStyle?.font
    }
    
    override open func layoutSubviews() {
        super.layoutSubviews()
        titleLabel?.sizeToFit()
        let labelSize = titleLabel?.frame.size ?? .zero
        let imageSize = self.imageSize ?? imageView?.frame.size ?? .zero

        let totalWidth = labelSize.width + imageSize.width + spacing
        let totalHeight = labelSize.height + imageSize.height + spacing

        var imageFrame: CGRect = .zero
        var labelFrame: CGRect = .zero

        switch position {
        case .left:
            imageFrame = CGRect(origin: CGPoint(x: bounds.width / 2.0 - totalWidth / 2.0,
                                                y: bounds.height / 2.0 - imageSize.height / 2.0),
                                size: CGSize(width: imageSize.width, height: imageSize.height))
            labelFrame = CGRect(origin: CGPoint(x: imageFrame.maxX + spacing,
                                                y: bounds.height / 2.0 - labelSize.height / 2.0),
                                size: CGSize(width: labelSize.width, height: labelSize.height))

        case .right:
            labelFrame = CGRect(x: bounds.width / 2.0 - totalWidth / 2.0,
                                y: bounds.height / 2.0 - labelSize.height / 2.0,
                                width: labelSize.width,
                                height: labelSize.height)
            imageFrame = CGRect(x: labelFrame.maxX + spacing,
                                y: bounds.height / 2.0 - imageSize.height / 2.0,
                                width: imageSize.width,
                                height: imageSize.height)

        case .top:
            imageFrame = CGRect(x: bounds.width / 2.0 - imageSize.width / 2.0,
                                y: bounds.height / 2.0 - totalHeight / 2.0,
                                width: imageSize.width,
                                height: imageSize.height)
            labelFrame = CGRect(origin: CGPoint(x: 5, y: imageFrame.maxY + spacing),
                                size: CGSize(width: bounds.width - 10, height: labelSize.height))

        case .bottom:
            labelFrame = CGRect(origin: CGPoint(x: 5, y: bounds.height / 2.0 - totalHeight / 2.0),
                                size: CGSize(width: bounds.width - 10, height: labelSize.height))
            imageFrame = CGRect(origin: CGPoint(x: bounds.width / 2.0 - imageSize.width / 2.0,
                                                y: labelFrame.maxY + spacing),
                                size: CGSize(width: imageSize.width, height: imageSize.height))
        }

        titleLabel?.frame = labelFrame
        imageView?.frame = imageFrame
    }

    open func rotateUp() {
        guard let imageView = imageView else { return }
        UIView.animate(withDuration: 0.3) {
            imageView.transform = .identity
        }
    }

    open func rotateDown() {
        guard let imageView = imageView else { return }
        UIView.animate(withDuration: 0.3) {
            imageView.transform = CGAffineTransform(rotationAngle: .pi)
        }
    }
    
    override open func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        alpha = 0.65
    }

    override open func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        alpha = 1
    }

    override open func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        alpha = 1
    }
    
    @objc
    private func onClickButton(sender: UIButton) {
        onClickButtonClosure?(sender)
    }
}
