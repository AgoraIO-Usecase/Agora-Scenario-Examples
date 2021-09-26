//
//  MainVM+Invoke.swift
//  LivePK
//
//  Created by ZYP on 2021/9/26.
//

import Foundation

extension MainVM {
    func invokeShouldShowTips(tips: String) {
        if Thread.current.isMainThread {
            delegate?.mainVMShouldShowTips(tips: tips)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.mainVMShouldShowTips(tips: tips)
        }
    }
    
    func invokeDidUpdateRenderInfos(renders: [RenderInfo]) {
        if Thread.current.isMainThread {
            delegate?.mainVMDidUpdateRenderInfos(renders: renders)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            self.delegate?.mainVMDidUpdateRenderInfos(renders: renders)
        }
    }
}
