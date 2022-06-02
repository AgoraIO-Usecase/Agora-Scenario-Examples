//
//  EntryView.swift
//  SuperAppCore
//
//  Created by ZYP on 2021/11/12.
//

import UIKit

protocol SuperAppRoomListViewDelegate: NSObjectProtocol {
    func entryViewDidTapCreateButton(_ view: SuperAppRoomListView)
    func entryView(_ view: SuperAppRoomListView,
                   didSelected info: SuperAppRoomListView.Info,
                   at index: Int)
    func entryViewdidPull(_ view: SuperAppRoomListView)
}

class SuperAppRoomListView: UIView {
    typealias Info = SuperAppRoomListViewCell.Info
    let createButton = UIButton()
    let centerButton = UIButton()
    let tipsLabel = UILabel()
    let collectionView: UICollectionView
    let refreshControl = UIRefreshControl()
    weak var delegate: SuperAppRoomListViewDelegate?
    var infos = [Info]()
    
    override init(frame: CGRect) {
        let flowLayout = UICollectionViewFlowLayout()
        let width = (UIScreen.main.bounds.size.width - 3 * 10)/2
        flowLayout.itemSize = .init(width: width, height: width)
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: flowLayout)
        super.init(frame: frame)
        setup()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setup() {
        backgroundColor = .white
        
        collectionView.refreshControl = refreshControl
        collectionView.contentInset = UIEdgeInsets(top: 10, left: 10, bottom: 10, right: 10)
        collectionView.backgroundColor = .white
        tipsLabel.textColor = .gray
        createButton.setImage(UIImage(named: "pic-create"), for: .normal)
        centerButton.setImage(UIImage(named: "pic-placeholding"), for: .normal)
        tipsLabel.text = "请创建一个房间"
        
        addSubview(collectionView)
        addSubview(centerButton)
        addSubview(tipsLabel)
        addSubview(createButton)
        
        centerButton.translatesAutoresizingMaskIntoConstraints = false
        tipsLabel.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        createButton.translatesAutoresizingMaskIntoConstraints = false
        
        centerButton.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        centerButton.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        tipsLabel.topAnchor.constraint(equalTo: centerButton.bottomAnchor).isActive = true
        tipsLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        
        collectionView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        
        createButton.rightAnchor.constraint(equalTo: rightAnchor, constant: -10).isActive = true
        createButton.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -20).isActive = true
    }
    
    private func commonInit() {
        collectionView.register(SuperAppRoomListViewCell.self, forCellWithReuseIdentifier: "EntryViewCell")
        collectionView.dataSource = self
        collectionView.delegate = self
        createButton.addTarget(self,
                               action: #selector(buttonTap(_:)),
                               for: .touchUpInside)
        centerButton.addTarget(self,
                               action: #selector(buttonTap(_:)),
                               for: .touchUpInside)
        refreshControl.addTarget(self,
                                 action: #selector(refreshPull),
                                 for: .valueChanged)
    }
    
    @objc func buttonTap(_ button: UIButton) {
        delegate?.entryViewDidTapCreateButton(self)
    }
    
    @objc func refreshPull() {
        delegate?.entryViewdidPull(self)
    }
    
    func update(infos: [Info]) {
        let shouldHidenCenterButton = infos.count > 0
        centerButton.isHidden = shouldHidenCenterButton
        tipsLabel.isHidden = shouldHidenCenterButton
        self.infos = infos
        collectionView.reloadData()
    }
    
    func endRefreshing() {
        refreshControl.endRefreshing()
    }
}

// MARK: - UICollectionViewDataSource & UICollectionViewDelegate
extension SuperAppRoomListView: UICollectionViewDataSource, UICollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return infos.count
    }
    
    func collectionView(_ collectionView: UICollectionView,
                        cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "EntryViewCell", for: indexPath) as! SuperAppRoomListViewCell
        let info = infos[indexPath.row]
        cell.setInfo(info)
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView,
                        didSelectItemAt indexPath: IndexPath) {
        collectionView.deselectItem(at: indexPath, animated: true)
        let info = infos[indexPath.row]
        delegate?.entryView(self,
                            didSelected: info,
                            at: indexPath.row)
    }
}
