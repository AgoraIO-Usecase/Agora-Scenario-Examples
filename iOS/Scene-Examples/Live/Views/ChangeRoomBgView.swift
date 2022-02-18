//
//  ChangeRoomBgView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/1/26.
//

import UIKit
import AgoraUIKit_iOS

struct ChangeRoomBGModel {
    var imageName: String = ""
}

class ChangeRoomBgView: UIView {
    
    var didSelectedBgImageClosure: ((String) -> Void)?
    
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        label.text = "background".localized
        label.colorStyle = .white
        return label
    }()
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        let w = (Screen.width - 40) / 3
        view.itemSize = CGSize(width: w, height: w)
        view.edge = UIEdgeInsets(top: 0, left: 10, bottom: 0, right: 10)
        view.minInteritemSpacing = 10
        view.minLineSpacing = 10
        view.delegate = self
        view.scrollDirection = .vertical
        view.register(ChangeRoomBGCell.self,
                      forCellWithReuseIdentifier: ChangeRoomBGCell.description())
        return view
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        createData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createData() {
        var dataArray = [ChangeRoomBGModel]()
        for i in 1...9 {
            let model = ChangeRoomBGModel(imageName: String(format: "BG%02d", i))
            dataArray.append(model)
        }
        collectionView.dataArray = dataArray
    }
    
    private func setupUI() {
        backgroundColor = .init(hex: "#4F506A")
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true

        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        heightAnchor.constraint(equalToConstant: Screen.width + 50).isActive = true
        addSubview(titleLabel)
        addSubview(collectionView)
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}

extension ChangeRoomBgView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: ChangeRoomBGCell.description(), for: indexPath) as! ChangeRoomBGCell
        let model = self.collectionView.dataArray?[indexPath.item] as? ChangeRoomBGModel
        cell.setImageName(model?.imageName ?? "")
        return cell
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let model = self.collectionView.dataArray?[indexPath.item] as? ChangeRoomBGModel
        didSelectedBgImageClosure?(model?.imageName ?? "")
    }
}


class ChangeRoomBGCell: UICollectionViewCell {
    private lazy var imageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "BG01")
        imageView.contentMode = .scaleAspectFill
        imageView.cornerRadius = 5
        imageView.layer.borderColor = UIColor.blueColor.cgColor
        imageView.layer.borderWidth = 0
        return imageView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setImageName(_ name: String) {
        imageView.image = UIImage(named: name)
    }
    
    private func setupUI() {
        contentView.addSubview(imageView)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor).isActive = true
        imageView.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        imageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor).isActive = true
        imageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
    
    override var isSelected: Bool {
        didSet {
            imageView.layer.borderWidth = isSelected ? 1 : 0
        }
    }
}
