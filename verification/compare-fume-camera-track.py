"""Report the rejected adaptive camera experiment; never use this as an acceptance gate."""
from pathlib import Path
import csv, hashlib, json, math
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
file=root/'build/verification/fume-camera-track/comparison.csv'
rows=[list(map(float,r))for r in csv.reader(l for l in file.read_text(encoding='utf-8').splitlines()if not l.startswith('#'))]
out=root/'verification/fume-camera-track';out.mkdir(exist_ok=True)
scenarios=[]
for scenario in range(4):
    group=[r for r in rows if int(r[0])==scenario]
    scenarios.append(dict(scenario=scenario,stepped=bool(scenario&1),static=bool(scenario&2),nodes=int(group[0][-1]),
        max_native_adapted_reference_error=max(max(abs(r[2]-r[3]),abs(r[4]-r[5]),abs(r[6]-r[7]))for r in group),
        max_original_60hz_world_distance=max(math.hypot(r[8]-r[3],r[9]-r[5])for r in group),
        max_original_60hz_scale_error=max(abs(r[10]-r[7])for r in group)))
report=dict(reference='cc6226e464050a456a2913e062374f23d3844cc5',
    conditions='synthetic measured camera geometry; 800x600; media seconds 0..8; float origin9.25; smooth/stepped x static/float; local trial error .02px',
    reference_adapted='untouched original stepFumeCamera run by an explicitly adapted media-time driver; this driver is not the original RAF behavior',
    original_60hz='original operator at explicit 60Hz endpoint samples; same geometry, epochs, speed, intensity and float phase',
    status='REJECTED for runtime integration: source60 trajectory residual and preparation-node cost remain unacceptable; test-only experiment, not in published library',
    scenarios=scenarios,source_fixture_sha256=hashlib.sha256((root/'verification/fume-fixtures/camera-track.tsv').read_bytes()).hexdigest(),
    native_report_sha256=hashlib.sha256(file.read_bytes()).hexdigest(),
    next='derive an efficient media-time spring response and calibrate against the actual finite-step source; keep deterministic pause/seek and do not reduce visual criteria')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
pic=Image.new('RGB',(1100,760),'white');draw=ImageDraw.Draw(pic)
group=[r for r in rows if int(r[0])==0]
draw.text((65,18),'Fume camera: original finite-step response vs REJECTED adaptive experiment',fill='black')
for row,(title,series)in enumerate([
    ('Camera Y (world px)',[(5,(40,130,210),'Adaptive media'),(9,(220,105,40),'Original60Hz')]),
    ('Camera scale',[(7,(40,130,210),'Adaptive media'),(10,(220,105,40),'Original60Hz')]),
    ('Position distance from original60Hz (world px)',[(None,(165,45,80),'Distance')])]):
    top=65+row*220;left=80;right=1065;bottom=top+165
    values=[[math.hypot(r[8]-r[3],r[9]-r[5])if index is None else r[index]for r in group]for index,_,_ in series]
    low=min(min(v)for v in values);high=max(max(v)for v in values);span=max(high-low,.001)
    draw.text((left,top-20),title,fill='black')
    draw.line((left,top,left,bottom,right,bottom),fill=(140,140,140))
    for i in range(5):
        y=top+(bottom-top)*i/4;draw.line((left,y,right,y),fill=(230,230,230));draw.text((5,y-5),f'{high-span*i/4:.2f}',fill='black')
    for vals,(_,color,label)in zip(values,series):
        points=[(left+r[1]/8*(right-left),bottom-(v-low)/span*(bottom-top))for r,v in zip(group,vals)]
        draw.line(points,fill=color,width=2)
        draw.text((right-230,top+10+series.index(next(s for s in series if s[2]==label))*15),label,fill=color)
    for t in range(9):draw.text((left+t/8*(right-left),bottom+5),str(t),fill='black')
draw.text((80,738),'Media time (seconds). Trajectory chart only; not a screenshot or completed Fume theme.',fill='black')
pic.save(out/'trajectories.png')
print(json.dumps(report,indent=2))
