//
//  LiveOnlineView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/12/15.
//

import UIKit
import AgoraUIKit_iOS

class LiveOnlineView: UIView {
    private lazy var onLineView: UIView = {
        let view = UIView()
        view.backgroundColor = .black.withAlphaComponent(0.4)
        view.layer.cornerRadius = 14
        view.layer.masksToBounds = true
        return view
    }()
    private lazy var personImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(systemName: "person")?.withTintColor(.white, renderingMode: .alwaysOriginal))
        return imageView
    }()
    private lazy var onLineLabel: UILabel = {
        let label = UILabel()
        label.text = "4"
        label.textColor = .white
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.itemSize = CGSize(width: 28, height: 28)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 10
        view.delegate = self
        view.scrollDirection = .horizontal
        view.register(LiveOnLineViewCell.self,
                      forCellWithReuseIdentifier: "LiveOnLineViewCell")
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
        onLineView.translatesAutoresizingMaskIntoConstraints = false
        personImageView.translatesAutoresizingMaskIntoConstraints = false
        onLineLabel.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(onLineView)
        onLineView.addSubview(personImageView)
        onLineView.addSubview(onLineLabel)
        addSubview(collectionView)
        
        heightAnchor.constraint(equalToConstant: 28).isActive = true
        onLineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -1).isActive = true
        onLineView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        onLineView.heightAnchor.constraint(equalTo: heightAnchor).isActive = true
        personImageView.leadingAnchor.constraint(equalTo: onLineView.leadingAnchor, constant: 10).isActive = true
        personImageView.centerYAnchor.constraint(equalTo: onLineView.centerYAnchor).isActive = true
        
        onLineLabel.leadingAnchor.constraint(equalTo: personImageView.trailingAnchor, constant: 6).isActive = true
        onLineLabel.centerYAnchor.constraint(equalTo: onLineView.centerYAnchor).isActive = true
        onLineLabel.trailingAnchor.constraint(equalTo: onLineView.trailingAnchor,constant: -10).isActive = true
        
        collectionView.trailingAnchor.constraint(equalTo: onLineView.leadingAnchor, constant: -10).isActive = true
        collectionView.heightAnchor.constraint(equalTo: onLineView.heightAnchor).isActive = true
//        collectionView.widthAnchor.constraint(equalToConstant: 118).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.dataArray = ["1", "2", "3", "4"]
    }
}
extension LiveOnlineView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "LiveOnLineViewCell", for: indexPath)
        
        return cell
    }
}

class LiveOnLineViewCell: UICollectionViewCell {
    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: String(format: "portrait%02d", Int.random(in: 1...14))))
        imageView.contentMode = .scaleAspectFill
        imageView.layer.cornerRadius = 14
        imageView.layer.masksToBounds = true
        return imageView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(avatarImageView)
        avatarImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        avatarImageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        avatarImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        avatarImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
}
