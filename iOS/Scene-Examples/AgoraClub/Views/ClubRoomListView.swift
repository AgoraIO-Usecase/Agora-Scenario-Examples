//
//  ClubRoomListView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/4/15.
//

import UIKit
import AgoraUIKit_iOS

class ClubRoomListView: UIView {
    var didClubItemClosure: ((LiveRoomInfo) -> Void)?
    
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel()
        label.text = "房间列表"
        label.textColor = .white
        label.fontStyle = .large
        return label
    }()
    private lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.delegate = self
        view.edge = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        view.minLineSpacing = 15
        view.minInteritemSpacing = 0
        let viewW = Screen.width
        let w = (viewW - view.minLineSpacing - view.edge.left - view.edge.right) / 2.0
        view.itemSize = CGSize(width: w, height: w)
        view.scrollDirection = .vertical
        view.isPagingEnabled = false
        view.addRefresh()
        view.register(LiveRoomListCell.self,
                      forCellWithReuseIdentifier: LiveRoomListCell.description())
        return view
    }()
    private var dataArray = [LiveRoomInfo]()
    
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        getLiveData()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func getLiveData() {
        SyncUtil.fetchAll { results in
            self.collectionView.endRefreshing()
            self.dataArray = results.compactMap({ $0.toJson() }).compactMap({ JSONObject.toModel(LiveRoomInfo.self, value: $0 )})
            self.collectionView.dataArray = self.dataArray
        } fail: { error in
            self.collectionView.endRefreshing()
            LogUtils.log(message: "get live data error == \(error.localizedDescription)", level: .info)
        }
    }
    
    private func setupUI() {
        backgroundColor = UIColor(hex: "#0E141D")
        collectionView.backgroundColor = backgroundColor
        addSubview(titleLabel)
        addSubview(collectionView)
        translatesAutoresizingMaskIntoConstraints = false
        widthAnchor.constraint(equalToConstant: Screen.height).isActive = true
        heightAnchor.constraint(equalToConstant: Screen.height * 0.7).isActive = true
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 20).isActive = true
        collectionView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}

extension ClubRoomListView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let item = self.collectionView.dataArray?[indexPath.item] as? LiveRoomInfo else { return }
        didClubItemClosure?(item)
    }
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveRoomListCell.description(),
                                                      for: indexPath) as! LiveRoomListCell
        cell.setRoomInfo(info: self.collectionView.dataArray?[indexPath.item])
        return cell
    }
    func pullToRefreshHandler() {
        getLiveData()
    }
}
