# Pendolo explicit chorus: masked ink and sequential glow

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
AGPL-3.0-only. Original `pendoloMotionProfile.ts` and
`PendoloActiveLyricSweep.tsx:175–200` map to `PendoloChorus.kt`,
`PendoloChorusLayer.kt` and `PendoloSweepTarget.kt`. Browser imports the
unchanged source component. No unknown assets; same covered fixture fonts.

Port preserves three intensity profiles, focal-only activation, .58/.32
accent mixes, rounded CSS filter sigmas and padding. Transport pause does
not deactivate a focal chorus. Keyword clips retain complete shaped rows.
Filters run **after** the sweep mask; the second shadow consumes original
ink plus the first shadow, rather than a second independent text shadow.
Native blur uses existing CSS-sigma conversion; pre-31/software path uses
the existing premultiplied blur. Layer lifetimes are composition scoped.

60 actual-source parameter cases are exact. Four synthetic scenes, two widths,
three intensities, 11 media times:264 browser/native pairs,528 measured row/
prefix fields exact.132 additional forced-software pairs. Pause, randomized
seek/resize, remount, palette and chorus disable/restore checks pass. All400
prior sweep/colored/static/mounted native PNG hashes are unchanged.

Primary foreground MAE mean3.124627, peak9.237356; forced-software mean3.403482,
peak8.729687. No alignment, phase or color fitting. `contact.png` inspected.
Residual AA/gradient/coverage remains. Smaller mean than the non-chorus matrix
is not proof of improvement: glow changes foreground-mask membership.
These are isolated sweeps, not halo/parent-scale/full-theme acceptance.

All Pendolo JVM tests, Android debug APK assembly and Desktop compile pass:
`.work/pendolo-chorus-final.log`. No device or visible application was launched;
forced-software JVM is not Android device verification. Runtime performance/
memory of dynamic chorus blur has not been measured.

Reproduce:

```
node verification/classic-browser/generate-pendolo-chorus.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-chorus-sweep.mjs
./gradlew :library:jvmTest --tests '*Pendolo*'
python verification/compare-pendolo-chorus-sweep.py
```

Raw PNGs live in `build/verification/pendolo-chorus-sweep-{browser,kotlin,software}`;
tracked conditions, source inputs, frame list and hashes specify this matrix.
Next: actual original wheel halo/parent-scale, activation interruptions and
full-wheel color/glow comparison before exposing any public Pendolo preview.
