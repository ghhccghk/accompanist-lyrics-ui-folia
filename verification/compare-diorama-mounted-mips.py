"""Actual original GPU mip RGBA versus independent native channel reductions."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
fixture=root/'verification/diorama-fixtures/mounted-mips-v1'
scene=json.loads((fixture/'scene.json').read_text(encoding='utf8'))
old=json.loads((root/'verification/diorama-fixtures/mounted-raster-v1/scene.json').read_text(encoding='utf8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert scene['originalPngSha256']==old['originalPngSha256']
assert scene['state']==old['state'] and scene['callbackIntervals']==old['callbackIntervals']
assert scene['motion']==old['motion'] and scene['rendering']==old['rendering']
assert scene['fontSha256']==old['fontSha256'] and scene['browser']==old['browser']
assert scene['teardown']==dict(roots=0,queuedRafs=0)
source=root/'build/verification/diorama-mounted-mips-browser'
native=root/'build/verification/diorama-mounted-mips-kotlin'
for name in ['mounted-before.png','mounted-after.png']:assert sha(source/name)==scene['originalPngSha256']
base=np.array(Image.open(root/'verification/diorama-fixtures/mounted-raster-v1/cache-7.png').convert('RGBA'))
rows=[]
for mip in scene['mips']:
    data=(source/mip['file']).read_bytes()
    assert hashlib.sha256(data).hexdigest()==mip['sha256']
    if mip['level']!=0:assert (fixture/mip['file']).read_bytes()==data
    a=np.frombuffer(data,dtype=np.uint8).reshape(mip['height'],mip['width'],4).astype(np.int32)
    b=np.array(Image.open(native/f"mip-{mip['level']}.png").convert('RGBA'),dtype=np.int32)
    c=np.array(Image.open(native/f"linear-occupancy-{mip['level']}.png").convert('RGBA'),dtype=np.int32)
    opaque=np.array(Image.open(native/f"occupancy-{mip['level']}.png").convert('RGBA'),dtype=np.int32)
    if mip['level']==0:assert np.array_equal(a,base) and np.array_equal(a,b)
    amask=(a[:,:,3]>0)|(b[:,:,3]>0)
    # Include RGB in zero-alpha texels: those values still contribute to
    # straight texture interpolation. Never drop them by alpha threshold.
    rmask=np.any(a[:,:,:3]!=0,axis=2)|np.any(b[:,:,:3]!=0,axis=2)|np.any(c[:,:,:3]!=0,axis=2)
    alpha_error=abs(a[:,:,3]-b[:,:,3]); color_error=abs(a[:,:,:3]-c[:,:,:3])
    row=dict(level=mip['level'],dimensions=[mip['width'],mip['height']],rawSha256=mip['sha256'],
             alphaMae=float(alpha_error[amask].mean()),alphaPeak=int(alpha_error.max()),
             alphaUnion=int(amask.sum()),colorUnionIncludingTransparent=int(rmask.sum()),
             sourceNonzeroRgbAtZeroAlpha=int(np.count_nonzero(a[:,:,:3][a[:,:,3]==0])),
             sourceRgbAtPositiveAlpha=[int(a[:,:,:3][a[:,:,3]>0].min()),int(a[:,:,:3][a[:,:,3]>0].max())],
             maskedTintRgbMae=float(abs(a[:,:,:3]-b[:,:,:3])[rmask].mean()),
             opaqueSrgbReductionMae=float(abs(a[:,:,:3]-opaque[:,:,:3])[rmask].mean()),
             independentLinearF16RgbMae=float(color_error[rmask].mean()),independentLinearF16RgbPeak=int(color_error.max()),
             nativePngHashes={p.name:sha(p) for p in [native/f"mip-{mip['level']}.png",native/f"occupancy-{mip['level']}.png",native/f"linear-occupancy-{mip['level']}.png"]})
    assert row['alphaPeak']<=1 and row['independentLinearF16RgbPeak']<=1,'Raw texel channel control failed; NOT a visual acceptance gate'
    rows.append(row)
out=root/'verification/diorama-mounted-mips';out.mkdir(exist_ok=True)
report=dict(baseline='47980cb',sourceRevision=scene['revision'],originalSceneStateHistoryAndTextureVersionsUnchanged=True,
            scope=scene['mipScope'],gpuMipZeroEqualsOriginalCanvasExactly=True,
            rawChannelControl='All raw channels, including nonzero RGB at zero alpha; independent F16 linear occupancy reduction and ordinary alpha reduction. No pixel mapping fit or source texels fed to a native renderer.',
            rows=rows,interpretation='Premultiplied mask tint discards the source linear-light straight-RGB mip channel even when alpha closely matches. Independent F16 linear occupancy matches actual GPU mip RGB within1 byte here. Per-fragment color/tone/fog and anisotropic footprint still require rendering proof; these texel controls are not visual acceptance.',
            productionChanged=False,deviceValidated=False,visualAcceptance=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(960,220),'#191919');draw=ImageDraw.Draw(sheet)
for i,level in enumerate([4,5]):
    m=next(m for m in scene['mips'] if m['level']==level)
    a=np.frombuffer((fixture/m['file']).read_bytes(),dtype=np.uint8).reshape(m['height'],m['width'],4)
    for j,(label,data)in enumerate([('GPU straight RGB (alpha ignored)',a[:,:,:3]),
                                  ('Native premult mask RGB (alpha ignored)',np.array(Image.open(native/f'mip-{level}.png').convert('RGB'))),
                                  ('Independent linear occupancy',np.array(Image.open(native/f'linear-occupancy-{level}.png').convert('RGB'))) ]):
        draw.text((j*320+3,i*110+3),f'L{level} '+label,fill='white')
        # Show a fixed3x nearest scale of these small mip texels, no metric use.
        im=Image.fromarray(data).resize((m['width']*3,m['height']*3),Image.Resampling.NEAREST)
        sheet.paste(im,(j*320+3,i*110+25))
sheet.save(out/'contact.png')
print(json.dumps(rows,indent=2))
