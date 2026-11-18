# Actual original GPU mip color/alpha — 2026-11-18

Baseline47980cb, fixed Folia cc6226e4, same380×600/DPR1/60Hz/incoming700ms,
font, Motion/direct dependency/browser locks and complete452-callback history.
Read ACTUAL already-uploaded/generated cache7 mip0/4/5 through a temporary
READ-only framebuffer attachment. No source texture write, new render/RAF or
ref/matrix mutation. Restore READ framebuffer/read buffer exactly, DRAW untouched;
original texture AND Source versions checked unchanged. Full original PNG/state
before/after and every callback interval match the independently frozen source.
Unmount roots0/queued RAF0. Only reference host code changes; upstream clean.

Exact raw RGBA is retained before any Canvas PNG conversion: tiny-alpha straight
RGB can be rounded by Canvas premultiplied storage. Level0 is EXACTLY equal to
the existing source-cache7 PNG RGBA; its1.5MB bytes remain ignored. Only mip4/5
raw bytes (5768+1428 bytes) are committed as necessary synthetic fixtures, with
original independent hashes. No user lyric/font/cover or source runtime added.

Native starts from that SAME base image as an input control, not a desired
render output. The rejected ordinary premultiplied-mask pyramid and a separate
opaque color-occupancy pyramid are prepared independently. For this source
white-on-transparent raster, RGB is1 at every nonzero-alpha base texel and0 at
zero-alpha texels. Reduce that opaque occupancy separately in linear F16, then
encode to sRGB; alpha is reduced through the ordinary mask path. No fitted
gamma, alpha curve, threshold deleting contributions or original mip input.

| Actual mip | Alpha MAE / peak | Original RGB range at alpha>0 | Mask-tint RGB MAE | Linear-F16 RGB MAE / peak |
| --- | ---: | --- | ---: | ---: |
| 0 | 0 /0 | 255..255 | 0 | 0 /0 |
| 4 | .044393 /1 | 14..255 | 70.344595 | .123874 /1 |
| 5 | .055215 /1 | 11..251 | 102.155689 | .197605 /1 |

Color union includes ALL nonzero source/native color, notably48/12 RGB channel
values at ZERO alpha in actual mip4/5. Those values still affect later straight
texture interpolation and are not discarded. Opaque8bit sRGB-space reduction
also fails (37.851351/58.455090 MAE); linear-light separation matters. This
explains why simply minifying the alpha mask and applying constant post-tone
tint is incomplete. Contact explicitly ignores alpha to expose raw RGB;
it is not a rendered lyric comparison. Full numbers/hashes retained.

One targeted JVM input test passes. The raw-channel check within1 byte is only
for these independently captured8bit mip texels; it is NOT a replacement for
the unchanged full-stage visual criteria or proof of per-fragment equivalence.
No production sampler, public Diorama, Android/window/device/performance pass.
The rejected isotropic candidate remains reversed, with all168 restored hashes.

```powershell
# Initial capture refuses existing fixture:
node verification/classic-browser/capture-diorama-mounted-raster.mjs --mips
.\gradlew.bat :library:jvmTest --tests '*DioramaMipRasterTest'
python verification/compare-diorama-mounted-mips.py
```

The capture's recorded host hashes describe the exact first capture; later
host persistence only copies small mip4/5 raw fixtures, no source calculation
change. Next a bounded native fragment control must preserve independent RGB
and alpha through sampling, THEN tint→ACES→sRGB→fog→blend. Validate against the
same original layer and labelled isotropic control before considering any
production integration; handle the actual anisotropic footprint separately.
