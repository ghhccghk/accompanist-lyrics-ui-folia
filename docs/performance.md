# Kotlin performance probe — 2026-11-14

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
