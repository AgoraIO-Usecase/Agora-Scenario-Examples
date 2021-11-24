//
//  BORVideoRoomDetailCell.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/1.
//

import UIKit

class BORVideoRoomDetailCell: UICollectionViewCell {
    var clickMuteButtonClosure: ((Bool) -> Void)?
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor(hex: "#ffffff")
        label.font = UIFont.systemFont(ofSize: 14)
        return label
    }()
    private lazy var volumeButton: UIButton = {
       let button = UIButton()
        button.setImage(UIImage(systemName: "mic")?.withTintColor(.red, renderingMode: .alwaysOriginal), for: .normal)
        button.setImage(UIImage(systemName: "mic.slash")?.withTintColor(.red, renderingMode: .alwaysOriginal), for: .selected)
        button.addTarget(self, action: #selector(clickMuteButton), for: .touchUpInside)
        button.isHidden = true
        return button
    }()
    private lazy var videoView = UIView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupUI() {
        videoView.translatesAutoresizingMaskIntoConstraints = false
        volumeButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleLabel)
        contentView.addSubview(videoView)
        contentView.addSubview(volumeButton)
        
        videoView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        videoView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        videoView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        videoView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
        
        volumeButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
        volumeButton.rightAnchor.constraint(equalTo: contentView.rightAnchor, constant: -10).isActive = true
    }
    
    func setupItemData(with item: Any?) {
        guard let model = item as? LiveCanvasModel else { return }
        titleLabel.text = "User-\(model.canvas?.uid ?? 0)"
        model.canvas?.view = videoView
    }
    
    @objc
    private func clickMuteButton(sender: UIButton) {
        sender.isSelected = !sender.isSelected
        clickMuteButtonClosure?(sender.isSelected)
    }
}
