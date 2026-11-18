# Source texture sampling ablation — 2026-11-18

Baseline5bd0613, fixed Folia cc6226e4, same real mounted700ms mesh7/cache7,
380×600/DPR1/60Hz, original452 callbacks, font, camera, material, fog and ACES.
The original layer is still the independently frozen original clone PNG; whole
scene before/after/restore, observed state and callback intervals are unchanged.
Teardown roots0/queued RAF0. No rerendered baseline replaces the original48.

The two additional images are explicitly MODIFIED CLONE DIAGNOSTICS, not original
acceptance frames. Clone actual mesh/material/map; replace cloned Texture Source
with a new Source using the same canvas before needsUpdate so the real original
Source/cache is not marked dirty. Only sampler settings change: linear/no-mip/
anisotropy1 versus trilinear/anisotropy1. No new RAF, geometry or timing changes.
Original texture, material and mesh are not disposed; diagnostic clones are.

| Same four-image union513 | Pixels | Positive RGB mass | MAE vs native |
| --- | ---: | ---: | ---: |
| Original trilinear/aniso4 | 409 | 22421 | 10.972060 |
| Diagnostic linear/no-mip/aniso1 | 194 | 27178 | 0.400260 |
| Diagnostic trilinear/aniso1 | 512 | 20614 | 12.795322 |
| Current native/shared source input | 193 | 27678 | 0 |

The different union513 means these MAEs must not be compared numerically to the
old409-pixel-union13.762021 as a benefit. All variants here share this same union,
including every lowest contribution. No registration, pixel/phase shift, alpha
deletion or expected-value update. Actual per-frame hashes, peaks, bounds, local64
and fixed crop contact are retained. Source-only removal of mipmaps recreates
nearly the native coverage; this demonstrates a real minification contribution
in this control. It does not prove all residuals share this cause, or that the
isotropic control is an acceptable final replacement for source anisotropy4.

Next test a cached explicit mip pyramid with LOD derived from the independently
computed projective Jacobian (no hand-picked LOD), initially on shared cache7.
Retain these sampling diagnostics and the untouched real source as separate
comparisons. Expand frozen full matrices only after the local hypothesis works.

```powershell
# Refuses overwriting fixture:
node verification/classic-browser/capture-diorama-mounted-raster.mjs --sampling
python verification/compare-diorama-mounted-sampling.py
```

Reference-only host changes; no production code or dependency changes, new
Android/real-window/device/performance evidence or final visual acceptance.
