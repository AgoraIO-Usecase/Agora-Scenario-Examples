//
//  LiveShoppingListView.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2022/8/31.
//

import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

enum LiveShoppingStatus: Int, Codable {
    case list = 0
    case goodsShelves = 1
    
    var title: String {
        switch self {
        case .list: return "上架"
        case .goodsShelves: return "下架"
        }
    }
}

class LiveShoppingListModel: Codable {
    var imageName: String = ""
    var title: String = ""
    var price: Double = 0.0
    var desc: String = ""
    var status: LiveShoppingStatus = .list
    var objectId: String?
    
    static func createData() -> [LiveShoppingListModel] {
        var tempArray = [LiveShoppingListModel]()
        var model = LiveShoppingListModel()
        model.imageName = "pic-1"
        model.title = "口红"
        model.price = 7000.0
        model.desc = "Lipstick_Description".localized
        tempArray.append(model)

        model = LiveShoppingListModel()
        model.imageName = "pic-2"
        model.title = "背包"
        model.price = 3399
        model.desc = "Rucksack_Description".localized
        tempArray.append(model)
        
        model = LiveShoppingListModel()
        model.imageName = "pic-3"
        model.title = "iPad"
        model.price = 4988.0
        model.desc = "iPad_Description".localized
        tempArray.append(model)
        
        model = LiveShoppingListModel()
        model.imageName = "pic-4"
        model.title = "裤子"
        model.price = 998.0
        model.desc = "Pants_Description".localized
        tempArray.append(model)
        
        return tempArray
    }
}

class LiveShoppingListView: UIView {
    var onTapLookGoodsClosure: ((LiveShoppingListModel) -> Void)?
    
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .middle)
        label.text = "商品列表"
        return label
    }()
    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray
        return view
    }()
    private lazy var segmentView: SegmentView = {
        
        let segmentView = SegmentView(frame: CGRect(x: (Screen.width - 200) * 0.5,
                                                    y: 44,
                                                    width: 200,
                                                    height: 44),
                                      segmentStyle: .init(),
                                      titles: ["待上架", "已上架"])
        segmentView.style.indicatorStyle = .line
        segmentView.style.indicatorHeight = 2
        segmentView.style.indicatorColor = .blueColor
        segmentView.style.indicatorWidth = 60
        segmentView.selectedTitleColor = .black
        segmentView.normalTitleColor = .gray
        segmentView.valueChange = { [weak self] index in
            guard let self = self, self.segmentView.titles.count > index else { return }
            if index == 0 {
                self.collectionView.dataArray = self.dataArray.filter({ $0.status == .list })
            } else {
                self.collectionView.dataArray = self.dataArray.filter({ $0.status == .goodsShelves })
            }
        }
        return segmentView
    }()
    private lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        view.itemSize = CGSize(width: Screen.width, height: 120)
        view.minInteritemSpacing = 0
        view.minLineSpacing = 10
        view.delegate = self
        view.scrollDirection = .vertical
        view.emptyTitle = "暂时没有商品"
        view.register(LiveShoppingListViewCell.self,
                      forCellWithReuseIdentifier: LiveShoppingListViewCell.description())
        return view
    }()
    private var dataArray: [LiveShoppingListModel] = [] {
        didSet {
            collectionView.dataArray = dataArray
        }
    }
    private var role: AgoraClientRole = .broadcaster
    private var channelName: String = ""
    
    init(role: AgoraClientRole, channelName: String) {
        super.init(frame: .zero)
        self.role = role
        self.channelName = channelName
        setupUI()
        if role == .broadcaster {
            LiveShoppingListModel.createData().forEach({
                dataArray.append($0)
            })
        } else {
            getGoodsList()
            subscribeHandler()
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        if role == .broadcaster {
            segmentView.setSelectIndex(index: 0)
            collectionView.dataArray = dataArray.filter({ $0.status == .list })
        } else {
            collectionView.dataArray = dataArray.filter({ $0.status == .goodsShelves })
        }
    }
    
    private func getGoodsList() {
        SyncUtil.scene(id: channelName)?.collection(className: SYNC_SCENE_SHOPPING_INFO).get(success: { objects in
            let models = objects.compactMap({
                JSONObject.toModel(LiveShoppingListModel.self,
                                   value: $0.toJson())
            })
            if self.role == .broadcaster {
                self.dataArray = models
            } else {
                self.dataArray = models.filter({ $0.status == .goodsShelves })
            }
        }, fail: nil)
    }
    
    private func subscribeHandler() {
        SyncUtil.scene(id: channelName)?.subscribe(key: SYNC_SCENE_SHOPPING_INFO, onCreated: nil, onUpdated: { object in
            guard let model = JSONObject.toModel(LiveShoppingListModel.self,
                                                 value: object.toJson()) else { return }
            self.getGoodsList()
            ToastView.show(text: "\(model.title) 上架", postion: .bottom)
        }, onDeleted: { object in
            if let index = self.dataArray.firstIndex(where: { object.getId() == $0.objectId }) {
                self.dataArray.remove(at: index)
            }
        }, onSubscribed: nil, fail: nil)
    }
    
    private func setupUI() {
        backgroundColor = .white
        translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        addSubview(lineView)
        addSubview(segmentView)
        addSubview(collectionView)
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        
        segmentView.isHidden = role == .audience
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        heightAnchor.constraint(equalToConstant: Screen.height - 150.fit).isActive = true
        
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 15).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10).isActive = true
        lineView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 15).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: lineView.bottomAnchor, constant: role == .broadcaster ? 44 : 0).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
}
extension LiveShoppingListView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: LiveShoppingListViewCell.description(),
                                                      for: indexPath) as! LiveShoppingListViewCell
        if let model = self.collectionView.dataArray?[indexPath.item] as? LiveShoppingListModel {
            cell.setupModel(model: model, channelName: channelName, role: role)
        }
        cell.onTapLookGoodsClosure = onTapLookGoodsClosure
        return cell
    }
}


class LiveShoppingListViewCell: UICollectionViewCell {
    var onTapLookGoodsClosure: ((LiveShoppingListModel) -> Void)?
    private lazy var imageView: AGEImageView = {
        let imageView = AGEImageView(imageName: "Shopping/pic-1")
        return imageView
    }()
    private lazy var titleLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .middle)
        label.text = "背包"
        return label
    }()
    private lazy var priceLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .middle)
        label.text = "200.0"
        return label
    }()
    private lazy var button: AGEButton = {
        let button = AGEButton(style: .outline(borderColor: .gray),
                               colorStyle: .primary,
                               fontStyle: .middle)
        button.setTitle("上架", for: .normal)
        button.cornerRadius = 20
        button.addTarget(self, action: #selector(onTapButtonHandler), for: .touchUpInside)
        return button
    }()
    private var currentModel: LiveShoppingListModel?
    private var channelName: String = ""
    private var role: AgoraClientRole = .broadcaster
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupModel(model: LiveShoppingListModel, channelName: String, role: AgoraClientRole) {
        currentModel = model
        self.role = role
        self.channelName = channelName
        imageView.image = UIImage(named: "Shopping/\(model.imageName)")
        titleLabel.text = model.title
        priceLabel.text = "\(model.price)"
        let title = role == .broadcaster ? model.status.title : "去看看"
        button.setTitle(title, for: .normal)
    }
    
    private func setupUI() {
        imageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        priceLabel.translatesAutoresizingMaskIntoConstraints = false
        button.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(imageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(priceLabel)
        contentView.addSubview(button)
        
        imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15).isActive = true
        imageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10).isActive = true
        imageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: imageView.trailingAnchor, constant: 15).isActive = true
        titleLabel.topAnchor.constraint(equalTo: imageView.topAnchor, constant: 15).isActive = true
        
        priceLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        priceLabel.bottomAnchor.constraint(equalTo: imageView.bottomAnchor, constant: -15).isActive = true
        
        button.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15).isActive = true
        button.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
        button.widthAnchor.constraint(equalToConstant: 80).isActive = true
        button.heightAnchor.constraint(equalToConstant: 40).isActive = true
    }
    
    @objc
    private func onTapButtonHandler() {
        guard let model = currentModel else {
            return
        }
        if role == .audience {
            onTapLookGoodsClosure?(model)
            AlertManager.hiddenView()
            return
        }
        AlertManager.hiddenView(all: true) {
            let title = model.status == .list ? "你是否要上架\"\(model.title)\"" : "你是否要下架\"\(model.title)\""
            let alertVC = UIAlertController(title: title,
                                            message: nil,
                                            preferredStyle: .alert)
            let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
            let confirm = UIAlertAction(title: "Confirm".localized, style: .default) { _ in
                if model.status == .list {
                    model.status = .goodsShelves
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_SCENE_SHOPPING_INFO).add(data: params, success: { object in
                        model.objectId = object.getId()
                    }, fail: nil)
                } else {
                    model.status = .list
                    let params = JSONObject.toJson(model)
                    SyncUtil.scene(id: self.channelName)?.collection(className: SYNC_SCENE_SHOPPING_INFO).update(id: model.objectId ?? "", data: params, success: nil, fail: nil)
                }
            }
            alertVC.addAction(cancel)
            alertVC.addAction(confirm)
            UIAlertController.cl_topViewController()?.present(alertVC,
                                                              animated: true,
                                                              completion: nil)
        }
    }
}
