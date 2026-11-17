# Bounded Diorama text-plane capability probe

Folia cc6226e464050a456a2913e062374f23d3844cc5 (AGPL-3.0-only), unchanged checkout.
Three.js0.185.1 exactly matches that checkout's package-lock; verification-only,
MIT notice preserved in LICENSES. No new Kotlin/native runtime dependency.

generate-diorama-projection.mjs executes actual cameraPath.ts and
dioramaSceneUnits.ts via Vite SSR, plus actual Three PerspectiveCamera and
Quaternion.3719 source rows:4 invented seeds (including emoji UTF-16 hash),
12 frames,4 weave levels,3 widths, plane axes/corners, fit and near/far lifetime.
DioramaProjectionTest compares at1e-8; observed maximum1.3058638614893425e-9.
Homography vs direct projection1e-9; tests distinguish near/far/behind clipping.
These are frozen views, **not original CameraRig playback**.

capture-diorama-projection.mjs uses a dedicated4174 Vite server, one headless
Chromium151.0.7922.34 page at a time, SwiftShader software WebGL. Both server and
browser close in finally. It imports actual original path,placement,frameQuaternion,
frame-fit and rasterDioramaLine; actual Three MeshBasicMaterial/PlaneGeometry
projects the plane. No React, background, fog, active-word effects or depth
occlusion is claimed. Explicit3 frozen eye offsets ×380/800/801×600,DPR1.
Same original already-shaped texture (invented text, shared OFL font) is drawn
by JVM Compose Canvas with the independently computed projective4×4 Matrix.
It tests perspective rather than faking depth with scale/rotation. It deliberately
does **not** test Kotlin text shaping. No source rendering algorithms replaced.

9 source/native pairs: whole nonbackground RGB union including low alpha;
MAE14.5183063024, native/source ink bounds differ up to2px; all frame/channel/local
64px errors and hashes are in comparison.json. Contact image inspected; sampling
and antialiasing differences remain. Matrix Float corner check.001px passed.
Random reverse view changes reproduce identical pixels. No registration,
threshold/alpha deletion or latency fit. Neither this metric nor internal math
constitutes public Diorama acceptance.

```
node verification/classic-browser/generate-diorama-projection.mjs
node verification/classic-browser/capture-diorama-projection.mjs
./gradlew :library:jvmTest --tests '*Diorama*Test'
python verification/compare-diorama-projection.py
```
Use gradlew.bat on Windows. pnpm install --frozen-lockfile inclassic-browser
before source generation. Actual-source fixture planes.json records backend and
scope. Large generated source/native PNGs remain underignored build/.
Source raster fixture is30KB generated invented text; font provenance remains
verification/assets/README.md, no SF Pro or user resources.

Failed CJS alias attempt preserved .work/diorama-projection-source.log; corrected
to the same package's ESM build (no algorithm edits), source-second.log success.
Initial browser optimization scanned unrelated existing HTML fixtures and printed
unresolved unrelated application imports; capture succeeded. Later dedicated
server disables dependency discovery to constrain its scope, then reran.
Numeric/JVM draw: .work/diorama-canvas-first.log. Android is build-only;
no real Desktop window, Android device or performance claim. Full remaining
scope and source map: docs/diorama-investigation.md.
