//
//  BaseTableViewLayout.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import UIKit

@objc
protocol BaseTableViewLayoutDelegate {
    @objc optional func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath)
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell

    @objc optional func pullToRefreshHandler()
}

class BaseTableViewLayout: UIView {
    //MARK: Public
    var rowHeight: CGFloat = 0 {
        didSet {
            tableView.rowHeight = rowHeight
        }
    }
    var estimatedRowHeight: CGFloat = 0 {
        didSet {
            tableView.estimatedRowHeight = estimatedRowHeight
        }
    }
    var dataArray: [Any]? {
        didSet {
            emptyView.isHidden = !(dataArray?.isEmpty ?? true)
            tableView.reloadData()
        }
    }
    var separatorStyle: UITableViewCell.SeparatorStyle = .none {
        didSet {
            tableView.separatorStyle = separatorStyle
        }
    }
    var headerView: UIView? {
        didSet {
            tableView.tableHeaderView = headerView
        }
    }
    var emptyTitle: String? {
        didSet {
            emptyView.setEmptyTitle(emptyTitle)
        }
    }
    var emptyImage: UIImage? {
        didSet {
            emptyView.setEmptyImage(emptyImage)
        }
    }
    var contentInset: UIEdgeInsets = .zero {
        didSet {
            tableView.contentInset = contentInset
        }
    }
    var showsVerticalScrollIndicator: Bool = false {
        didSet {
            tableView.showsVerticalScrollIndicator = showsVerticalScrollIndicator
        }
    }
    weak open var delegate: BaseTableViewLayoutDelegate?
    var isRefreshing: Bool {
        refreshControl.isRefreshing
    }
    func reloadData() {
        tableView.reloadData()
    }
    func register(_ cellClass: AnyClass?, forCellWithReuseIdentifier identifier: String) {
        tableView.register(cellClass, forCellReuseIdentifier: identifier)
    }
    
    func register(_ nib: UINib?, forCellWithReuseIdentifier identifier: String) {
        tableView.register(nib, forCellReuseIdentifier: identifier)
    }
    func insertBottomRow(item: Any?) {
        guard let datas = dataArray,
        let item = item else { return }
        tableView.beginUpdates()
        let indexPath = IndexPath(row: datas.count, section: 0)
        dataArray?.append(item)
        tableView.insertRows(at: [indexPath], with: .fade)
        tableView.endUpdates()
        tableView.scrollToRow(at: indexPath, at: .bottom, animated: true)
    }
    func addRefresh() {
        tableView.refreshControl = refreshControl
    }
    func beginRefreshing() {
        refreshControl.beginRefreshing()
    }
    func endRefreshing() {
        refreshControl.endRefreshing()
    }
    
    private lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.backgroundColor = .clear
        tableView.estimatedSectionFooterHeight = 0
        tableView.estimatedSectionHeaderHeight = 0
        tableView.separatorStyle = .none
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        return tableView
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
        backgroundColor = .clear
        tableView.translatesAutoresizingMaskIntoConstraints = false
        emptyView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(tableView)
        tableView.addSubview(emptyView)
        tableView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        tableView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        emptyView.leadingAnchor.constraint(equalTo: tableView.leadingAnchor).isActive = true
        emptyView.topAnchor.constraint(equalTo: tableView.frameLayoutGuide.topAnchor).isActive = true
        emptyView.widthAnchor.constraint(equalTo: tableView.widthAnchor).isActive = true
        emptyView.heightAnchor.constraint(equalTo: tableView.heightAnchor).isActive = true
    }
    
    @objc
    func pullRefreshHandler() {
        delegate?.pullToRefreshHandler?()
    }
}

extension BaseTableViewLayout: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        dataArray?.count ?? 0
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = delegate?.tableView(tableView, cellForRowAt: indexPath)
        cell?.selectionStyle = .none
        return cell ?? UITableViewCell()
    }
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        delegate?.tableView?(tableView, didSelectRowAt: indexPath)
    }
}
