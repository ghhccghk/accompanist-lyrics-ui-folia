import csv,json,re,math,sys
from pathlib import Path
from collections import Counter
folder=Path(sys.argv[1])
meta=json.loads((folder/'metadata.json').read_text())
blocks=(folder/'samples.txt').read_text(errors='replace').split('\nsample:\n')[1:]
samples=[]
for b in blocks:
    stamp=re.search(r'^  time: (\d+)',b,re.M)
    tid=re.search(r'^  thread_id: (\d+)',b,re.M)
    if stamp and tid:samples.append((int(stamp[1]),int(tid[1]),b))
assert samples,'No samples'
t0=min(s[0] for s in samples);t1=max(s[0] for s in samples)
patterns=['SoftwareBlurKt.blurPremultiplied','ClassicLayer.record','GlowMaskKt.blurredMasks','GlowMaskKt.blurBoxCoverage','PartitaLineKt','ClassicLineKt','neon::bilerp','gc::','art::jit::','folia_mask_experiment']
counts={p:sum(p in s[2] for s in samples) for p in patterns}
frames={}
for file in folder.glob('gfx-*.txt'):
    header=None
    for line in file.read_text(errors='replace').splitlines():
        if line.startswith('Flags,'):header=line.rstrip(',').split(',');continue
        if header is None or not re.match(r'^\d+,',line):continue
        values=line.rstrip(',').split(',')
        if len(values)!=len(header):continue
        row=dict(zip(header,map(int,values)))
        a=row['IntendedVsync'];b=row['FrameCompleted']
        if t0<=a<b<=t1:frames[(a,b)]=row
ordered=sorted(frames.values(),key=lambda f:f['FrameCompleted']-f['IntendedVsync'],reverse=True)
ms=sorted((b-a)/1e6 for a,b in frames)
def percentile(q):return ms[max(0,math.ceil(q*len(ms))-1)] if ms else None
long=[]
for f in ordered[:12]:
    a=f['IntendedVsync'];b=f['FrameCompleted'];overlap=[s for s in samples if a<=s[0]<=b]
    long.append(dict(durationMs=(b-a)/1e6,offsetSeconds=(a-t0)/1e9,exportedSamples=len(overlap),inclusiveSamples={p:sum(p in s[2] for s in overlap) for p in patterns},flags=f['Flags']))
mem={}
for file in folder.glob('mem-*.txt'):
    match=re.search(r'TOTAL PSS:\s*(\d+)\s+TOTAL RSS:\s*(\d+)\s+TOTAL SWAP PSS:\s*(\d+)',file.read_text(errors='replace'))
    if match:mem[file.name]=dict(zip(['pssKiB','rssKiB','swapPssKiB'],map(int,match.groups())))
record=(folder/'record.log').read_text(errors='replace')
m=re.search(r'Samples recorded:\s*([\d,]+)\. Samples lost:\s*([\d,]+)',record)
assert m,record
assert (folder/'pid-after.txt').read_text().strip()==meta['pid'],'Process changed'
report=dict(installedApkSha256=meta['apkSha256'],event=meta['event'],frequencyHz=meta['frequency'],recordedSamples=int(m[1].replace(',','')),lostSamples=int(m[2].replace(',','')),exportedSamples=len(samples),firstToLastSampleSeconds=(t1-t0)/1e9,inclusiveSamplesNotAdditive=counts,inclusivePercent={p:round(n/len(samples)*100,3) for p,n in counts.items()},mainThreadPercent=round(sum(s[1]==int(meta['pid']) for s in samples)/len(samples)*100,3),retainedFrames=dict(count=len(ms),p50Ms=percentile(.5),p95Ms=percentile(.95),maxMs=max(ms) if ms else None,over100Ms=sum(t>100 for t in ms),rawFlags=dict(Counter(str(f['Flags']) for f in frames.values()))),memorySnapshots=mem,limitations='Sample overhead; incomplete deduplicated frame rings, fully inside first/last exported timestamps only. Inclusive stacks overlap. Not controlled A/B, session FPS, peak memory or proof of exclusive wall-time attribution. Theme inferred from stacks, quality/backend unrecorded.')
(folder/'analysis.json').write_text(json.dumps(report,indent=2)+'\n')
(folder/'long-frame-overlap.json').write_text(json.dumps(long,indent=2)+'\n')
print(json.dumps(report,indent=2))
print('Longest retained frames:',[(round(f['durationMs'],3),f['inclusiveSamples']) for f in long[:3]])
