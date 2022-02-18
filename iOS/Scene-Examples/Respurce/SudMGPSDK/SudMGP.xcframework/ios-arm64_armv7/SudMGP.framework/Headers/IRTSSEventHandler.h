//
//  Created by Herbert on 2021/10/21.
//

#import <Foundation/Foundation.h>
#import "RTSSResult.h"


@protocol IRTSSEventHandler <NSObject>

-(void)onRtssHit:(RTSSResult *)result repeated:(BOOL) repeated;

-(void)onRtssNoHit:(RTSSResult *)result;

-(void)onRtssError:(NSInteger)errCode errMsg:(NSString *)errMsg;

@end
