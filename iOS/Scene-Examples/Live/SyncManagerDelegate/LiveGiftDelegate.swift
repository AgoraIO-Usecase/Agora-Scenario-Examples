//
//  LiveGiftDelegate.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/12.
//

import UIKit

enum PKLiveType {
    /// 自己
    case me
    /// 对方
    case target
}
/// 收到礼物回调
var LiveReceivedGiftClosure: ((LiveGiftModel, PKLiveType) -> Void)?
class LiveGiftDelegate: ISyncManagerEventDelegate {
    private var view: LiveBaseView
    private var type: PKLiveType
    init(view: LiveBaseView, type: PKLiveType) {
        self.view = view
        self.type = type
    }
    func onCreated(object: IObject) {
        LogUtils.log(message: "onCreated gift == \(String(describing: object.toJson()))", level: .info)
//        guard let model = JSONObject.toModel(LiveGiftModel.self, value: object.toJson()) else { return }
//        self.vc.playGifView.isHidden = false
//        self.vc.playGifView.loadGIFName(gifName: model.gifName)
//        LiveReceivedGiftClosure?(model, type)
//        self.vc.chatView.sendMessage(message: model.userId + "送出了一个" + model.title)
    }
    
    func onUpdated(object: IObject) {
        LogUtils.log(message: "onUpdated gift == \(String(describing: object.toJson()))", level: .info)
        guard let model = JSONObject.toModel(LiveGiftModel.self, value: object.toJson()) else { return }
        if type == .me {
            view.playGifView.isHidden = false
            view.playGifView.loadGIFName(gifName: model.gifName)
            let model = ChatMessageModel(message: model.userId + "送出了一个" + model.title, messageType: .message)
            view.chatView.sendMessage(messageModel: model)
        }
        LiveReceivedGiftClosure?(model, type)
    }
    
    func onDeleted(object: IObject?) {
        LogUtils.log(message: "onDeleted gift == \(String(describing: object?.toJson()))", level: .info)
    }
    
    func onSubscribed() {
        LogUtils.log(message: "onSubscribed gift", level: .info)
    }
    
    func onError(code: Int, msg: String) {
        LogUtils.log(message: "onError gift code ==\(code) msg == \(msg)", level: .error)
    }
}
