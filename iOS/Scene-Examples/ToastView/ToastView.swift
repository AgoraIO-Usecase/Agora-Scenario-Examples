//
//  ToastView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//
import UIKit

enum ToastViewPostion {
    case top, center, bottom
}

class ToastView: UIView {
    private lazy var tagImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        imageView.isHidden = true
        return imageView
    }()
    private lazy var label: UILabel = {
        let label = UILabel()
        label.text = ""
        label.textColor = .white
        label.font = .systemFont(ofSize: 14)
        label.numberOfLines = 0
        label.preferredMaxLayoutWidth = UIScreen.main.bounds.width - 60
        return label
    }()

    var text: String? {
        didSet {
            label.text = text
        }
    }
    var textColor: UIColor? {
        didSet {
            guard let color = textColor else { return }
            label.textColor = color
        }
    }
    var font: UIFont? {
        didSet {
            label.font = font ?? .systemFont(ofSize: 14)
        }
    }
    
    var tagImage: UIImage? {
        didSet {
            guard tagImage != nil else { return }
            tagImageView.image = tagImage
            tagImageView.isHidden = tagImage == nil
        }
    }

    var cornerRadius: CGFloat = 0 {
        didSet {
            layer.cornerRadius = cornerRadius
            layer.masksToBounds = true
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    static func show(text: String, duration: CGFloat = 2.5, view: UIView? = nil) {
        show(text: text, tagImage: nil, textColor: .white, font: nil, duration: duration, postion: .center, view: view)
    }
    
    static func show(text: String, postion: ToastViewPostion = .center, duration: CGFloat = 2.5, view: UIView? = nil) {
        show(text: text, tagImage: nil, textColor: .white, font: nil, duration: duration, postion: postion, view: view)
    }
    
    static func show(text: String, tagImage: UIImage? = nil, postion: ToastViewPostion = .center, view: UIView? = nil) {
        show(text: text, tagImage: tagImage, textColor: .white, font: nil, duration: 2.5, postion: postion, view: view)
    }
    
    static func show(text: String,
                     tagImage: UIImage? = nil,
                     textColor: UIColor = .white,
                     font: UIFont? = nil,
                     duration: CGFloat = 2.5,
                     postion: ToastViewPostion = .center,
                     view: UIView?) {
        guard let currentView = view ?? UIApplication.keyWindow else { return }
        let toastView = ToastView()
        toastView.backgroundColor = UIColor.black.withAlphaComponent(0)
        toastView.cornerRadius = 10
        toastView.text = text
        toastView.tagImage = tagImage
        toastView.textColor = textColor
        toastView.font = font
        currentView.addSubview(toastView)
        toastView.translatesAutoresizingMaskIntoConstraints = false
        toastView.centerXAnchor.constraint(equalTo: currentView.centerXAnchor).isActive = true
        switch postion {
        case .top:
            toastView.topAnchor.constraint(equalTo: currentView.safeAreaLayoutGuide.topAnchor, constant: 30).isActive = true
        case .center:
            toastView.centerYAnchor.constraint(equalTo: currentView.centerYAnchor).isActive = true
        case .bottom:
            toastView.bottomAnchor.constraint(equalTo: currentView.safeAreaLayoutGuide.bottomAnchor, constant: -100).isActive = true
        }
        
        UIView.animate(withDuration: 0.15) {
            toastView.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        } completion: { _ in
            DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                UIView.animate(withDuration: 0.15) {
                    toastView.alpha = 0
                } completion: { _ in
                    toastView.removeFromSuperview()
                }
            }
        }
    }
    
    private func setupUI() {
        backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.6)
        tagImageView.translatesAutoresizingMaskIntoConstraints = false
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(tagImageView)
        addSubview(label)
        
        tagImageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        tagImageView.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        label.leadingAnchor.constraint(equalTo: tagImageView.trailingAnchor, constant: 5).isActive = true
        label.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
        label.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
        label.bottomAnchor.constraint(equalTo: bottomAnchor,constant: -10).isActive = true
    }
}
