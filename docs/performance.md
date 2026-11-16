# Kotlin performance probe — 2026-11-14

2026-11-16 最新：示例已增加可选 Rust CPU 辉光遮罩开关和真实 JNI 接入，默认 Kotlin。
Android debug 可选 arm64/x86_64，Windows 真实 JNI/同帧对照通过，Android 尚未设备运行；
详见 [示例 A/B 验证与运行](native-mask-sample.md)。本节覆盖下方历史“未接示例”描述，
不表示完整 native 渲染器、多主题效果验收或设备性能已完成。

## Rust JNI遮罩实验 — 2026-11-16（最新，未启用生产）

用户授权后，已独立实现native/experimental-mask，真实构建Windows x64 DLL并通过JNI运行。
不是仅写Rust草案或无声Kotlin回退。Rust1.91.1、jni-sys0.3.0/Cargo.lock、release thin LTO，
无fast-math/SIMD/线程。JDK21.0.5、Windows x64、i7-12650H、16逻辑核、512MiB heap，
其他程序存在、温度/功耗未控制，不是整帧或Android性能结论。工具链仅在忽略的.work内，
未改PATH/注册表。DLL加载通过；Cargo测试EXE的LLD import-library链接失败，未声称通过。

30组随机尺寸/半径（1×1到640×320，sigma1/2.25/10/20/4096）与生产Kotlin alpha逐像素相同，
同算法Kotlin候选也逐像素相同；只读/堆/未对齐/过小/重叠buffer、NaN、非法尺寸/数量拒绝，
JVM使用-Xcheck:jni。所有native scratch与结果由Java持有，调用同步、不保存指针/句柄。
第一轮审查发现只读输出检查缺失和kernel工作量不同，修复后完整重测三进程。

下表为三个独立进程各12预热、40次交替顺序测量的P50范围，单位ms，非最快值：

| 路径 | 320×160 | 640×320 |
| --- | ---: | ---: |
| 原Kotlin kernel（含两个ARGB结果打包） | 0.9932–1.0028 | 5.6996–6.8491 |
| 同连续纵向扫描Kotlin kernel | 0.6964–0.7069 | 3.0543–3.1259 |
| Rust JNI kernel（同ARGB工作量） | 0.8140–0.8320 | 3.6016–3.6024 |
| 当前生产Kotlin双遮罩prepare | 2.6342–2.6788 | 9.8907–10.1643 |
| 复用scratch＋连续扫描Kotlin候选prepare | 1.9304–1.9442 | 6.7867–6.8478 |
| Rust JNI prepare（含输入/输出传输、位图创建/读取） | 1.9015–1.9620 | 7.2848–7.4058 |

CPU纵向扫描改成按连续行写入（相同递推/量化）才带来主要收益；Rust对比未经优化Kotlin
有改善，但对同算法Kotlin没有稳定优势，640×320准备仍更慢。不能据此正式切换native。
Rust prepare JVM分配约1,026,135/4,098,135B，另持有1,433,608/5,734,408B direct buffers；
Kotlin候选约1,435,952/5,736,752B。native位图/driver内存、长期GC/GPU未测，不能仅按JVM
分配宣布总内存改善。内核输出与bitmap读取工作量已对齐；preparation方案分配策略仍不同。

原始三轮P50/P95/P99、条件、校验和在verification/native-mask/windows-x64-run-{1,2,3}.txt。
中间失败/偏置基准留.work/rust-mask-*，不拿作最终收益证据。复现见native/experimental-mask/README.md。
下一项：先将已证明的连续扫描Kotlin候选以像素回归验证接入，再测真实歌词准备/整帧；
Rust原型保留供后续更合适的热点继续比较。默认库/示例无native依赖，Android未构建/加载Rust，
新主题效果验收与未实现主题继续按themes-parity.md推进，不能由基准替代。

### 已落地：Kotlin连续纵向扫描

`GlowMask.kt:boxPass`只改变纵向遍历顺序，使用上一输出行的rolling sums，保留每列
初始化/减后加顺序、Double精度、轴末量化、偶窗口偏移、六次buffer交换及所有权。
未引入pool/额外native依赖；遮罩与API<31预乘颜色fallback共用此内核。
独立静态审查无新增问题。GlowMaskTest与旧递推快照逐值比较72组随机整数/小数输入，
含单行/列、奇偶/超大窗口。JVM classic原色/white mask/软件fallback/连续序列372帧，
同当前源码基线与优化版PNG SHA全部相同，暂停/逆向检查通过。最初比较旧software
缓存发现30张不同，故重新构建当前HEAD旧内核并完整重采，证实是旧素材不能当有效基线；
没有替换原版期望或隐藏失败。散列见verification/native-mask/kotlin-contiguous-pixel-regression.json。

同进程12预热、40次交替旧/新CPU+ARGB配对：320×160 P50 1.0635→0.9444ms（约11%），
640×320 5.7849→4.1757ms（约28%），P95 6.0796→4.4226ms。原始条件/尾值在
kotlin-contiguous-traversal.txt。这是单进程局部观察；先前独立MaskBenchmark
2.3171→1.5693ms受进程/JIT影响，随后旧内核复跑1.6476ms，不宣称稳定32%整准备提升。
该优化后两端构建通过；未启设备/真实Desktop窗口，未证明用户实际掉帧已解决。

This is an initial logic/allocation measurement, not a rendering, frame-time or
60Hz result. Android CPU/GPU, mask preparation, texture upload, GC peaks and full
sample frame percentiles remain unmeasured. No Rust experiment is justified by
the currently measured query cost; retain the Kotlin implementation.

Environment: Intel Core i7-12650H, 16 logical CPUs, Windows x64 (JVM reports
Windows 10 10.0), OpenJDK 25.0.4, test heap 512 MiB. Other applications/builds
were present; power/thermal state was not controlled. No surface/font/GPU used.

Probe: 1000 original synthetic lines (1s spacing, 800ms duration), seed 20261114,
30,000 warmup queries, 20 batches × 10,000 uniformly random seeks. Percentiles
below are **batch-average ns/query**, not individual sample or frame tails.
Allocation uses the JVM ThreadMXBean for the measurement thread.

| Metric | Before | After |
| --- | ---: | ---: |
| Visual timeline preparation (single cold measurement), ms | 74.26 | 44.46 |
| Query P50, ns | 791.18 | 77.40 |
| Query P95, ns | 1404.64 | 153.07 |
| Query P99, ns | 1427.00 | 155.73 |
| Allocated bytes/query | 1429.88 | 0.00636 |

Same query checksum: 114691800. These are one before/after process run each;
they establish a useful local observation, not a statistically controlled speedup.

Changes: prepare immutable VisualSelection values at every source/epoch/exit
boundary; binary search returns the prepared value without query-time lists.
Replace quadratic latest-primary scan with an advancing cursor, preserve stable
equal-start priority into tails, and reuse source selections within a boundary.
Tradeoff: additional O(boundaries + retained secondary memberships) preparation
storage. Extreme simultaneous voices still need a separately measured budget.

Reproduce: `./gradlew :library:jvmTest --tests '*ComputeBenchmark*' --rerun-tasks`.
Report: `library/build/verification/compute-benchmark.txt` (ignored build output).
Functional tests remain separate and contain no machine-dependent latency gate.

Next measurements: mask generation cold/warm latency and allocations, 300-unit
layout, dense transitions, resize, full-sample frame distribution, Android midrange
device CPU/GPU and memory. Optimize measured preparation/rendering hotspots before
considering JNI, and include all transfer/lifetime costs in any future experiment.

After pinning the JVM toolchain to 21 for independent-consumer compatibility,
the same optimized probe on OpenJDK 21.0.5 measured preparation 42.40ms and
batch-average query P50/P95/P99 84.42/121.79/158.96ns, 0.00636 B/query.
This is a separate observation; it is not compared as a JVM-version speedup.

## Paired halo preparation — 2026-11-15

MaskBenchmark measures two masks (radii 10/20) from an original 320×160 alpha
rectangle. Same host, JDK 21.0.5, 512MiB test heap, one thread; one cold probe,
10 warmups, 30 individual paired preparations. It includes bitmap read/conversion
and CPU blur, excludes glyph shaping/rasterization, native allocations and GPU.

| Metric | Separate preparation | Shared source/scratch |
| --- | ---: | ---: |
| Cold probe ms | 10.2381 | 9.8875 |
| Warm P50 ms | 3.0322 | 2.3417 |
| Warm P95 ms | 6.4588 | 2.7209 |
| Warm max ms | 8.0232 | 2.7548 |
| JVM allocated bytes/pair | 2,050,840 | 1,231,095 |

One process run per variant, uncontrolled host load: these observations do not
establish whole-renderer throughput or statistically controlled latency gains.
Checksum remains 16851. The optimization reads one rasterized glyph source and
reuses three CPU arrays across radii, while each output bitmap owns its pixels.
ClassicLine now rasterizes each shaped unit once for both layers and avoids
repreparing white masks when only tint/breathing changes.

All 102 classic/reduced-motion checkpoint hashes were identical before/after.
GlowMaskTest independently checks three-pass integer rounding and transparent
edges against a naive convolution, including radii exceeding image dimensions
and repeated radius output after scratch reuse. Reproduce with
`./gradlew :library:jvmTest --tests '*MaskBenchmark*'`; report is
`library/build/verification/mask-benchmark.txt`. Total scene budgets, shaping,
real-window frame tails and Android CPU/GPU measurements remain open.
