//
//  SampleHandler.m
//  Agora-ScreenShare-Extension(Socket)
//
//  Created by zhaoyongqiang on 2021/11/22.
//


#import "SampleHandler.h"
#import <AgoraScreenShare/AgoraScreenShare.h>

@implementation SampleHandler

- (void)broadcastStartedWithSetupInfo:(NSDictionary<NSString *,NSObject *> *)setupInfo {
    [[AgoraScreenShare shareInstance] startClient];
}

- (void)broadcastPaused {
    // User has requested to pause the broadcast. Samples will stop being delivered.
}

- (void)broadcastResumed {
    // User has requested to resume the broadcast. Samples delivery will resume.
}

- (void)broadcastFinished {
  [[AgoraScreenShare shareInstance] stopClient];
}

- (void)processSampleBuffer:(CMSampleBufferRef)sampleBuffer withType:(RPSampleBufferType)sampleBufferType {
    
    switch (sampleBufferType) {
        case RPSampleBufferTypeVideo:
            [[AgoraScreenShare shareInstance] pushVideoSampleBuffer:sampleBuffer];
            break;
        case RPSampleBufferTypeAudioApp:
            [[AgoraScreenShare shareInstance] pushAudioAppSampleBuffer:sampleBuffer];
            break;
        case RPSampleBufferTypeAudioMic:
//            [[AgoraScreenShare shareInstance] pushAudioMicSampleBuffer:sampleBuffer];
            break;
            
        default:
            break;
    }
}

@end
