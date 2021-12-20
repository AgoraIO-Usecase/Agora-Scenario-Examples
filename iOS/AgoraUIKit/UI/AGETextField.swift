//
//  AGETextField.swift
//  AgoraUIKit
//
//  Created by zhaoyongqiang on 2021/12/14.
//

import UIKit

public class AGETextField: UITextField {
    public var colorStyle: AGETextColorStyle = .black {
        didSet {
            updateTextColor()
        }
    }
    public var fontStyle: AGETextFontStyle = .middle {
        didSet {
            updateTextFont()
        }
    }
    
    public var cornerRadius: CGFloat = 0 {
        didSet {
            layer.cornerRadius = cornerRadius
            layer.masksToBounds = true
        }
    }
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
    public var shadowColor: UIColor? {
        didSet {
            layer.shadowColor = shadowColor?.cgColor
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
    
    init(colorStyle: AGETextColorStyle = .black,
         fontStyle: AGETextFontStyle = .middle) {
        super.init(frame: .zero)
        self.colorStyle = colorStyle
        self.fontStyle = fontStyle
        setupUI()
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        placeholder = "请输入"
        font = fontStyle.font
        cornerRadius = 10
        borderColor = .blueColor
        borderWidth = 1
        textColor = .black
        leftView = UIView(frame: CGRect(x: 10, y: 0, width: 10, height: 0))
        leftViewMode = .always
    }
    
    private func updateTextColor() {
        textColor = colorStyle.color
    }
    private func updateTextFont() {
        font = fontStyle.font
    }
}
