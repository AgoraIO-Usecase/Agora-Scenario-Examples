//
//  HandsupListDialog.swift
//  BlindDate
//
//  Created by XC on 2021/4/22.
//

import Foundation
import UIKit
import RxSwift
import Core

protocol RoomDelegate: DialogDelegate {
    var viewModel: RoomViewModel { get set }
}

class HandsupListDialog: Dialog {
    weak var delegate: RoomDelegate!
    var title: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 18)
        view.textAlignment = .center
        view.textColor = UIColor(hex: Colors.Black)
        view.text = "Application List".localized
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
        let id = NSStringFromClass(Action.self)
        listView.register(HandsupCellView.self, forCellReuseIdentifier: id)
        listView.dataSource = self
        listView.rowHeight = 64
        listView.separatorStyle = .none
        
        self.delegate.viewModel.onHandsupListChange
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] list in
                self.listView.reloadData()
            })
            .disposed(by: disposeBag)
        self.show(controller: delegate, padding: 6)
    }
}

extension HandsupListDialog: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.delegate.viewModel.handsupList.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let identifier = NSStringFromClass(Action.self)
        let cell = tableView.dequeueReusableCell(withIdentifier: identifier, for: indexPath) as! HandsupCellView
        cell.item = self.delegate.viewModel.handsupList[indexPath.row]
        cell.delegate = self
        return cell
    }
}

extension HandsupListDialog: HandsupListDelegate {
    func reject(action: Action) -> Observable<Result<Void>> {
        return self.delegate.viewModel.process(action: action, agree: false)
    }
    
    func agree(action: Action) -> Observable<Result<Void>> {
        return self.delegate.viewModel.process(action: action, agree: true)
    }
}

protocol HandsupListDelegate: class {
    func reject(action: Action) -> Observable<Result<Void>>
    func agree(action: Action) -> Observable<Result<Void>>
}

class HandsupCellView: UITableViewCell {
    weak var delegate: HandsupListDelegate!
    private let disposeBag = DisposeBag()
    var item: Action! {
        didSet {
            name.text = item.member.user.name
            avatar.image = UIImage(named: item.member.user.getLocalAvatar(), in: Bundle(identifier: "io.agora.InteractivePodcast")!, with: nil)
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        render()
        agreeButton.rx.tap
            .flatMap { [unowned self] _ in
                return self.delegate.agree(action: self.item)
            }
            .subscribe()
            .disposed(by: disposeBag)
        
        rejectButton.rx.tap
            .flatMap { [unowned self] _ in
                return self.delegate.reject(action: self.item)
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
    
    var rejectButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.Purple
        view.setTitle("Decline".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Purple), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 12)
        return view
    }()
    
    var agreeButton: UIButton = {
        let view = RoundButton()
        view.borderColor = Colors.Purple
        view.setTitle("Agree".localized, for: .normal)
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
        contentView.addSubview(rejectButton)
        contentView.addSubview(agreeButton)
        
        agreeButton.width(constant: 54)
            .height(constant: 24)
            .marginTrailing(anchor: contentView.trailingAnchor)
            .centerY(anchor: contentView.centerYAnchor)
            .active()
        rejectButton.width(constant: 54)
            .height(constant: 24)
            .marginTrailing(anchor: agreeButton.leadingAnchor, constant: 10)
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
