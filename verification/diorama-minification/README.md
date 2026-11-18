# REJECTED isotropic explicit-pyramid candidate — 2026-11-18

Baselinec0e2804, fixed original Folia cc6226e4. Full candidate preserved in
rejected-candidate.patch and REVERSED from production. Do not adopt this as
equivalence or relax acceptance to its improved aggregate.

The candidate prepares a halving pyramid once per complete shaped paragraph,
selects two levels from the independently computed inverse screen Jacobian at
the plane center, and mixes premultiplied contributions through Plus in a
temporary transparent layer. Geometry, shaping, clocks/camera, materials, tint,
fog and original images/times are unchanged. No fitted LOD, frame delay or font
curve. This approximates isotropic minification, not original per-fragment
anisotropy4/straight-texture RGB sampling. Extra preparation/memory and layer
cost are unmeasured; there is NO performance benefit claim.

All original48 mounted and24 Latin/Arabic/CJK/long-line images remain unchanged.
Before/after use the SAME per-frame all-nonbackground union across all modes,
including every lowest contribution. The union differs from older reports;
the older mean must not be compared to this new union as a benefit.

| Frozen comparison | Before MAE | Candidate MAE | Mean regressions | Local regressions |
| --- | ---: | ---: | ---: | ---: |
| Mounted finite camera, analytic material48 | 6.428287 | 5.255995 | 2 | 0 |
| Mounted finite camera, finite material48 | 6.369447 | 5.197094 | 2 | 0 |
| Mounted media camera48 | 18.915137 | 17.985042 | 1 | 0 |
| Plain24 | 10.544128 | 8.249781 | 7 | 6 |

Fixed contact includes the failures. Near Latin800 MAE8.392917→9.528935 and local
17.171635→20.666667; Arabic801 local15.388889→17.055556. The original actual whole
word outlines are visibly crisper than the candidate. At700ms the shared-cache
candidate spreads608 pixels vs actual original409 (current native193), with RGB
mass27777 vs22421. Local improvement alone is insufficient to accept that spread.
All regressions, bounds, peaks, hashes and source intervals are retained. No
registration, low-alpha deletion, resampled oracle or replaced expected values.

Candidate validation:8 existing mounted/plain/breath/window JVM tests;6 new
pure-Jacobian/pyramid/parent-alpha contract tests; Android library and Desktop
compile passed. Initial command used nonexistent desktop compileKotlinJvm; the
correct compileKotlin command passed, original failure retained. Passing tests
do not make this visually acceptable. Restored5 mounted/plain tests pass with
all168 baseline full-stage PNG hashes unchanged. No new runtime/device evidence.

Raw before and rejected PNGs remain ignored under .work; only necessary fixed
contact, numerical report and reproducible source patch are committed. To
reproduce, start with c0e2804-compatible production, generate its mounted/plain
outputs and preserve them before applying the rejected patch:

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest' --tests '*DioramaPlainTextTest'
python verification/compare-diorama-minification.py --save-before
git apply verification/diorama-minification/rejected-candidate.patch
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest' --tests '*DioramaPlainTextTest'
python verification/compare-diorama-minification.py
git apply --reverse verification/diorama-minification/rejected-candidate.patch
```

The comparer refuses mismatched candidate production hashes and baseline
overwrites. Reproduction also needs the unchanged original72 images/manifest;
use their existing captures, not newly selected frames.

Next isolate the original GPU-generated mip RGBA against the native premultiplied
mask pyramid, including transparent-texel RGB. Constant post-tone tint and mask
alpha may discard source straight-texture RGB; inspect actual data before
another implementation. Do not merely select a lower LOD to sharpen the failures.
