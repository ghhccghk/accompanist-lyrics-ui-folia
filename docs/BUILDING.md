# 构建与 Maven 打包

库坐标：`top.ghhccghk.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT`。
Android/JVM变体由Gradle元数据选择；独立的lyrics-core坐标保持原样。
库产物不携带sample或Rust，不需要Cargo。Rust目前仅为sample CPU遮罩实验。
Kotlin/Native 编译试验独立于 Rust/JNI 与 JVM 安装包，见 [可行性与命令](KOTLIN_NATIVE.md)。
目前完整 UI 仍为 Android/JVM；Native 时钟探针不属于歌词库发布目标。

## 工具与平台

通用：JDK21、Android SDK platform37。默认Android sample额外需要NDK28.2.13676358、
Rust1.91.1及aarch64-linux-android/x86_64-linux-android两个target。
Desktop Rust使用本机target；Linux需C链接器，macOS需Command Line Tools。
默认运行时仍使用Kotlin，通过设置开关才加载Rust。

| 主机 | Desktop native资源 | Android native构建脚本 |
| --- | --- | --- |
| Windows x64 | windows-x86_64/*.dll | PowerShell，Windows NDK host |
| Linux x64/arm64 | linux-x86_64或linux-aarch64/*.so | sh，NDK linux-x86_64 host |
| macOS Intel/Apple Silicon | macos-x86_64或macos-aarch64/*.dylib | sh，NDK darwin-x86_64 host |

Android交叉编译需主机能运行对应NDK编译器；不声称Linux ARM主机能直接运行x64 NDK。
Desktop二进制只打包当前构建主机架构，不在Windows生成macOS/Linux二进制，也不宣称通用包。
本轮Windows真实构建/加载验证通过；Linux/macOS仅完成代码和脚本编排测试，尚无真实主机编译、
动态链接、播放器或安装包运行结果。macOS签名、公证和Linux发行版兼容性尚未验证。

Windows脚本依次查找PATH、CARGO_HOME、用户.cargo以及项目已有的.work/rust-bootstrap私有安装，
并为后者设置相邻rustup目录；只作用于子进程，结束恢复环境，不自动安装工具或改系统PATH。
Unix脚本支持PATH、CARGO_HOME和项目已有私有目录。显式指定无效Cargo时报告失败，不换用其他安装。

## 运行

Windows：

```powershell
./gradlew.bat :sample:android-app:assembleDebug
./gradlew.bat :sample:desktop-app:run
```

Linux/macOS：

```sh
./gradlew :sample:android-app:assembleDebug
./gradlew :sample:desktop-app:run
```

没有Rust/NDK时可显式加入`-PfoliaExperimentalMask=false`构建sample；开关会诚实报告不可用。
指定已有工具：`-PfoliaRustCargo=<cargo绝对路径>`。
指定已有Desktop库：`-PfoliaNativeMaskDirectory=<含当前主机动态库的目录>`。
IDE直接启动的VM参数可用`-Dfolia.mask.library=<动态库绝对路径>`。
无需设置java.library.path即可从sample-shared JAR加载对应平台资源。

## Maven

```sh
# 三种库产物、POM、Gradle module元数据和sources写入build/local-maven
./gradlew :library:publishAllPublicationsToVerificationRepository
# 本机标准Maven缓存
./gradlew :library:publishToMavenLocal
# 独立根目录只通过坐标消费：JVM渲染/回调、Android编译、归属声明与依赖边界
python3 scripts/verify-consumer.py
```

Windows使用gradlew.bat及python。校验脚本只需Python3标准库，内部按平台选择wrapper。
本地发布和打包已验证；没有配置或执行远程上传，Maven Central签名/账号/完整发布元数据不在本轮。
如未来要将Rust分发给库消费者，应建立独立可选运行时产物；当前Maven库不宣称包含native。
