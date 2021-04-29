# 场景化Demo
此项目包含多个场景Demo，可以输出一个整理APK，也可输出单个场景APK。在 **gradle.properties** 中 **isModule** 进行设置。

目前包含以下场景

|场景|工程名称|
|----|----|
|互动播客|[InteractivePodcast](./InteractivePodcast/README.md)|
|相亲|[MarriageInterview](./MarriageInterview/README.md)|

# 前提条件
开始前，请确保你的开发环境满足如下条件：
- Android Studio 4.0.0 或以上版本。
- Android 4.1 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 使用
#### 注册Agora
前往 [Agora官网](https://console.agora.io/) 注册项目，生产appId，然后替换工程**data**中 **strings_config.xml** 中 **app_id**。

#### 数据源
- 本项目目前提供了2种数据接入：**leancloud** 和 **firebase**，可以在 Android Studio 的 Build Variants 中进行切换。
- 如果需要自己实现数据源，请参考项目 **data** 代码中实现，主要继承接口 **IDataProvider** 实现具体方法。

##### 注册Leanclould
1. 前往 [Leancloud官网](https://www.leancloud.cn/) 注册项目，生产 appId、appKey、server_url。
- 替换工程 **data** 中  **strings_config.xml** 中 **leancloud_app_id**、**leancloud_app_key**、**leancloud_server_url**。
- 替换 [LeanCloudHelp.py](https://github.com/AgoraIO-Usecase/Scene-Examples/blob/master/leanCloudHelp.py) 中 **appid** 和 **appkey**。
2. 安装 [Python](https://www.python.org/)，如果已经安装请忽略。
3. Python安装之后，控制台执行以下命令。
```
pip install leancloud
或者
pip3 install leancloud
```
4. Android Studio Terminal 中执行文件 [LeanCloudHelp.py](https://github.com/AgoraIO-Usecase/Scene-Examples/blob/master/leanCloudHelp.py)。
```
python ./LeanCloudHelp.py
或者
python3 ./LeanCloudHelp.py
```

##### 注册Firebase
前往 [Firebase官网](https://firebase.google.com/) 注册项目，生成文件 **google-services.json**，然后放到对应工程下面。比如使用InteractivePodcast，那目录结构 **InteractivePodcast/google-services.json**。

#### 运行示例项目
1. 开启 Android 设备的开发者选项，通过 USB 连接线将 Android 设备接入电脑。
2. 在 Android Studio 中，点击 Sync Project with Gradle Files 按钮，同步项目。
3. 在 Android Studio 左下角侧边栏中，点击 Build Variants 选择对应的平台。
4. 点击 Run app 按钮。运行一段时间后，应用就安装到 Android 设备上了。
5. 打开应用，即可使用。
