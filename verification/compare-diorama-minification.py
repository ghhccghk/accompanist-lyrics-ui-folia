"""Frozen original72 views, same-union before/after explicit-mip candidate."""
from pathlib import Path
import hashlib
import json
import argparse
import shutil
import subprocess
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
before=root/'.work/diorama-mip-before-c0e2804'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--save-before',action='store_true')
args=parser.parse_args()
if args.save_before:
    path='library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/styles/diorama/DioramaPlainText.kt'
    assert (root/path).read_text(encoding='utf8')==subprocess.check_output(['git','show','c0e2804:'+path],cwd=root).decode('utf8').replace('\r\n','\n')
    if before.exists():raise SystemExit('Preserve existing baseline; refusing overwrite')
    groups={'mounted':('build/verification/diorama-mounted-kotlin',144),'plain':('build/verification/diorama-text-plane-kotlin',24)}
    paths={k:sorted((root/p).glob('*.png')) for k,(p,n) in groups.items()}
    assert all(len(paths[k])==n for k,(p,n) in groups.items())
    manifest={}
    for group,items in paths.items():
        target=before/group;target.mkdir(parents=True)
        for p in items:
            shutil.copy2(p,target/p.name);manifest[group+'/'+p.name]=hashlib.sha256(p.read_bytes()).hexdigest()
    (before/'hashes.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
    print('Saved baseline; now apply rejected candidate patch and run captures')
    raise SystemExit(0)
saved=json.loads((before/'hashes.json').read_text(encoding='utf8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert all(sha(before/name)==value for name,value in saved.items())
mounted=json.loads((root/'verification/diorama-fixtures/mounted-cut-v1/scene.json').read_text(encoding='utf8'))
plain=json.loads((root/'verification/diorama-fixtures/text-planes.json').read_text(encoding='utf8'))
bg=np.array([22,36,53]); modes=['finite','material','media']

def compare(files,labels):
    a=[np.array(Image.open(p).convert('RGB'),dtype=np.int32) for p in files]
    mask=np.logical_or.reduce([np.any(im!=bg,axis=2) for im in a]);result={}
    for label,im in zip(labels,a[1:]):
        d=abs(a[0]-im)
        local=[float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
               for y in range(0,im.shape[0],64) for x in range(0,im.shape[1],64) if mask[y:y+64,x:x+64].any()]
        ys,xs=np.where(np.any(im!=bg,axis=2))
        result[label]=dict(foregroundMae=float(d[mask].mean()) if mask.any() else 0,
                           worst64=max(local,default=0),peak=int(d.max()),pixels=int(xs.size),
                           bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None)
    return dict(unionPixels=int(mask.sum()),metrics=result,hashes={str(p.relative_to(root)).replace('\\','/'):sha(p) for p in files})

mounted_rows=[]
source_hashes=json.loads((root/'.work/diorama-coverage-before-c3e464c/hashes.json').read_text(encoding='utf8'))
for row in mounted['captures']:
    name=row['file'];source=root/'build/verification/diorama-mounted-browser'/name
    # Frozen original hashes come from the unmodified independently retained
    # pre-coverage manifest, not a newly generated candidate report.
    assert sha(source)==source_hashes['mounted-source/'+name]
    files=[source];labels=[]
    for mode in modes:
        files += [before/'mounted'/(mode+'-'+name),root/'build/verification/diorama-mounted-kotlin'/(mode+'-'+name)]
        labels += [mode+'-before',mode+'-after']
    mounted_rows.append(dict(file=name,**compare(files,labels)))
plain_rows=[]
for row in plain['observations']:
    name=row['file'];source=root/'build/verification/diorama-text-plane-browser'/name
    assert sha(source)==source_hashes['plain-source/'+name]
    plain_rows.append(dict(file=name,**compare([source,before/'plain'/name,root/'build/verification/diorama-text-plane-kotlin'/name],['before','after'])))

def summary(rows,before,after):
    return {key:dict(meanMae=float(np.mean([r['metrics'][key]['foregroundMae'] for r in rows])),
                     worst64=max(r['metrics'][key]['worst64'] for r in rows)) for key in [before,after]} | dict(
        improved=sum(r['metrics'][after]['foregroundMae']<r['metrics'][before]['foregroundMae'] for r in rows),
        unchanged=sum(r['metrics'][after]['foregroundMae']==r['metrics'][before]['foregroundMae'] for r in rows),
        regressions=[dict(file=r['file'],before=r['metrics'][before],after=r['metrics'][after]) for r in rows
                     if r['metrics'][after]['foregroundMae']>r['metrics'][before]['foregroundMae']],
        localRegressions=[dict(file=r['file'],before=r['metrics'][before]['worst64'],after=r['metrics'][after]['worst64']) for r in rows
                          if r['metrics'][after]['worst64']>r['metrics'][before]['worst64']])

source_paths=['library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/styles/diorama/'+p for p in ['DioramaPlainText.kt','DioramaMipPyramid.kt']]
candidate_hashes={p:sha(root/p) for p in source_paths}
prior=root/'verification/diorama-minification/comparison.json'
if prior.exists():
    record=json.loads(prior.read_text(encoding='utf8'))
    if 'candidateSourceHashes' in record:assert record['candidateSourceHashes']==candidate_hashes,'Restore the preserved candidate, do not overwrite its failure report with other production output'
report=dict(baseline='c0e2804',sourceRevision=mounted['revision'],candidateSourceHashes=candidate_hashes,
            mask='Original plus before/after ALL modes in fixed per-frame nonbackground union; no registration, alpha deletion, time shift or omitted image.',
            conditions='Frozen original48 mounted +24 Latin/Arabic/CJK/long plain, same core, font, dimensions, clock, camera and material policies.',
            mounted={mode:summary(mounted_rows,mode+'-before',mode+'-after') for mode in modes},
            plain=summary(plain_rows,'before','after'),mountedRows=mounted_rows,plainRows=plain_rows,
            decision='REJECT and reverse candidate: near-view Latin/Arabic blur is visibly worse;7 plain mean and6 local regressions, despite improved aggregate. Isotropic center-LOD/premultiplied mask is not original aniso4/straight-texture color sampling. Preserve the full comparison, do not adopt it as final or weakened acceptance.',
            visualAcceptance=False,deviceValidated=False)
out=root/'verification/diorama-minification';out.mkdir(exist_ok=True)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
# Fixed cases include the mounted regressions and near-plane Latin/Arabic
# regressions, not just the strongest improvements. Same scale in each row;
# these thumbnails are visual aids and NEVER metric inputs.
cases=[('plain','latin-800--3.6-1.9.png'),('plain','arabic-801--3.6-1.9.png'),
       ('plain','long-800-1.7-0.8.png'),('mounted','material-mounted-800-60-2100.png'),
       ('mounted','material-mounted-380-60-700.png'),('mounted','media-mounted-380-60-3200.png')]
sheet=Image.new('RGB',(1200,6*325),'#191919');draw=ImageDraw.Draw(sheet)
for i,(group,name) in enumerate(cases):
    source=(root/'build/verification/diorama-text-plane-browser'/name if group=='plain' else
            root/'build/verification/diorama-mounted-browser'/name.removeprefix('material-').removeprefix('media-'))
    current=(root/'build/verification/diorama-text-plane-kotlin'/name if group=='plain' else root/'build/verification/diorama-mounted-kotlin'/name)
    for j,(label,p)in enumerate(zip(['source','before','candidate'],[source,before/group/name,current])):
        im=Image.open(p).convert('RGB');im.thumbnail((400,300),Image.Resampling.LANCZOS)
        draw.text((400*j+4,325*i+2),label+' '+name,fill='white');sheet.paste(im,(400*j,325*i+23))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ['mounted','plain']},indent=2))
