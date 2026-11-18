"""Same unchanged mounted original / old native / follower-native 144 frames."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/pendolo-tick-stage-browser'
before=root/'.work/pendolo-follower-before/native'
after=root/'build/verification/pendolo-tick-stage-kotlin'
out=root/'verification/pendolo-follower';out.mkdir(exist_ok=True)
records=[]
for line in (root/'verification/pendolo-fixtures/tick-stage-frames.tsv').read_text().splitlines():
    if not line or line.startswith('#'):continue
    fields=line.split('\t');name=fields[0]+'.png'
    paths=[p/name for p in (source,before,after)]
    arrays=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in paths]
    assert len({a.shape for a in arrays})==1
    mask=np.logical_or.reduce([np.any(a!=[22,36,53],axis=2) for a in arrays])
    r=dict(file=name,png_sha256={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths})
    for mode,a in zip(['before','after'],arrays[1:]):
        d=abs(arrays[0]-a)
        local=[float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
            for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if mask[y:y+64,x:x+64].any()]
        r[mode]=dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0,
                     full_mae=float(d.mean()),worst64=max(local,default=0),channel_peak=int(d.max()))
    records.append(r)
assert len(records)==144
numeric={}
for mode,folder in [('before',before),('after',after)]:
    rows=[l.split('\t') for p in folder.glob('*-residual.tsv') for l in p.read_text().splitlines() if l and not l.startswith('#')]
    numeric[mode]=dict(tick={f'tick{i}':max(float(r[-1]) for r in rows if r[1]==f'tick{i}') for i in range(4)},
        content_rect_peak=max(float(r[-1]) for r in rows if not r[1].startswith('tick') and int(r[2])<4))
summary=dict(frames=len(records),foreground_mean={m:float(np.mean([r[m]['foreground_mae'] for r in records])) for m in ['before','after']},
    worst_frame={m:max(r[m]['foreground_mae'] for r in records) for m in ['before','after']},
    worst64={m:max(r[m]['worst64'] for r in records) for m in ['before','after']},numeric=numeric,
    changed_native_frames=sum(list(r['png_sha256'].values())[1]!=list(r['png_sha256'].values())[2] for r in records))
conditions=json.loads((root/'verification/pendolo-fixtures/follower-v1/conditions.json').read_text(encoding='utf8'))
raw=(root/'build/verification/pendolo-follower/raw-summary.tsv').read_text().splitlines()[-1].split('\t')
stops=[l.split('\t') for l in (root/'build/verification/pendolo-follower/stops.tsv').read_text().splitlines()[1:]]
summary['independent_source']=dict(raw_peak=float(raw[0]),root_boundary_representations=int(raw[1]),
    first_rest_peak=max(float(r[-1]) for r in stops),
    semantic_track_peak=float((root/'build/verification/pendolo-follower/track-summary.tsv').read_text().splitlines()[-1]))
legacy=[l.split('\t')for l in (root/'build/verification/pendolo-follower/legacy-differences.tsv').read_text().splitlines()[1:]]
summary['legacy_dispatcher_expectation_migration']=dict(different_fields=len(legacy),
    peak=max((float(r[-1])for r in legacy),default=0),
    differences=[dict(scene=r[0],intensity=r[1],snap=float(r[2]),time=float(r[3]),field=int(r[4]),legacy=float(r[5]),corrected=float(r[6]),error=float(r[7]))for r in legacy])

# The NEW27-frame input has a legacy-rotation control, not a historical public
# binary labelled as before. Actual after images MUST come from FoliaLyricsView.
stage_source=root/'build/verification/pendolo-follower-stage-browser'
stage_native=root/'build/verification/pendolo-follower-stage-kotlin'
stage=[]
for line in (root/'verification/pendolo-fixtures/follower-stage-v1/frames.tsv').read_text().splitlines()[1:]:
    name=line.split('\t')[0];paths=[stage_source/name,stage_native/('legacy-'+name),stage_native/name]
    arrays=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in paths]
    mask=np.logical_or.reduce([np.any(a!=[22,36,53],axis=2)for a in arrays])
    row=dict(file=name,png_sha256={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest()for p in paths})
    for mode,a in zip(['legacy_control','public'],arrays[1:]):
        d=abs(a-arrays[0]);local=[float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())for y in range(0,600,64)for x in range(0,380,64)if mask[y:y+64,x:x+64].any()]
        row[mode]=dict(foreground_mae=float(d[mask].mean()),worst64=max(local,default=0),channel_peak=int(d.max()))
    stage.append(row)
assert len(stage)==27
stage_rows=[l.split('\t')for p in stage_native.glob('*.tsv')for l in p.read_text().splitlines()[1:]]
stage_summary=dict(frames=27,
    foreground_mean={m:float(np.mean([r[m]['foreground_mae']for r in stage]))for m in ['legacy_control','public']},
    worst_frame={m:max(r[m]['foreground_mae']for r in stage)for m in ['legacy_control','public']},
    worst64={m:max(r[m]['worst64']for r in stage)for m in ['legacy_control','public']},
    rotation_peak=dict(legacy_control=max(abs(float(r[4])-float(r[5]))for r in stage_rows),public=max(abs(float(r[4])-float(r[6]))for r in stage_rows)))
report=dict(conditions=conditions,summary=summary,
    mask='Same source/before/after union of ALL nonbackground pixels, including low alpha. No registration, cutoff, phase fitting or changed oracle.',
    status='Fix follower rest semantics. Continuous first-rest is an explicit media-time adaptation; original RAF sampling/rotation and raster residuals remain, not complete theme acceptance.',frames=records,
    new_public_stage=dict(summary=stage_summary,frames=stage))
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['latin-380-calm-73000.png','latin-380-chaotic-70000.png','latin-380-normal-9000.png']
sheet=Image.new('RGB',(1140,len(names)*630),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,folder)in enumerate([('Original',source),('Previous dispatcher',before),('Follower rest',after)]):
        draw.text((j*380+4,i*630+3),label+' | '+name,fill='white')
        sheet.paste(Image.open(folder/name).convert('RGB'),(j*380,i*630+25))
sheet.save(out/'contact.png')
sheet=Image.new('RGB',(1140,3*630),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(['tied-normal-1100.png','tied-chaotic-1270.png','regular-chaotic-1300.png']):
    for j,(label,p)in enumerate([('Original wheel',stage_source/name),('Legacy rotation control',stage_native/('legacy-'+name)),('Public core follower',stage_native/name)]):
        draw.text((j*380+4,i*630+3),label+' | '+name,fill='white');sheet.paste(Image.open(p).convert('RGB'),(j*380,i*630+25))
sheet.save(out/'public-stage-contact.png')
print(json.dumps(summary,indent=2))
print(json.dumps(stage_summary,indent=2))
