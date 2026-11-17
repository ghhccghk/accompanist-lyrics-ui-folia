"""Low-alpha original halo control: no threshold may hide the faint pixels."""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/pendolo-chorus-halo-browser'
native=root/'build/verification/pendolo-chorus-halo-kotlin'
out=root/'verification/pendolo-chorus-stage';out.mkdir(exist_ok=True)
frames=[];hashes={}
for file in sorted(source.glob('*.png')):
    other=native/file.name
    assert other.exists(),other
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (file,other)]
    assert a.shape==b.shape
    mask=np.any(a!=[22,36,53],axis=2)|np.any(b!=[22,36,53],axis=2)
    error=abs(a-b)
    frames.append(dict(file=file.name,pixels=int(mask.sum()),union_mae=float(error[mask].mean()) if mask.any() else 0,
        full_mae=float(error.mean()),peak_channel_error=int(error.max())))
    for p in (file,other):hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
assert len(frames)==168
report=dict(scope='Actual original halo with content group opacity:0 vs native halo-only seam; layout unchanged. Native uses observed source wheel rotation only to isolate this layer. No >8 threshold: all nonbackground pixels counted.',
    mean_union_mae=sum(f['union_mae'] for f in frames)/len(frames),peak_union_mae=max(f['union_mae'] for f in frames),
    peak_channel_error=max(f['peak_channel_error'] for f in frames),frames=frames,png_sha256=hashes)
software=[]
for file in sorted((root/'build/verification/pendolo-chorus-halo-software').glob('*.png')):
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (source/file.name,file)]
    mask=np.any(a!=[22,36,53],axis=2)|np.any(b!=[22,36,53],axis=2)
    error=abs(a-b)
    software.append(dict(file=file.name,union_mae=float(error[mask].mean()) if mask.any() else 0,peak_channel_error=int(error.max())))
    hashes[file.relative_to(root).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
assert len(software)==48
report['software']=dict(frames=software,mean_union_mae=sum(f['union_mae'] for f in software)/len(software),
    peak_union_mae=max(f['union_mae'] for f in software),peak_channel_error=max(f['peak_channel_error'] for f in software))
(out/'halo-comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['latin-380-calm-spaced-11000.png','latin-380-normal-spaced-11000.png','latin-380-chaotic-spaced-11000.png','cjk-800-normal-spaced-11000.png']
sheet=Image.new('RGB',(1000,len(names)*380),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate((source,native)):
        draw.text((j*500+4,i*380+2),f'{"Original" if j==0 else "Kotlin"} halo x8 from background | {name}',fill='white')
        a=np.asarray(Image.open(folder/name).convert('RGB')).astype(np.int16)
        pic=Image.fromarray(np.clip((a-[22,36,53])*8+[22,36,53],0,255).astype(np.uint8))
        pic.thumbnail((490,350));sheet.paste(pic,(j*500,i*380+23))
sheet.save(out/'halo-contact.png')
print(json.dumps({k:v for k,v in report.items() if k not in ('frames','png_sha256')},indent=2))
