"""Validate recorded Android waiting captures (run capture-android.py first).

This checks exact offset equivalence and blank outro; source phase equivalence
and finite/multivoice selection are tested separately in the library tests.
"""
from pathlib import Path
import json
from PIL import Image, ImageChops

root=Path(__file__).resolve().parents[1]/'build/verification'
normal=root/'classic-android-api34-waiting-confirmed'
offset=root/'classic-android-api34-waiting-offset'
reduced=root/'classic-android-api34-waiting-reduced'
records=[]
for folder in (normal,offset,reduced):
    conditions=json.loads((folder/'conditions.json').read_text(encoding='utf8'))
    assert conditions['sdk']=='34'
    assert all(row['pausedRepeatIdentical'] for row in conditions['frames'])
    records.append({'directory':folder.name,'requests':len(conditions['frames']),
                    'reverse_revisits':sum(row['seekRevisit'] for row in conditions['frames'])})
for width in (380,800):
    for time in (1500,5000,18000,29000):
        a=Image.open(normal/f'waiting-{width}-default-{time}ms.png').convert('RGB')
        b=Image.open(offset/f'waiting-offset-{width}-default-{time+500}ms.png').convert('RGB')
        assert ImageChops.difference(a,b).getbbox() is None, (width,time,'offset')
        assert len(a.getcolors(a.width*a.height))>1, (width,time,'missing dots')
    end=Image.open(normal/f'waiting-{width}-default-40000ms.png').convert('RGB')
    assert end.getextrema()==((22,22),(36,36),(53,53)), 'No invented outro dots'
report=root/'waiting-android-summary.json'
report.write_text(json.dumps({'captures':records,'offset_comparisons':8,
    'offset_pixel_error':0,'outro_blank':True,
    'limits':'API34 emulator; fixed media snapshots; no physical-device/performance claim.'},indent=2),encoding='utf8')
print(report)
