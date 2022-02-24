# 场景化Demo
此项目包含多个场景Demo，可以输出一个整理APK，也可输出单个场景APK。在 **gradle.properties** 中 **isRunAlone** 进行设置。

目前包含以下场景

|场景|工程名称|
|----|----|
|单主播直播|[SingleHostLive](./modules/SingleHostLive/)|
|PK直播|[LivePK](./modules/LivePK/)|
|小班课|[BreakoutRoom](./modules/BreakoutRoom/)|
|游戏主播PK主播|[RTEGame](./modules/RTEGame/)|
|游戏直播间1V1|[OneLive](./modules/OneLive/)|
|游戏直播间同玩|[ComLive](./modules/ComLive/)|

# 前提条件
开始前，请确保你的开发环境满足如下条件：
- Android Studio 4.0.0 或以上版本。
- Android 5.0 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 使用
#### 注册Agora
前往 [Agora官网](https://console.agora.io/) 注册项目，生产appId

然后替换工程**libs/base-library**中 **strings_config.xml** 中 **rtc_app_id**

如果启用了token模式，需要替换 **rtc_app_token**

**rtm_app_id**可以与**rtc_app_id**一致

**rtm_app_token**可以与**rtc_app_token**一致

其余字段请联系技术支持团队获取