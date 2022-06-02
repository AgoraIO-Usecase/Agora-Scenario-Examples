//
//  EntryViewCell.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/12.
//

import UIKit

class SuperAppRoomListViewCell: UICollectionViewCell {
    let personCountView = IconTextView(frame: .zero)
    let briefView = LabelShadowView(frame: .zero)
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setup() {
        personCountView.isHidden = true
        personCountView.offsetLeftX = -4.0
        personCountView.offsetRightX = 4.0
        personCountView.imageView.image = UIImage(named: "icon-audience")
        personCountView.label.textColor = .white
        personCountView.label.font = UIFont.systemFont(ofSize: 10)
        
        briefView.imageView.layer.cornerRadius = 8
        briefView.imageView.layer.masksToBounds = true
        contentView.addSubview(briefView)
        contentView.addSubview(personCountView)
        personCountView.translatesAutoresizingMaskIntoConstraints = false
        briefView.translatesAutoresizingMaskIntoConstraints = false
        
        personCountView.rightAnchor.constraint(equalTo: contentView.rightAnchor, constant: -6).isActive = true
        personCountView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6).isActive = true
        personCountView.widthAnchor.constraint(equalToConstant: 48).isActive = true
        personCountView.heightAnchor.constraint(equalToConstant: 22).isActive = true
        
        briefView.leftAnchor.constraint(equalTo: contentView.leftAnchor).isActive = true
        briefView.rightAnchor.constraint(equalTo: contentView.rightAnchor).isActive = true
        briefView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        briefView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
    
    func commonInit() {}
    
    func setInfo(_ info: Info) {
        briefView.label.text = info.title
        personCountView.label.text = "\(info.count)"
        briefView.imageView.image = UIImage(named: info.imageName)
    }
}

extension SuperAppRoomListViewCell {
    struct Info {
        let imageName: String
        let title: String
        let count: Int
    }
}

extension SuperAppRoomListViewCell {
    class IconTextView: UIControl {
        private(set) var label = UILabel(frame: CGRect.zero)
        private(set) var imageView = UIImageView(frame: CGRect.zero)
        
        var offsetLeftX: CGFloat = 0
        var offsetRightX: CGFloat = 0
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            self.backgroundColor = UIColor(red: 0,
                                           green: 0,
                                           blue: 0,
                                           alpha: 0.4)
            label.textAlignment = .right
            
            self.addSubview(imageView)
            self.addSubview(label)
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        override func layoutSubviews() {
            super.layoutSubviews()
            
            let height = self.frame.height
            let width = self.frame.width
            let radius = height * 0.5
            self.layer.cornerRadius = radius
            
            let subsTopSpace: CGFloat = 2.0
            let imageViewHeight = height - (subsTopSpace * 2.0)
            let imageViewWidth = imageViewHeight
            let imageX = radius + offsetLeftX
            let imageY = subsTopSpace
             
            let imageViewFrame = CGRect(x: imageX,
                                        y: imageY,
                                        width: imageViewWidth,
                                        height: imageViewHeight)
            imageView.frame = imageViewFrame
            
            let labelHeight = imageViewHeight
            let labelWidth = width - radius - imageViewFrame.maxX + offsetRightX
            let labelX = imageViewFrame.maxX
            let labelY = subsTopSpace
            
            let labelFrame = CGRect(x: labelX,
                                    y: labelY,
                                    width: labelWidth,
                                    height: labelHeight)
            label.frame = labelFrame
        }
    }
    
    class LabelShadowView: UIView {
        fileprivate var shadow = UIImageView(frame: CGRect.zero)
        var shadowOffSetX: CGFloat = 0.0
        var imageView = UIImageView(frame: CGRect.zero)
        var label = UILabel(frame: CGRect.zero)
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            initViews()
        }
        
        required init?(coder: NSCoder) {
            super.init(coder: coder)
            initViews()
        }
        
        override func layoutSubviews() {
            super.layoutSubviews()
            self.imageView.frame = CGRect(x: 0,
                                          y: 0,
                                          width: self.bounds.width,
                                          height: self.bounds.height)
            
            let shadowH: CGFloat = 25.0
            let shadowW: CGFloat = self.bounds.width - (shadowOffSetX * 2)
            let shadowX: CGFloat = shadowOffSetX
            let shadowY: CGFloat = self.bounds.height - shadowH
            self.shadow.frame = CGRect(x: shadowX,
                                       y: shadowY,
                                       width: shadowW,
                                       height: shadowH)
            
            let labelH: CGFloat = 25.0
            let labelW: CGFloat = self.bounds.width - (shadowOffSetX * 2)
            let labelX: CGFloat = shadowOffSetX
            let labelY: CGFloat = self.bounds.height - labelH
            
            self.label.frame = CGRect(x: labelX, y: labelY, width: labelW, height: labelH)
        }
        
        private func initViews() {
            addSubview(imageView)
            
            let shadowImage = UIImage(named: "shadow")!
            let shadowRoundImage = UIImage(named: "shadow-round")!
            let shadowRondStrechImage = shadowRoundImage.stretchableImage(withLeftCapWidth: Int(shadowImage.size.width * 0.5),
                                                                          topCapHeight: Int(shadowImage.size.height * 0.5))
            
            shadow.image = shadowRondStrechImage
            addSubview(shadow)
            
            label.textColor = UIColor.white
            label.font = UIFont.systemFont(ofSize: 11)
            label.textAlignment = .center
            addSubview(label)
        }
    }
}


