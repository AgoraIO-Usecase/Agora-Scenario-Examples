#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@protocol ISudASR <NSObject>

/**
 * 传入的音频数据必须是：PCM格式，采样率：16000， 采样位数：16， 声道数： MONO
 * data一定都要是有效数据，否则精确性有影响
 */
- (void) pushAudio:(NSData *)data;

@end

NS_ASSUME_NONNULL_END
