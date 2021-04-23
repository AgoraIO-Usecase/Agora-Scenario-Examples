//
//  RoomListenerList.swift
//  BlindDate
//
//  Created by XC on 2021/4/23.
//

import Foundation
import UIKit
import RxSwift
import Core

class RoomListenerList: Dialog {
    weak var delegate: RoomDelegate!
    var title: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 18)
        view.textAlignment = .center
        view.textColor = UIColor(hex: Colors.Black)
        view.text = "成员列表".localized
        return view
    }()
    
    var line: UIView = {
       let view = UIView()
        view.backgroundColor = UIColor(hex: Colors.LightGray)
        return view
    }()
    
    var listView: UITableView = {
        let view = UITableView()
        view.backgroundColor = .clear
        return view
    }()
    
    override func setup() {
        backgroundColor = UIColor(hex: Colors.White)
        addSubview(title)
        addSubview(line)
        addSubview(listView)
    }
    
    override func render() {
        roundCorners([.topLeft, .topRight], radius: 18)
        backgroundColor = .white
        shadow()
        title.marginLeading(anchor: leadingAnchor, constant: 20)
            .marginTrailing(anchor: trailingAnchor, constant: 20)
            .marginTop(anchor: topAnchor, constant: 10)
            .active()
        
        line.marginTop(anchor: title.bottomAnchor, constant: 10)
            .marginLeading(anchor: leadingAnchor, constant: 20)
            .marginTrailing(anchor: trailingAnchor, constant: 20)
            .height(constant: 1/* / UIScreen.main.scale*/)
            .active()
        
        listView.marginTop(anchor: line.bottomAnchor)
            .marginLeading(anchor: leadingAnchor, constant: 20)
            .marginTrailing(anchor: trailingAnchor, constant: 20)
            .height(constant: 320, relation: .greaterOrEqual)
            .marginBottom(anchor: bottomAnchor)
            .active()
    }
    
    func show(delegate: RoomDelegate) {
        self.delegate = delegate
        let id = NSStringFromClass(ListenerCellView.self)
        listView.register(ListenerCellView.self, forCellReuseIdentifier: id)
        listView.dataSource = self
        listView.rowHeight = 64
        listView.separatorStyle = .none
        
        self.delegate.viewModel.onListenersListChange
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] list in
                self.listView.reloadData()
            })
            .disposed(by: disposeBag)
        self.show(controller: delegate, padding: 6)
    }
}

extension RoomListenerList: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.delegate.viewModel.listeners.list.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let identifier = NSStringFromClass(ListenerCellView.self)
        let cell = tableView.dequeueReusableCell(withIdentifier: identifier, for: indexPath) as! ListenerCellView
        cell.isManager = self.delegate.viewModel.isManager
        cell.item = self.delegate.viewModel.listeners.list[indexPath.row]
        cell.delegate = self
        return cell
    }
}

extension RoomListenerList: ListenerListDelegate {
    func invate(member: Member) -> Observable<Result<Void>> {
        return self.delegate.viewModel.inviteSpeaker(member: member)
            .flatMap { result -> Observable<Result<Void>> in
                return result.onSuccess { () -> Observable<Result<Void>> in
                    self.delegate.dismiss(dialog: self).asObservable().map { _ in Result<Void>(success: true) }
                }
        }
    }
}

protocol ListenerListDelegate: class {
    func invate(member: Member) -> Observable<Result<Void>>
}

class ListenerCellView: UITableViewCell {
    weak var delegate: ListenerListDelegate!
    private let disposeBag = DisposeBag()
    var item: Member! {
        didSet {
            name.text = item.user.name
            avatar.image = UIImage(named: item.user.getLocalAvatar(), in: Bundle.init(identifier: "io.agora.InteractivePodcast")!, with: nil)
        }
    }
    var isManager: Bool = true {
        didSet {
            inviteButton.isHidden = !isManager
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        render()
        inviteButton.rx.tap
            .flatMap { [unowned self] _ in
                return self.delegate.invate(member: self.item)
            }
            .subscribe()
            .disposed(by: disposeBag)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    var avatar: UIImageView = {
        let view = RoundImageView()
        //view.image = UIImage(named: "default")
        return view
    }()
    
    var name: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 14)
        view.numberOfLines = 1
        view.textColor = UIColor.black
        return view
    }()
    
    var inviteButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.Purple
        view.setTitle("邀请".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.White), for: .normal)
        view.backgroundColor = UIColor(hex: Colors.Purple)
        view.titleLabel?.font = UIFont.systemFont(ofSize: 12)
        return view
    }()
    
    func render() {
        selectionStyle = .none
        backgroundColor = .clear
        contentView.addSubview(avatar)
        contentView.addSubview(name)
        contentView.addSubview(inviteButton)
        
        inviteButton.width(constant: 54)
            .height(constant: 24)
            .marginTrailing(anchor: contentView.trailingAnchor)
            .centerY(anchor: contentView.centerYAnchor)
            .active()
        avatar
            .width(constant: 42)
            .height(constant: 42)
            .marginLeading(anchor: contentView.leadingAnchor)
            .centerY(anchor: contentView.centerYAnchor)
            .active()
        name.marginLeading(anchor: avatar.trailingAnchor, constant: 10)
            .marginTrailing(anchor: contentView.trailingAnchor, constant: 10, relation: .equal)
            .centerY(anchor: contentView.centerYAnchor)
            .active()
    }
}

