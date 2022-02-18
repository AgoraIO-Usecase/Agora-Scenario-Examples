#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// APP接入方需要调用handle.success或handle.fail
@protocol ISudFSMStateHandle <NSObject>
-(void) success:(NSString*) dataJson;
-(void) failure:(NSString*) dataJson;
@end

NS_ASSUME_NONNULL_END