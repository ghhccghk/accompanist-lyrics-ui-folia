"""Fail closed on any raster change in the preserved native stage matrices."""
import hashlib
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
before=json.loads((root/'.work/diorama-direct-raster-before.json').read_text())
groups={}
for group, files in before.items():
    actual={name:hashlib.sha256((root/'build/verification'/group/name).read_bytes()).hexdigest() for name in files}
    changed=[name for name in files if files[name]!=actual[name]]
    groups[group]=dict(preserved=len(files),changed=changed,hashes=actual)
    assert not changed, f'Unexpected raster change in {group}: {changed}'
coverage=[]
for line in (root/'build/verification/diorama-raster-window/coverage.tsv').read_text().splitlines():
    if line.startswith('#') or not line:continue
    r=line.split('\t')
    assert int(r[6])==0
    coverage.append(dict(case=r[0],advance=float(r[1]),units=int(r[2]),eliminatedFullAtlasBytes=int(r[3]),
                         retainedBaseAndHaloBytes=int(r[4]),widestUnit=int(r[5]),changedPixels=int(r[6])))
assert len(coverage)==2
report=dict(scope='Exact native hash preservation, not improvement in original-stage parity. Source tables/images/gates unchanged. Allocation bytes are RGBA dimensions, not measured PSS/heap or timing.',
            groups=groups,coverage=coverage,
            limits=['A single very wide/bidi-spanning unit can still require a large raster.',
                    'All-song retained textures and halo scratch are not bounded by this change.',
                    'No new Android device/real-window/performance claim.'])
out=root/'verification/diorama-raster-window';out.mkdir(exist_ok=True)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(groups={k:dict(preserved=v['preserved'],changed=len(v['changed'])) for k,v in groups.items()},coverage=coverage),indent=2))
