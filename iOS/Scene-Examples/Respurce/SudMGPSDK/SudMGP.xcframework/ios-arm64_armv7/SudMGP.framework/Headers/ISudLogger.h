#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Priority constant for the println method; use Log.v.
 */
#define LogVERBOSE  2

/**
 * Priority constant for the println method; use Log.d.
 */
#define LogDEBUG 3

/**
 * Priority constant for the println method; use Log.i.
 */
#define LogINFO 4

/**
 * Priority constant for the println method; use Log.w.
 */
#define LogWARN 5

/**
 * Priority constant for the println method; use Log.e.
 */
#define LogERROR 6

/**
 * Priority constant for the println method.
 */
#define LogASSERT 7

@protocol ISudLogger <NSObject>
- (void) setLogLevel:(int) level;
- (void) log:(int) level tag:(NSString*) tag msg:(NSString*) msg;
- (void) log:(int) level tag:(NSString*) tag msg:(NSString*) msg error:(NSError *) error;
@end

NS_ASSUME_NONNULL_END