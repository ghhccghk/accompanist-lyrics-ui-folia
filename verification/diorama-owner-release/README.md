# Release after partial sampler preparation failure

Baseline28a4b4e, 2026-11-18. This fixes an independently reproduced resource
state error in the internal plain-neighbor owner, not a visual/performance claim.

Sequence: request two actual prepared paragraphs A/B; sampler A succeeds,
sampler B throws the exact injected exception; caller unmounts with an empty
request. The old owner had set `requested=emptyList()` before preparation.
The empty-request shortcut therefore returned while A was still resident.

The new regression first FAILED on unmodified production code: one test/one
failure, expected A's close callback but observed an empty list. Local original
failure log `.work/diorama-plain-owner-partial-failure.log` is retained. This
is not a changed visual expected value or swallowed preparation exception.

Represent incomplete preparation as null rather than successful empty input.
Empty/unmount now evicts A, updates bytes/count to zero and preserves original
shared raster ownership. Explicit close stays idempotent and the thrown cause
is unchanged. The active-base owner already uses this state and its matching
partial-failure/retry/unmount assertions pass as a cross-check.

Five focused JVM tests pass (plain3/active2), Android library and Desktop compile
pass. No shader, source fixture, rendered color/timing, public API, capability,
budget or default changed. No full image recapture was necessary; stable success
path is unchanged. No device, PSS/CPU, real-window or final theme acceptance.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaPlainRendererTest' --tests '*DioramaActiveBaseRendererTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
```
