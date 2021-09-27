//
//  MainView.swift
//  LivePK
//
//  Created by ZYP on 2021/9/23.
//

import UIKit

class MainView: UIView {
    let collectionView = UICollectionView(frame: .zero, collectionViewLayout: VideoLayout())
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setup() {
        collectionView.backgroundColor = .white
        backgroundColor = .white
        addSubview(collectionView)
        collectionView.showsHorizontalScrollIndicator = false
    }
    
    func commonInit() {
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        collectionView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}

class VideoLayout: UICollectionViewFlowLayout {
    
    var attrs = [UICollectionViewLayoutAttributes]()
    
    override init() {
        super.init()
        sectionInset = .zero
        minimumLineSpacing = 0
        minimumInteritemSpacing = 0
        scrollDirection = .horizontal
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func prepare() {
        super.prepare()
        let count = collectionView?.numberOfItems(inSection: 0) ?? 1
        for i in 0..<count {
            let indexPath = IndexPath(row: i, section: 0)
            if let attr = layoutAttributesForItem(at: indexPath) {
                attrs.append(attr)
            }
        }
    }
    
    override func layoutAttributesForItem(at indexPath: IndexPath) -> UICollectionViewLayoutAttributes? {
        let attribute = super.layoutAttributesForItem(at: indexPath)
        let width = UIScreen.main.bounds.size.width/2 - 0.5
        attribute?.frame = .init(x: CGFloat(indexPath.row) * width + (indexPath.row > 0 ? 1 : 0),
                                 y: 0,
                                 width: width,
                                 height: width * 16/9 )
        return attribute
    }
    
    override func layoutAttributesForElements(in rect: CGRect) -> [UICollectionViewLayoutAttributes]? {
        let count = collectionView?.numberOfItems(inSection: 0) ?? 1
        attrs.removeAll()
        for i in 0..<count {
            let indexPath = IndexPath(row: i, section: 0)
            if let attr = layoutAttributesForItem(at: indexPath) {
                attrs.append(attr)
            }
        }
        return attrs
    }
}
