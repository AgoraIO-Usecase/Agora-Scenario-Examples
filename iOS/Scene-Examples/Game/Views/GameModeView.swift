//
//  GameModeView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit
import Agora_Scene_Utils

class GameModeView: UIView {
    var didGameModeItemClosure: ((GameModeModel) -> Void)?
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "游戏模式"
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray
        return view
    }()
    public lazy var collectionLayout: AGECollectionView = {
        let view = AGECollectionView()
        let w = (Screen.width - 15 * 4) / 3
        view.itemSize = CGSize(width: w, height: 153)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 15
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 0)
        view.delegate = self
        view.scrollDirection = .horizontal
        view.showsHorizontalScrollIndicator = false
        view.register(GameModeViewCell.self,
                      forCellWithReuseIdentifier: GameModeViewCell.description())
        return view
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        collectionLayout.dataArray = GameModeModel.createDatas()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 15
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMinXMaxYCorner]
        layer.masksToBounds = true
        translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        collectionLayout.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(collectionLayout)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 15).isActive = true
        lineView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -15).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        collectionLayout.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionLayout.topAnchor.constraint(equalTo: lineView.bottomAnchor, constant: 15).isActive = true
        collectionLayout.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionLayout.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor).isActive = true
        collectionLayout.heightAnchor.constraint(equalToConstant: 153).isActive = true
    }
}

extension GameModeView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: GameModeViewCell.description(),
                                                      for: indexPath) as! GameModeViewCell
        cell.setupGameModeData(model: GameModeModel.createDatas()[indexPath.item])
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        didGameModeItemClosure?(GameModeModel.createDatas()[indexPath.item])
    }
}

class GameModeViewCell: UICollectionViewCell {
    private lazy var button: UIButton = {
        let button = UIButton()
        button.layer.cornerRadius = 15
        button.layer.masksToBounds = true
        button.isUserInteractionEnabled = false
        return button
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "连麦PK游戏"
        label.textColor = .black
        label.font = .systemFont(ofSize: 14)
        label.setContentHuggingPriority(.defaultHigh, for: .vertical)
        label.setContentCompressionResistancePriority(.defaultHigh, for: .vertical)
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupGameModeData(model: GameModeModel) {
        button.backgroundColor = model.bgColor
        button.setImage(UIImage(named: model.iconName), for: .normal)
        titleLabel.text = model.title
    }
    
    func setupGameCenterData(model: GameCenterModel) {
        button.setImage(UIImage(named: model.iconName ?? "Game/draw"), for: .normal)
        titleLabel.text = model.gameName
    }
    
    private func setupUI() {
        button.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(button)
        contentView.addSubview(titleLabel)
        
        button.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        button.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        button.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        button.heightAnchor.constraint(equalTo: contentView.widthAnchor).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: button.bottomAnchor, constant: 10).isActive = true
    }
}
