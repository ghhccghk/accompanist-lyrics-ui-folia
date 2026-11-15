# 云阶对照输入与复现

固定源：Folia v0.7.26，`cc6226e464050a456a2913e062374f23d3844cc5`。
上游 checkout 必须干净；所有歌词均为本项目合成文本，字体归属见 `verification/assets`。

- `layout.tsv`：直接执行原版布局及避让函数；216 布局、612 行。
- `chunk-motion*.tsv`：挂载原版 Partita/PartitaChunk/GlowWord，记录实际 Motion 与 DOM。
- `word-filter.tsv`：原版短词 active→passed 时的正文滤镜；不以自写 JS 曲线作期望。
- `scenes.tsv`：补充紧密短词，和 classic-fixtures/scenes.tsv 共同作为两侧输入。

在 `verification/classic-browser` 安装 pnpm-lock.yaml 锁定的依赖，并运行
`node verification/classic-browser/serve.mjs`（仓库根目录）。另一个终端依次执行：

```text
node verification/classic-browser/generate-partita.mjs
node verification/classic-browser/capture-partita.mjs
node verification/classic-browser/capture-partita.mjs --tight-only
node verification/classic-browser/capture-partita.mjs --avoid-only
node verification/classic-browser/capture-word-filter.mjs
gradlew.bat :library:jvmTest --tests "*Partita*" --max-workers=2
python verification/compare-partita.py --include-tight --include-avoid
```

参考 PNG 和字体 SHA 位于 `build/verification/partita-browser`；Compose PNG 位于
`build/verification/partita-kotlin`，比较报告及接触表在 `partita-kotlin-comparison`。
这些目录均为本地生成产物，不提交图片或缓存。测试在每个时间点推进 UI 时钟并逆序重访，
检查暂停冻结和直接重建；画面误差另行记录，测试通过不代表视觉验收通过。

两侧均 380/800×600、DPR=1、同字体、同强度、同媒体时间。正文 shell 不包含播放器、
完整背景引擎及共享字幕，不能由此推定这些内容或 Android 设备运行已验收。
