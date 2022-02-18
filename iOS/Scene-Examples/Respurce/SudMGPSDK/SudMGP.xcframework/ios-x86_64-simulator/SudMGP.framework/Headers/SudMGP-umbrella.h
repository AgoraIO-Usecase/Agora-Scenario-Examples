#ifdef __OBJC__
#import <UIKit/UIKit.h>
#else
#ifndef FOUNDATION_EXPORT
#if defined(__cplusplus)
#define FOUNDATION_EXPORT extern "C"
#else
#define FOUNDATION_EXPORT extern
#endif
#endif
#endif

#import "ISudListenerGetFQSInfo.h"
#import "ISudListenerGetMGInfo.h"
#import "ISudAPPD.h"
#import "ISudASR.h"
#import "ISudFSMMG.h"
#import "ISudFSMStateHandle.h"
#import "ISudFSTAPP.h"
#import "ISudListenerASR.h"
#import "ISudListenerGetMGList.h"
#import "ISudListenerInitSDK.h"
#import "ISudListenerNotifyStateChange.h"
#import "ISudListenerUninitSDK.h"
#import "ISudLogger.h"
#import "SudMGP.h"
#import "IRTSS.h"
#import "IRTSSEventHandler.h"
#import "RTSSResult.h"
#import "SmRtAsr.h"
#import "SmRtAsrClient.h"

FOUNDATION_EXPORT double SudMGPVersionNumber;
FOUNDATION_EXPORT const unsigned char SudMGPVersionString[];

