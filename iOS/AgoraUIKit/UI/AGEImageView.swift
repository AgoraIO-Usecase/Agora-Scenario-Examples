//
//  AGEImageView.swift
//  AgoraUIKit
//
//  Created by zhaoyongqiang on 2021/12/13.
//

import UIKit

public enum AGEImageType: String {
    case avatar = "person.circle"
    case placeHolder = "pic-placeholding"
}

public class AGEImageView: UIImageView {
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
    
    private var imageType: AGEImageType = .placeHolder {
        didSet {
            updateImageType()
        }
    }
    
    init(type: AGEImageType) {
        super.init(frame: .zero)
        self.imageType = type
    }
    
    init(systemName: String) {
        super.init(frame: .zero)
        image = UIImage(systemName: systemName)
    }
    init(imageName: String) {
        super.init(frame: .zero)
        image = UIImage(named: imageName)
    }
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        contentMode = .scaleAspectFit
        cornerRadius = 5
        isUserInteractionEnabled = true
    }
    
    private func updateImageType() {
        switch imageType {
        case .avatar:
            image = UIImage(systemName: imageType.rawValue)?.withTintColor(.blueColor, renderingMode: .alwaysOriginal)
            
        default:
            image = UIImage(named: imageType.rawValue)
        }
    }
}
