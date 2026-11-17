"""Reduced-motion target-flow repair, independently of normal Motion parity."""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
before=root/'.work/cappella-reduced-flow-aligned-before'
after=root/'build/verification/cappella-reduced-flow-kotlin'
out=root/'verification/cappella-reduced-flow';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
cases=[]
def rows(p):
    return [line.split('\t') for line in p.read_text(encoding='utf-8').splitlines() if line and not line.startswith('#')]
for p in sorted(after.glob('*.tsv')):
    old,new=rows(before/p.name),rows(p)
    assert [r[:2] for r in old]==[r[:2] for r in new]
    assert [r[3::2] for r in old]==[r[3::2] for r in new], 'Do not change expected target flow'
    def errors(data):
        return [dict(time=float(r[0]),message=r[1],field=field,error=abs(float(r[a])-float(r[b])))
                for r in data for field,a,b in [('top',2,3),('height',4,5)]]
    old_errors,new_errors=errors(old),errors(new)
    cases.append(dict(case=p.stem,rows=len(new),beforePeak=max(e['error'] for e in old_errors),
        afterPeak=max(e['error'] for e in new_errors),worstBefore=max(old_errors,key=lambda e:e['error']),
        geometryHashes=[sha(before/p.name),sha(p)],imageHashes=[sha(folder/(p.stem+'-1020.png')) for folder in (before,after)]))
    assert cases[-1]['afterPeak']<1e-9
normal_before=root/'.work/cappella-reduced-normal-before'
normal_after=root/'build/verification/cappella-presence-kotlin'
normal=[dict(file=p.name,before=sha(p),after=sha(normal_after/p.name)) for p in sorted(normal_before.iterdir()) if p.suffix=='.png' or p.name.endswith('-geometry.tsv')]
assert len(normal)==246 and all(r['before']==r['after'] for r in normal)
report=dict(scope='Same240 pressure probe times converted through real published core millisecond timing. Reduced-motion flow removes spatial spring/projection/margins and uses current source-derived target sizes. This is an accessibility adaptation, not original normal Motion equivalence. Expected target values unchanged. No timing shift or changed tolerance.',
    baseRevision='2d472d4',sourceRevision='cc6226e464050a456a2913e062374f23d3844cc5',
    cases=cases,normalUnchanged=normal)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
sheet=Image.new('RGB',(760,6*510),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,c in enumerate(cases):
    for j,folder in enumerate((before,after)):
        name=c['case']+'-1020.png';sheet.paste(Image.open(folder/name).convert('RGB'),(j*380,i*510+25))
        draw.text((j*380+3,i*510+3),('Before | ' if j==0 else 'After | ')+name,fill='white')
sheet.save(out/'contact.png')
print(json.dumps(dict(cases=cases,normalFramesUnchanged=240,normalTablesUnchanged=6),indent=2))
