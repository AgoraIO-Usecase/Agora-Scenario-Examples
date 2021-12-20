//
//  VideoView.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/10/29.
//

import UIKit

@objc
public protocol AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell
    
    @objc optional func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath)
    @objc optional func pullToRefreshHandler()
}

public class AGECollectionView: UIView {
    //MARK: Public
    public var itemSize: CGSize = .zero {
        didSet {
            flowLayout.itemSize = itemSize
        }
    }
    public var estimatedItemSize: CGSize = .zero {
        didSet {
            flowLayout.estimatedItemSize = estimatedItemSize
        }
    }
    public var edge: UIEdgeInsets = .zero
    public var minLineSpacing: CGFloat = 0
    public var minInteritemSpacing: CGFloat = 0
    public var scrollDirection: UICollectionView.ScrollDirection = .horizontal
    public var showsHorizontalScrollIndicator: Bool = false {
        didSet {
            collectionView.showsHorizontalScrollIndicator = showsHorizontalScrollIndicator
        }
    }
    public var showsVerticalScrollIndicator: Bool = false {
        didSet {
            collectionView.showsVerticalScrollIndicator = showsVerticalScrollIndicator
        }
    }
    public var isPagingEnabled: Bool = false {
        didSet {
            collectionView.isPagingEnabled = isPagingEnabled
        }
    }
    public var isScrollEnabled: Bool = true {
        didSet {
            collectionView.isScrollEnabled = isScrollEnabled
        }
    }
    weak open var delegate: AGECollectionViewDelegate?
    public var dataArray: [Any]? {
        didSet {
            flowLayout.scrollDirection = scrollDirection
            emptyView.isHidden = !(dataArray?.isEmpty ?? true)
            collectionView.reloadData()
        }
    }
    public var visibleCells: [UICollectionViewCell] {
        collectionView.visibleCells
    }
    public var emptyTitle: String? {
        didSet {
            emptyView.setEmptyTitle(emptyTitle)
        }
    }
    public var emptyImage: UIImage? {
        didSet {
            emptyView.setEmptyImage(emptyImage)
        }
    }
    public var isRefreshing: Bool {
        refreshControl.isRefreshing
    }
    public func reloadData() {
        collectionView.reloadData()
    }
    public func register(_ cellClass: AnyClass?, forCellWithReuseIdentifier identifier: String) {
        collectionView.register(cellClass, forCellWithReuseIdentifier: identifier)
    }
    
    public func register(_ nib: UINib?, forCellWithReuseIdentifier identifier: String) {
        collectionView.register(nib, forCellWithReuseIdentifier: identifier)
    }
    public func addRefresh() {
        collectionView.refreshControl = refreshControl
    }
    public func beginRefreshing() {
        refreshControl.beginRefreshing()
    }
    public func endRefreshing() {
        refreshControl.endRefreshing()
    }
    
    private lazy var flowLayout: UICollectionViewFlowLayout = {
        let layout = UICollectionViewFlowLayout()
        layout.estimatedItemSize = .zero
        layout.footerReferenceSize = .zero
        layout.headerReferenceSize = .zero
        return layout
    }()
    private lazy var collectionView: UICollectionView = {
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: flowLayout)
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.backgroundColor = .clear
        collectionView.isPagingEnabled = true
        return collectionView
    }()
    private lazy var refreshControl: UIRefreshControl = {
        let refresh = UIRefreshControl()
        let attr = NSMutableAttributedString(string: "pull to refresh", attributes: [.foregroundColor: UIColor.black])
        refresh.attributedTitle = NSAttributedString(attributedString: attr)
        refresh.addTarget(self, action: #selector(pullRefreshHandler), for: .valueChanged)
        return refresh
    }()
    private lazy var emptyView: BaseEmptyView = {
        let view = BaseEmptyView()
        view.isHidden = true
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
        translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        emptyView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(collectionView)
        collectionView.addSubview(emptyView)
        collectionView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        emptyView.leadingAnchor.constraint(equalTo: collectionView.leadingAnchor).isActive = true
        emptyView.topAnchor.constraint(equalTo: collectionView.frameLayoutGuide.topAnchor).isActive = true
        emptyView.widthAnchor.constraint(equalTo: collectionView.widthAnchor).isActive = true
        emptyView.heightAnchor.constraint(equalTo: collectionView.heightAnchor).isActive = true
    }
    
    @objc
    private func pullRefreshHandler() {
        delegate?.pullToRefreshHandler?()
    }
}

extension AGECollectionView: UICollectionViewDataSource {
    public func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        dataArray?.count ?? 0
    }
    public func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = delegate?.collectionView(collectionView, cellForItemAt: indexPath) else { return UICollectionViewCell() }
        return cell
    }
}
extension AGECollectionView: UICollectionViewDelegate {
    public func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        delegate?.collectionView?(collectionView, didSelectItemAt: indexPath)
    }
}

extension AGECollectionView: UICollectionViewDelegateFlowLayout {
    public func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumLineSpacingForSectionAt section: Int) -> CGFloat {
        minLineSpacing
    }
    public func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumInteritemSpacingForSectionAt section: Int) -> CGFloat {
        minInteritemSpacing
    }
    public func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, insetForSectionAt section: Int) -> UIEdgeInsets {
        edge
    }
}


class BaseEmptyView: UIView {
    private lazy var imageView = UIImageView(image: UIImage(named: "pic-placeholding"))
    private lazy var descriptionLabel: UILabel = {
        let label = UILabel()
        label.text = "请创建一个房间"
        label.textColor = .gray
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        imageView.translatesAutoresizingMaskIntoConstraints = false
        descriptionLabel.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(imageView)
        addSubview(descriptionLabel)
        imageView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        imageView.bottomAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        descriptionLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        descriptionLabel.topAnchor.constraint(equalTo: imageView.bottomAnchor, constant: 15).isActive = true
    }
    
    func setEmptyImage(_ image: UIImage?) {
        imageView.image = image
    }
    func setEmptyTitle(_ title: String?) {
        descriptionLabel.text = title
    }
}
