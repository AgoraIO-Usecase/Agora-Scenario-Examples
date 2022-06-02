//
//  AgoraScreenShare.h
//  AgoraScreenShare
//
//  Created by ZH on 2021/8/20.
//

#import <Foundation/Foundation.h>
#import <ReplayKit/ReplayKit.h>

#ifndef AGORA_ENABLE_ENGINEKIK
#if __has_include(<AgoraRtcKit/AgoraRtcKit.h>)
#define AGORA_ENABLE_ENGINEKIK 1
#import <AgoraRtcKit/AgoraRtcKit.h>
#else
#define AGORA_ENABLE_ENGINEKIK 0
#endif
#endif

#define AGORA_SERVER_PORT 9090

#define AGORA_SERVER_IP @"127.0.0.1"

NS_ASSUME_NONNULL_BEGIN

typedef enum : int64_t {
    AgoraTransferVideoApp = 0,
    AgoraTransferAudioApp = 1,
    AgoraTransferAudioMic = 2,
    AgoraTransferSetUp = 3,
    AgoraTransferSetUpResult = 4,
} AgoraTransferFrameType;

@class AgoraScreenShare;

@protocol AgoraScreenShareDelegate<NSObject>

- (void)screenShareVideoCapture:(AgoraScreenShare *)capture
                   outputBuffer:(CVPixelBufferRef)pixelBuffer
                       rotation:(int)rotation
                           time:(CMTime)time;

- (void)screenShareAudioCapture:(AgoraScreenShare *)capture
                   inAudioFrame:(unsigned char *)inAudioFrame
                      frameSize:(int64_t)frameSize
                      audioType:(AgoraTransferFrameType)audioType
                     sampleRate:(int64_t)sampleRate
                     samples:(int64_t)samples
               channelsPerFrame:(int64_t)channelsPerFrame
                           time:(CMTime)time;

@end

@interface AgoraScreenShare : NSObject

@property (nonatomic, weak) id<AgoraScreenShareDelegate> delegate;

+ (instancetype)shareInstance;

#pragma mark - Host App

#if AGORA_ENABLE_ENGINEKIK

/// 导入引擎,自动处理( 主频道)
/// @param kit AgoraRtcEngineKit
- (BOOL)startServiceWithEngineKit:(AgoraRtcEngineKit *)kit;

/// 导入引擎,自动处理(多频道)
/// @param kit AgoraRtcEngineKit
///
- (BOOL)startServiceWithEngineKit:(AgoraRtcEngineKit *)kit
                       connection:(AgoraRtcConnection *)connection
                       regionRect:(CGRect)regionRect;
#endif

/// 需要自定义AgoraScreenShareDelegate回调处理
- (BOOL)startService;

/// 停止屏幕共享服务
- (void)stopService;

#pragma mark - BroadCast

/// broadcastStartedWithSetupInfo 方法中调用,分辨率默认屏幕宽高
- (BOOL)startClient;

/// broadcastStartedWithSetupInfo 方法中调用
/// @param dimensions 分辨率
- (BOOL)startClientWithInfo:(CGSize)dimensions;

/// broadcastFinished 方法中调用
- (void)stopClient;

/// RPSampleBufferTypeVideo
/// @param sampleBuffer CMSampleBufferRef for Video
- (void)pushVideoSampleBuffer:(CMSampleBufferRef)sampleBuffer;

/// RPSampleBufferTypeAudioApp
/// @param sampleBuffer CMSampleBufferRef for AudioApp
- (void)pushAudioAppSampleBuffer:(CMSampleBufferRef)sampleBuffer;

/// RPSampleBufferTypeAudioMic
/// @param sampleBuffer CMSampleBufferRef for AudioMic
- (void)pushAudioMicSampleBuffer:(CMSampleBufferRef)sampleBuffer;

@end

NS_ASSUME_NONNULL_END
