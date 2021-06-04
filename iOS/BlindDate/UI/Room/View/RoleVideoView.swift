//
//  RoleVideoView.swift
//  BlindDate
//
//  Created by XC on 2021/4/23.
//

import Foundation
import RxSwift
import RxRelay
import RxCocoa
import Core

class RoleVideoView {
    let disposeBag = DisposeBag()
    weak var delegate: RoomControlDelegate?
    var member: BlindDateMember? {
        didSet {
            if let member = member {
                addView?.isHidden = true
                nameView.isHidden = false
                nameView.text = member.user.name
                micView.isHidden = false
                videoView.isHidden = false
                let icon = member.isMuted || member.isSelfMuted ? "status_mic_close" : "status_mic_open"
                micView.image = UIImage(named: icon, in: Utils.bundle, compatibleWith: nil)
                
                if let old = oldValue {
                    if (old.isLocal != member.isLocal || old.streamId != member.streamId) {
                        unbindVideo(member: old)
                        bindVideo(member: member)
                    }
                } else {
                    bindVideo(member: member)
                }
            } else {
                addView?.isHidden = false
                nameView.isHidden = true
                micView.isHidden = true
                videoView.isHidden = true
                if let old = oldValue {
                    unbindVideo(member: old)
                }
            }
        }
    }
    weak var videoView: UIView!
    weak var nameView: UILabel!
    weak var micView: UIImageView!
    weak var addView: UIButton?
    
    func subcribeUIEvent() {
        if let addView = addView {
            addView.rx.tap
                .subscribe(onNext: { [unowned self] _ in
                    self.delegate?.onTap(view: self)
                })
                .disposed(by: disposeBag)
        }
        
        videoView.onTap().rx.event
            .subscribe(onNext: { [unowned self] _ in
                self.delegate?.onTap(view: self)
            })
            .disposed(by: disposeBag)
    }
    
    private func bindVideo(member: BlindDateMember) {
        if (member.isLocal) {
            RoomManager.shared().bindLocalVideo(view: videoView)
        } else {
            RoomManager.shared().bindRemoteVideo(view: videoView, uid: member.streamId)
        }
    }
    
    private func unbindVideo(member: BlindDateMember) {
        if (member.isLocal) {
            RoomManager.shared().bindLocalVideo(view: nil)
        } else {
            RoomManager.shared().bindRemoteVideo(view: nil, uid: member.streamId)
        }
    }
    
    deinit {
        Logger.log(message: "RoleVideoView deinit", level: .info)
    }
}
