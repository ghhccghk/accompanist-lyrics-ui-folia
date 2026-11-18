"""Bounded raw-mip fragment oracle, never a production or visual-pass input."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
native=root/'build/verification/diorama-mounted-input-kotlin'
source=root/'build/verification/diorama-mounted-sampling-browser'
out=root/'verification/diorama-fragment-control';out.mkdir(exist_ok=True)
build=root/'build/verification/diorama-fragment-color-kotlin';build.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
h=np.loadtxt(root/'build/verification/diorama-mounted-geometry-kotlin/homography.tsv').reshape(3,3)
inv=np.linalg.inv(h)
levels={}
scene=json.loads((root/'verification/diorama-fixtures/mounted-mips-v1/scene.json').read_text(encoding='utf8'))
for level,w,height in [(4,103,14),(5,51,7)]:
    path=root/f'verification/diorama-fixtures/mounted-mips-v1/mip-{level}.rgba'
    assert sha(path)==next(m['sha256'] for m in scene['mips'] if m['level']==level)
    a=np.frombuffer(path.read_bytes(),dtype=np.uint8).reshape(height,w,4)/255.
    a[:,:,:3]=np.where(a[:,:,:3]<=.04045,a[:,:,:3]/12.92,((a[:,:,:3]+.055)/1.055)**2.4)
    levels[level]=a
bg=np.array([22,36,53])/255
primary=np.array([223,229,236])/255
primary=np.where(primary<=.04045,primary/12.92,((primary+.055)/1.055)**2.4)

def sample(a,u,v):
    height,w=a.shape[:2];xx=u/1650*w-.5;yy=v/232*height-.5
    ix=int(np.floor(xx));iy=int(np.floor(yy));fx=xx-ix;fy=yy-iy
    return sum(a[np.clip(iy+dy,0,height-1),np.clip(ix+dx,0,w-1)]*
               ((fx if dx else 1-fx)*(fy if dy else 1-fy)) for dy in [0,1] for dx in [0,1])

images={k:np.broadcast_to(np.array([22,36,53],dtype=np.uint8),(600,380,3)).copy()
        for k in ['cpu-source-mips','cpu-linear-color','cpu-alpha']}
probe_points={(200,420),(220,420),(240,420),(260,420)};probes=[];lods=[]
for y in range(410,432):
    for x in range(170,277):
        uv=inv@np.array([x+.5,y+.5,1]);u,v=uv[:2]/uv[2]
        if not (0<=u<1650 and 0<=v<232):continue
        a,b,z=h@np.array([u,v,1])
        dx=(h[0,:2]*z-a*h[2,:2])/z**2;dy=(h[1,:2]*z-b*h[2,:2])/z**2
        determinant=dx[0]*dy[1]-dx[1]*dy[0]
        lod=np.log2(max(np.hypot(dy[1],dy[0]),np.hypot(dx[1],dx[0]))/abs(determinant))
        assert 4<=lod<=5;lods.append(float(lod))
        tex=sample(levels[4],u,v)*(5-lod)+sample(levels[5],u,v)*(lod-4)
        c=primary*tex[:3]/.6
        q=np.array([[.59719,.35458,.04823],[.076,.90834,.01566],[.02840,.13383,.83777]])@c
        q=(q*(q+.0245786)-.000090537)/(q*(.983729*q+.4329510)+.238081)
        c=np.clip(np.array([[1.60475,-.53108,-.07367],[-.10208,1.10813,-.00605],[-.00327,-.07276,1.07602]])@q,0,1)
        rgb=np.where(c<=.0031308,c*12.92,1.055*c**(1/2.4)-.055)
        t=np.clip((z-12)/18,0,1);rgb=rgb+(bg-rgb)*t*t*(3-2*t);alpha=tex[3]*.3
        images['cpu-source-mips'][y,x]=np.clip(np.floor((rgb*alpha+bg*(1-alpha))*255+.5),0,255)
        images['cpu-linear-color'][y,x]=np.clip(np.floor(tex[0]*255+.5),0,255)
        images['cpu-alpha'][y,x]=np.clip(np.floor(tex[3]*255+.5),0,255)
        if(x,y)in probe_points:probes.append(dict(pixel=[x,y],uv=[float(u),float(v)],lod=float(lod),linearRgb=tex[:3].tolist(),alpha=float(tex[3]),acesLinear=c.tolist(),acesEncoded=np.where(c<=.0031308,c*12.92,1.055*c**(1/2.4)-.055).tolist()))
for name,a in images.items():Image.fromarray(a).save(build/(name+'.png'))
files={'original':source/'source-original.png','isotropic-diagnostic':source/'source-trilinear-1.png',
       'native-default':native/'source-neighbor-7.png','fragment':native/'source-neighbor-7-fragment.png',
       'cpu-source-mips':build/'cpu-source-mips.png','fragment-context-failed':out/'context-oetf-failed.png'}
sampler=json.loads((root/'verification/diorama-fixtures/mounted-sampling-v1/scene.json').read_text(encoding='utf8'))
for label,mode in [('original','original'),('isotropic-diagnostic','trilinear-1')]:
    assert sha(files[label])==next(s['sha256'] for s in sampler['samplingCaptures'] if s['mode']==mode)
frozen=json.loads((root/'verification/diorama-mounted-input/comparison.json').read_text(encoding='utf8'))
assert sha(files['native-default'])==frozen['neighbor7']['hashes'][str(files['native-default'].relative_to(root)).replace('\\','/')]
arrays={key:np.array(Image.open(path).convert('RGB'),dtype=np.int32) for key,path in files.items()}
mask=np.logical_or.reduce([np.any(a!=[22,36,53],axis=2) for a in arrays.values()])
rows={}
for key,a in arrays.items():
    ys,xs=np.where(np.any(a!=[22,36,53],axis=2))
    d=abs(arrays['original']-a);di=abs(arrays['isotropic-diagnostic']-a)
    rows[key]=dict(sha256=sha(files[key]),pixels=int(xs.size),positiveRgbMass=int(np.maximum(a-[22,36,53],0).sum()),
                   originalMae=float(d[mask].mean()),originalPeak=int(d.max()),isotropicMae=float(di[mask].mean()),isotropicPeak=int(di.max()),
                   worst64=max(float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean()) for y in range(0,600,64) for x in range(0,380,64) if mask[y:y+64,x:x+64].any()))
channel_probes=[]
for p in probes:
    x,y=p['pixel'];channel_probes.append(p|{'native-'+name:np.array(Image.open(native/f'fragment-{name}.png').convert('RGB'))[y,x].tolist() for name in ['linear-color','alpha','aces-linear','aces-encoded','primary']})
report=dict(baseline='e5d8e2d',sourceRevision=scene['revision'],conditions='Same380x600/DPR1/700ms actual source cache7, same native independent geometry/finite camera/material; scalar CPU source-mip input control uses real GPU mip4/5, not native output as expected.',
            scope='Isotropic mip0..5 JVM fragment probe only, no production use of GPU mip assets, original outputs never changed. Full shared-input image union including every low contribution. No pixel/phase shift.',
            unionPixels=int(mask.sum()),lodRange=[min(lods),max(lods)],measurements=rows,channelProbes=channel_probes,
            localIsotropicChannelGate=2,localIsotropicChannelGatePassed=rows['fragment']['isotropicPeak']<=2,
            interpretation='Only this isotropic shared-cache fragment control. Explicit source OETF fixes the preserved context-conversion failure. Original default aniso4 remains different; no production integration, full matrices or final visual acceptance.',
            productionChanged=False,deviceValidated=False,visualAcceptance=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(840,450),'#191919');draw=ImageDraw.Draw(sheet)
for i,(name,path)in enumerate(files.items()):
    x=(i%2)*420;y=(i//2)*150
    draw.text((x+5,y+3),name,fill='white')
    im=Image.open(path).convert('RGB').crop((170,410,277,432)).resize((321,66),Image.Resampling.NEAREST)
    sheet.paste(im,(x+5,y+25));draw.text((x+5,y+100),f"pixels={rows[name]['pixels']} mass={rows[name]['positiveRgbMass']}",fill='white')
sheet.save(out/'contact.png')
assert rows['fragment']['isotropicPeak']<=2,'Failed bounded isotropic shader control, not a formal visual gate'
print(json.dumps(dict(measurements=rows,channelProbes=channel_probes),indent=2))
