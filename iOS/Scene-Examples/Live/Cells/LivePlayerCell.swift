//
//  LivePlayerCell.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/11.
//

import UIKit

class LivePlayerCell: UICollectionViewCell {
    private lazy var liveView: UIView = {
        let view = UIView()
        return view
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        liveView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(liveView)
        liveView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: 44).isActive = true
    }
    
    func setupPlayerCanvas(with item: Any?) {
        guard let model = item as? LiveCanvasModel else { return }
        model.canvas?.view = liveView
    }
}
