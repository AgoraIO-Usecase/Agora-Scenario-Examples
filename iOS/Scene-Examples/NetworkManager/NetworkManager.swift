//
//  NetworkManager.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/19.
//

import UIKit

class NetworkManager {
    enum HTTPMethods: String {
        case GET = "GET"
        case POST = "POST"
    }
    typealias successful = ([String: Any]) -> Void
    typealias failure = (String) -> Void
    
    private lazy var sessionConfig: URLSessionConfiguration = {
        let config = URLSessionConfiguration.default
        config.httpAdditionalHeaders = ["Content-Type": "application/x-www-form-urlencoded"]
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 30
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        return config
    }()
    
    static let shared = NetworkManager()
    private init() { }
    
    func getRequest(urlString: String, success: successful?, failure: failure?) {
        DispatchQueue.global().async {
            self.request(urlString: urlString, params: nil, method: .GET, success: success, failure: failure)
        }
    }
    func postRequest(urlString: String, params: [String: Any]?, success: successful?, failure: failure?) {
        DispatchQueue.global().async {
            self.request(urlString: urlString, params: params, method: .POST, success: success, failure: failure)
        }
    }
    
    /// 生成签名
    func generateSignature(params: [String: Any]?, token: String) -> String {
        guard let params = params else { return "" }
        var value = Array(params.keys)
            .sorted()
            .compactMap({ String(format: "%@", "\(params[$0] ?? "")") })
            .joined(separator: "")
        value += token
        return value.md5Encrypt
    }
    
    private func request(urlString: String,
                         params: [String: Any]?,
                         method: HTTPMethods,
                         success: successful?,
                         failure: failure?) {
        let session = URLSession(configuration: sessionConfig)
        guard let request = getRequest(urlString: urlString,
                                       params: params,
                                       method: method,
                                       success: success,
                                       failure: failure) else { return }
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                self.checkResponse(response: response, data: data, success: success, failure: failure)
            }
        }.resume()
    }
    
    private func getRequest(urlString: String,
                            params: [String: Any]?,
                            method: HTTPMethods,
                            success: successful?,
                            failure: failure?) -> URLRequest? {
        guard let url = URL(string: urlString) else {
            return nil
        }
        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        if method == .POST {
            request.httpBody = convertParams(params: params).data(using: .utf8)
        }
        return request
    }
    
    private func convertParams(params: [String: Any]?) -> String {
        guard let params = params else { return "" }
        let value = params.map({ String(format: "%@=%@", $0.key, "\($0.value)") }).joined(separator: "&")
        return value
    }
    
    private func checkResponse(response: URLResponse?, data: Data?, success: successful?, failure: failure?) {
        if let httpResponse = response as? HTTPURLResponse {
            switch httpResponse.statusCode {
            case 200...201:
                if let resultData = data {
                    let result = String(data: resultData, encoding: .utf8)
                    print(result ?? "")
                    success?(JSONObject.toDictionary(jsonString: result ?? ""))
                } else {
                    failure?("Error in the request status code \(httpResponse.statusCode), response: \(String(describing: response))")
                }
            default:
                failure?("Error in the request status code \(httpResponse.statusCode), response: \(String(describing: response))")
            }
        }
    }
}
