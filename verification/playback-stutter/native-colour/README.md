# 小 sigma 颜色卷积迁移实验 — 2026-11-16

对象：此前真机栈中的 SoftwareBlur（含子调用约20%），不是逐词标量动画或完整渲染器。
独立编写 Rust `native/experimental-mask/src/color.rs`，数学依据为当前
`library/.../internal/rendering/SoftwareBlur.kt`（5de43c6），没有新增外部来源代码。
只处理 sigma<2 的有限 Gaussian：预乘/反预乘、白色与中性色复用、Double及舍入不变。
Kotlin/Java生成归一化系数，避免跨libm差异；整张已裁剪颜色图一次JNI，不逐字跨JNI。

## 结果与边界

- Windows x64 JDK21.0.5、i7-12650H；Android真机2509FPN0BC、arm64/API37。
- 两平台各两次最终独立进程，每次216种输入与108次重复/测量前后比较，324项全部
  输出ARGB、宽高、原点严格相同。另有空/短/heap/只读/未对齐/重叠buffer、维度/半径/
  非法归一化系数的真实JNI拒绝检查；JVM使用-Xcheck:jni。
- 1×1、单行/列、3×7、97×43、320×160，白/灰/彩色、全幅/留白，sigma
  .02/.25/.5/.6/1/1.99；含透明RGB及低alpha。跨平台位图往返本身不同，不跨平台比hash。
- 速度测97×43、640×320、1200×500留白图，每种颜色及.25/.5/1/1.99。
  每项12预热、25次交替Kotlin/Rust；冷工作区另5次，全部P50/P95保留，不选最快值。
  **计时包含读图/裁剪/系数生成/输入输出复制/JNI/输出位图创建**，不包括UI/GPU。
- 第一版逐像素Rust在桌面多数大图更慢（保留initial数据）。最终按行、逐系数累加，
  每个像素仍依次累加-r..r；安全slice便于LLVM沿相邻像素向量化，无fast-math/手写SIMD。
  这是遍历和编译优化共同结果，不能单独归功于语言。
- Android最终两轮1200×500彩色sigma1.99：Kotlin P50 **22.474/30.421ms**，
  Rust含传输 **7.097/8.695ms**；白色sigma.25：**5.104/5.732→3.322/3.615ms**。
  环境负载/温度/功耗未控制，初版与最终轮Kotlin基线也漂移，**不作跨轮前后百分比结论**。
  小图与桌面有不占优项，见完整CSV；不启用为默认，不宣称整会话卡顿解决。
- 复用direct工作区约32N+104字节；最大测试约3.34MiB，另有Java像素/输出与位图。
  cold列0表示无session持有，不表示没有直接内存分配或已立即回收。不是总内存下降结论。
- Android使用独立app_process/合成像素，未安装/停止示例、未读用户歌曲、未替换播放会话。
  两个Android ABI都构建成功；实际JNI运行仅arm64。Linux/macOS未执行。

## 复现

先执行native/experimental-mask现有宿主构建脚本，然后：

```powershell
./gradlew.bat :library:jvmNativeColourProbe -PfoliaNativeMaskDll=<absolute-library-path>
```

Android将本目录Java用JDK javac（release8）编译，再用SDK d8（min-api23、android.jar作lib）
转为probe.zip。推送当前Kotlin APK、probe.zip和arm64 .so到设备的临时目录，执行：

```sh
CLASSPATH=<apk>:<probe.zip> app_process / top.ghhccghk.accompanist.lyrics.folia.NativeColorExperiment <absolute-so> android
```

Java探针无Android专用依赖，反射调用实际生产Kotlin基线，不能偷偷换成未优化旧内核。
CSV保留全部测量，checks.json记录校验范围。初版数据为被替换候选，不用于最终收益宣称。
