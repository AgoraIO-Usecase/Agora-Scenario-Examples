//
//  HomeCardView.swift
//  BlindDate
//
//  Created by XC on 2021/4/21.
//

import UIKit
import RxSwift
import RxCocoa
import Core

protocol HomeCardDelegate: class {
    func onTapCard(with room: Room)
}

final class HomeCardView: UICollectionViewCell {
    static var ICON_WIDTH: CGFloat = 30
    fileprivate static let padding: CGFloat = 6
    fileprivate static let lineSpacing: CGFloat = 5
    fileprivate static let font = UIFont.systemFont(ofSize: 16)
    fileprivate static let lineHeight: CGFloat = 26.5
    
    //fileprivate let onRoomChanged: PublishRelay<Room> = PublishRelay()
    weak var delegate: HomeCardDelegate?
    let disposeBag = DisposeBag()
    private let textStyle: [NSAttributedString.Key : Any] = {
        let style = NSMutableParagraphStyle()
        style.lineSpacing = HomeCardView.lineSpacing
        let shadow = NSShadow()
        shadow.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.5)
        shadow.shadowBlurRadius = 2
        shadow.shadowOffset = CGSize(width: 0, height: 1)
        let attributes = [
            NSAttributedString.Key.font: HomeCardView.font,
            NSAttributedString.Key.paragraphStyle: style,
            NSAttributedString.Key.shadow: shadow
        ]
        return attributes
    }()
    
    var room: Room! {
        didSet {
            //title.text = room.channelName
            title.attributedText = NSAttributedString(string: room.channelName, attributes: textStyle)
            avatar.name.attributedText = NSAttributedString(string: room.anchor.name, attributes: textStyle)
            avatar.avatar.image = UIImage(named: room.anchor.getLocalAvatar(), in: Bundle.init(identifier: "io.agora.InteractivePodcast")!, with: nil)
            cover.image = UIImage(named: room.anchor.getLocalAvatar(), in: Bundle.init(identifier: "io.agora.InteractivePodcast")!, with: nil)
        }
    }
    
    var cover: UIImageView = {
        let view = RoundImageView()
        view.radius = 6
        view.color = nil
        view.borderWidth = 0
        return view
    }()
    
    var avatar: AvatarView = {
        let view = AvatarView()
        return view
    }()
    
    var title: UILabel = {
        let view = UILabel()
        view.textColor = UIColor(hex: Colors.White)
        view.numberOfLines = 2
        return view
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor.lightGray
        
        addSubview(cover)
        addSubview(title)
        addSubview(avatar)
        
        cover.fill(view: self)
            .active()
        
        title.marginTop(anchor: topAnchor, constant: HomeCardView.padding)
            .marginLeading(anchor: leadingAnchor, constant: HomeCardView.padding)
            .centerX(anchor: centerXAnchor)
            .active()
        
        avatar.height(constant: HomeCardView.ICON_WIDTH)
            .marginBottom(anchor: bottomAnchor, constant: 6)
            .marginLeading(anchor: leadingAnchor, constant: HomeCardView.padding)
            .marginTrailing(anchor: trailingAnchor, constant: HomeCardView.padding)
            .active()
        
        onTap().rx.event
            .throttle(RxTimeInterval.seconds(2), scheduler: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] _ in
                self.delegate?.onTapCard(with: room)
            })
            .disposed(by: disposeBag)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        rounded(radius: 6)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.highlight()
    }
    
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.unhighlight()
    }
    
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.unhighlight()
    }
    
    class AvatarView: UIView {
        var avatar: UIImageView = {
            let view = RoundImageView()
            view.image = UIImage(named: "default", in: Bundle.init(identifier: "io.agora.InteractivePodcast")!, with: nil)
            return view
        }()
        
        var name: UILabel = {
            let view = UILabel()
            view.font = UIFont.systemFont(ofSize: 14)
            view.numberOfLines = 1
            view.textColor = UIColor(hex: Colors.White)
            view.shadow(offset: CGSize(width: 0, height: 1))
            return view
        }()
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            backgroundColor = .clear
            addSubview(avatar)
            addSubview(name)
            avatar
                .width(constant: HomeCardView.ICON_WIDTH)
                .height(constant: HomeCardView.ICON_WIDTH)
                .marginLeading(anchor: leadingAnchor)
                .centerY(anchor: centerYAnchor)
                .active()
            name.marginLeading(anchor: avatar.trailingAnchor, constant: 10)
                .marginTrailing(anchor: trailingAnchor)
                .centerY(anchor: centerYAnchor, constant: 5)
                .active()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
    }

    static func sizeForItem(room: Room, width: CGFloat) -> CGSize {
        return CGSize(width: width, height: width)
    }
}

