//
//  RethinkSyncManager.swift
//  AgoraSyncManager
//
//  Created by zhaoyongqiang on 2022/7/5.
//

import SocketRocket

extension RethinkSyncManager {
    struct Config {
        let appId: String
        let channelName: String

        init(appId: String,
             channelName: String)
        {
            self.appId = appId
            self.channelName = channelName
        }
    }
}

enum SocketType: String {
    case getRoomList
    case send
    case subscribe
    case unsubscribe
    case query
    case deleteProp
    case ping
}

public class RethinkSyncManager: NSObject {
    private let SOCKET_URL: String = "wss://test-rethinkdb-msg.bj2.agoralab.co/v2"
//    private let SOCKET_URL: String = "wss://rethinkdb-msg.bj2.agoralab.co"
    private var timer: Timer?
    private var socket: SRWebSocket?
    private var connectBlock: SuccessBlockInt?
    var onSuccessBlock = [String: SuccessBlock]()
    var onSuccessBlockVoid = [String: SuccessBlockVoid]()
    var onDeleteBlockObjOptional = [String: SuccessBlockObjOptional?]()
    var onSuccessBlockObjOptional = [String: SuccessBlockObjOptional]()
    var onSuccessBlockObj = [String: SuccessBlockObj]()
    var onFailBlock = [String: FailBlock]()
    var onCreateBlocks = [String: OnSubscribeBlock]()
    var onUpdatedBlocks = [String: OnSubscribeBlock]()
    var onDeletedBlocks = [String: OnSubscribeBlock]()
    var appId: String = ""
    var channelName: String = ""
    var sceneName: String = ""

    /// init
    /// - Parameters:
    ///   - config: config
    ///   - complete: white `code = 0` is success, else error
    init(config: Config,
         complete: SuccessBlockInt?)
    {
        super.init()
        connectBlock = complete
        sceneName = config.channelName
        appId = config.appId
        reConnect(isRemove: true)
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(enterForegroundNotification),
                                               name: UIApplication.willEnterForegroundNotification,
                                               object: nil)
    }

    public func reConnect(isRemove: Bool = false) {
        guard let url = URL(string: SOCKET_URL) else { return }
        disConnect(isRemove: isRemove)
        socket = SRWebSocket(url: url)
        socket?.delegate = self
        socket?.open()
        timer = Timer.scheduledTimer(withTimeInterval: 10,
                                     repeats: true,
                                     block: { [weak self] _ in
            let params = ["action": SocketType.ping.rawValue,
                          "appId": self?.appId ?? "",
                          "channelName": self?.channelName ?? "",
                          "requestId": UUID().uuid16string()]
            let data = Utils.toJsonString(dict: params)?.data(using: .utf8)
            try? self?.socket?.send(dataNoCopy: data)
        })
        timer?.fire()
        RunLoop.main.add(timer!, forMode: .common)
        guard !onUpdatedBlocks.isEmpty else { return }
        // 重连后重新订阅
        onUpdatedBlocks.keys.forEach({
            subscribe(channelName: $0, roomId: "", objType: $0)
        })
    }

    public func disConnect(isRemove: Bool) {
        timer?.invalidate()
        timer = nil
        socket?.close()
        guard isRemove else { return }
        onSuccessBlock.removeAll()
        onSuccessBlockVoid.removeAll()
        onDeleteBlockObjOptional.removeAll()
        onSuccessBlockObjOptional.removeAll()
        onSuccessBlockObj.removeAll()
        onFailBlock.removeAll()
        onCreateBlocks.removeAll()
        onUpdatedBlocks.removeAll()
        onDeletedBlocks.removeAll()
    }

    public func write(channelName: String,
                      data: [String: Any?],
                      roomId: String,
                      objectId: String? = nil,
                      objType: String,
                      isAdd: Bool = false) {
        writeData(channelName: channelName,
                  params: data,
                  roomId: roomId,
                  objectId: objectId,
                  type: .send,
                  objType: objType,
                  isAdd: isAdd)
    }

    public func subscribe(channelName: String,
                          roomId: String,
                          objType: String) {
        writeData(channelName: channelName,
                  params: [:],
                  roomId: roomId,
                  type: .subscribe,
                  objType: objType)
    }

    public func unsubscribe(channelName: String, roomId: String, objType: String) {
        writeData(channelName: channelName,
                  params: [:],
                  roomId: roomId,
                  type: .unsubscribe,
                  objType: objType)
    }

    private func writeData(channelName: String,
                           params: Any,
                           roomId: String,
                           objectId: String? = nil,
                           type: SocketType,
                           objType: String,
                           isAdd: Bool = false)
    {
        var newParams = params
        var propsId: String = objectId ?? UUID().uuid16string()
        if objectId == nil && params is [String: Any] {
            var params = params as? [String: Any]
            if !(params?.keys.contains("objectId") ?? false) {
                params?["objectId"] = channelName
                propsId = channelName
            }
            newParams = params ?? [:]
        }

        let value = Utils.toJsonString(dict: newParams as? [String: Any])
        var p = ["appId": appId,
                 "sceneName": sceneName,
                 "action": type.rawValue,
                 "roomId": roomId,
                 "objVal": "",
                 "objType": objType,
                 "requestId": UUID().uuid16string(),
                 "props": [propsId: value ?? ""]] as [String: Any]
        if type == .subscribe || type == .unsubscribe || type == .query {
            p.removeValue(forKey: "props")
        }
        if isAdd {
            let attr = Attribute(key: propsId,
                                 value: value ?? "")
            if let successBlockObj = onSuccessBlockObj[objType] {
                successBlockObj(attr)
            }
            if let onCreateBlock = onCreateBlocks[objType] {
                onCreateBlock(attr)
            }
        }
        Log.debug(text: "send params == \(p)", tag: type.rawValue)
        let data = try? JSONSerialization.data(withJSONObject: p, options: [])
        do {
            try socket?.send(dataNoCopy: data)
        } catch {
            Log.errorText(text: error.localizedDescription, tag: "error")
        }
    }

    public func query(channelName: String,
                      roomId: String,
                      objType: String) {
        writeData(channelName: channelName,
                  params: [:],
                  roomId: roomId,
                  type: .query,
                  objType: objType)
    }
    
    public func getRoomList(channelName: String) {
        let params = ["action": "getRoomList",
                      "appId": appId,
                      "sceneName": channelName,
                      "requestId": UUID().uuid16string()]
        let data = try? JSONSerialization.data(withJSONObject: params, options: [])
        try? socket?.send(dataNoCopy: data)
    }

    public func delete(channelName: String, data: Any) {
        var objectIds: [Any] = []
        if let params = data as? [[String: Any]] {
            objectIds = params.compactMap({ $0["objectId"] })
        } else if let params = data as? [String: Any], let objectId = params["objectId"] {
            objectIds = [objectId]
        }
        let p = ["appId": appId,
                 "sceneName": sceneName,
                 "roomId": self.channelName,
                 "objVal": "",
                 "objType": channelName,
                 "action": SocketType.deleteProp.rawValue,
                 "requestId": UUID().uuid16string(),
                 "props": objectIds] as [String: Any]
        Log.debug(text: "delete params == \(p)", tag: "delete")
        let data = try? JSONSerialization.data(withJSONObject: p, options: [])
        try? socket?.send(dataNoCopy: data)
    }

    @objc
    private func enterForegroundNotification() {
        guard socket?.readyState != .OPEN else { return }
        reConnect()
    }
    
    private func roomListHandler(data: [[String: Any]]?) -> [Attribute]? {
        let params = data?.compactMap({ item -> Attribute? in
            let props = item["props"] as? [String: Any]
            return attrsHandler(params: props)?.first
        })
        return params
    }
    
    private func attrsHandler(params: [String: Any]?) -> [Attribute]? {
        let objects = params?.keys
        let attrs = objects?.compactMap { item -> Attribute? in
            let value = params?[item] as? String
            let json = Utils.toDictionary(jsonString: value ?? "")
            if json.isEmpty { // 过滤掉不是json的数据
                return nil
            }
            return Attribute(key: item, value: value ?? "")
        }
        return attrs
    }
}

extension RethinkSyncManager: SRWebSocketDelegate {
    public func webSocketDidOpen(_ webSocket: SRWebSocket) {
        Log.info(text: "连接状态 status == \(webSocket.readyState.rawValue)", tag: "connect")
        connectBlock?(webSocket.readyState.rawValue == 1 ? 0 : webSocket.readyState.rawValue)
        connectBlock = nil
    }

    public func webSocket(_ webSocket: SRWebSocket, didReceiveMessage message: Any) {
        guard let data = message as? Data else { return }
        let dict = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
        let action = SocketType(rawValue: dict?["action"] as? String ?? "") ?? .send
        if action == .ping { return }
        if let msg = dict?["msg"] as? String, msg == "success" {
            Log.info(text: "消息发送成功", tag: "socket")
        }
        let params = dict?["data"] as? [String: Any]
        let objType = dict?["objType"] as? String ?? ""
        let sceneName = dict?["sceneName"] as? String ?? ""
        let channelName = objType.isEmpty ? sceneName : objType
        let realAction = SocketType(rawValue: dict?["action"] as? String ?? "") ?? .send

        if action == .getRoomList, let successBlock = onSuccessBlock[channelName] {
            let params = dict?["data"] as? [[String: Any]]
            let attrs = roomListHandler(data: params)
            successBlock(attrs ?? [])
            return
        }
        
        if let code = dict?["code"] as? String, code != "0", let msg = dict?["msg"] as? String {
            let error = SyncError(message: "\(channelName) \(msg)", code: Int(code) ?? 0)
            Log.errorText(text: "code == \(code)  action == \(realAction) channelName == \(channelName) msg == \(msg)", tag: "error")
            onFailBlock[channelName]?(error)
            return
        }

        let props = params?["props"] as? [String: Any]
        let propsDel = params?["propsDel"] as? [String]
        let propsUpdate = params?["propsUpdate"] as? String
        let objects = props?.keys
        let attrs = attrsHandler(params: props)
        if action == .subscribe {
            if let onUpdateBlock = onUpdatedBlocks[channelName], let propsUpdate = propsUpdate {
                let params = Utils.toDictionary(jsonString: propsUpdate)
                let attrs = params.map({ Attribute(key: $0.key, value: ($0.value as? String) ?? "") })
                attrs.forEach({ onUpdateBlock($0) })
            }
            if let onDeleteBlock = onDeletedBlocks[channelName], realAction == .deleteProp {
                if objects?.isEmpty ?? false {
                    onDeleteBlock(Attribute(key: propsDel?.first ?? "", value: ""))
                    return
                }
                propsDel?.forEach({
                    let attr = Attribute(key: $0, value: "")
                    onDeleteBlock(attr)
                })
            }
        } else {
            if let successBlock = onSuccessBlock[channelName], action == .query || action == .getRoomList {
                successBlock(attrs ?? [])
            }
            if let successBlockVoid = onSuccessBlockVoid[channelName], action == .query, realAction != .deleteProp {
                successBlockVoid()
            }
            if let successBlockObjVoid = onSuccessBlockObjOptional[channelName], action == .query {
                successBlockObjVoid(attrs?.first)
            }
            if let deleteBlock = onDeleteBlockObjOptional[channelName], action == .deleteProp {
                deleteBlock?(attrs?.first)
            }
        }
        Log.info(text: "channelName == \(channelName) action == \(action.rawValue) realAction == \(realAction.rawValue) props == \(props ?? [:])", tag: "收到消息")
    }

    public func webSocket(_ webSocket: SRWebSocket, didFailWithError error: Error) {
        Log.errorText(text: error.localizedDescription, tag: "error")
        connectBlock?(-1)
    }

    public func webSocket(_ webSocket: SRWebSocket, didCloseWithCode code: Int, reason: String?, wasClean: Bool) {
        Log.warning(text: "socket closed == \(reason ?? "")", tag: "closed")
        reConnect()
    }
}
