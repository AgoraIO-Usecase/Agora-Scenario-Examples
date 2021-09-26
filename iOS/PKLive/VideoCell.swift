//
//  VideoCell.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import UIKit

class VideoCell: UICollectionViewCell {
    let videoView = UIView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setup() {
        backgroundColor = .gray
        contentView.addSubview(videoView)
        videoView.translatesAutoresizingMaskIntoConstraints = false
        videoView.leftAnchor.constraint(equalTo: contentView.leftAnchor).isActive = true
        videoView.rightAnchor.constraint(equalTo: contentView.rightAnchor).isActive = true
        videoView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        videoView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
    
    func commonInit() {}
}
