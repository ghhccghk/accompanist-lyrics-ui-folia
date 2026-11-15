"""Separate Android hardware paragraph coverage from cached software raster input."""
from pathlib import Path
import argparse, csv, hashlib, io, json, shlex, subprocess, time
from PIL import Image, ImageChops, ImageStat

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb',default='adb');p.add_argument('--serial',required=True)
p.add_argument('--colors',action='store_true',help='Same-origin multi-font/foreground/surface control, including white-mask tint')
args=p.parse_args()
root=Path(__file__).resolve().parents[1]
package='top.ghhccghk.accompanist.folia.sample'
def adb(*cmd):
    return subprocess.run([args.adb,'-s',args.serial,*cmd],capture_output=True,check=True,timeout=30).stdout
api=adb('shell','getprop','ro.build.version.sdk').decode().strip()
suffix='-colors' if args.colors else ''
out=root/f'build/verification/foreground-coverage{suffix}-api{api}';out.mkdir(parents=True,exist_ok=True)
reference=root/f'build/verification/foreground-coverage{suffix}-browser'
browser=json.loads((reference/'conditions.json').read_text(encoding='utf8'))
frames=[];token=int(time.time()*1000)
# Starting-window fade is outside the measured scene. Require two exact empty
# captures before the grid; still require strict paused equality for every glyph frame.
adb('shell','am','start','--activity-no-animation','--activity-single-top','-n',package+'/.ForegroundCoverageActivity',
    '--ez','blank','true','--ez','dark','false','--el','token',str(token))
deadline=time.monotonic()+15;empty=0
while empty<2:
    startup=Image.open(io.BytesIO(adb('exec-out','screencap','-p'))).convert('RGB').crop((0,0,512,320))
    empty=empty+1 if startup.getextrema()==((0,0),(0,0),(0,0)) else 0
    if time.monotonic()>deadline:
        startup.save(out/'startup-failed.png')
        raise RuntimeError('Empty hardware startup did not settle')
for row in browser['frames']:
    for mode in (['direct','cached','tinted'] if args.colors else ['direct','cached']):
        token+=1
        cached=mode!='direct'
        size=row['size'];dark=row.get('dark',False)
        ink=row.get('ink','#ffffff');surface=row.get('surface','#162435' if dark else '#000000')
        adb('logcat','-c')
        adb('shell','am','start','--activity-no-animation','--activity-single-top','-n',package+'/.ForegroundCoverageActivity',
            '--ef','size',str(size),'--ez','cached',str(cached).lower(),'--ez','dark',str(dark).lower(),
            '--es','font',row.get('font','latin'),'--es','ink',shlex.quote(ink),'--es','surface',shlex.quote(surface),
            '--ez','tinted',str(mode=='tinted').lower(),
            '--ez','blank','false','--el','token',str(token))
        deadline=time.monotonic()+15
        while True:
            log=adb('logcat','-d','-s','FoliaCoverage:I','AndroidRuntime:E').decode('utf8')
            if 'FATAL EXCEPTION' in log:raise RuntimeError(log)
            if any(line.rstrip().endswith(f'READY token={token}') for line in log.splitlines()):break
            if time.monotonic()>deadline:raise RuntimeError('No hardware frame: '+log)
            time.sleep(.1)
        def capture():return Image.open(io.BytesIO(adb('exec-out','screencap','-p'))).convert('RGB').crop((0,0,512,320))
        image=capture()
        bg=tuple(bytes.fromhex(surface[1:]))
        if image.getpixel((0,0))!=bg:
            image.save(out/'obscured.png')
            raise RuntimeError('System UI obscures hardware control; inspect obscured.png, do not waive')
        # Starting-window/compositor history can be stable in the first callback.
        # Equality is required rather than assuming frame completion means settled.
        second=capture()
        if ImageChops.difference(image,second).getbbox():
            image.save(out/'unstable-first.png');second.save(out/'unstable-second.png')
            raise RuntimeError('Paused hardware control changed')
        source=Image.open(reference/row['file']).convert('RGB')
        background=Image.new('RGB',image.size,bg)
        if not ImageChops.difference(source,background).getbbox() or not ImageChops.difference(image,background).getbbox():
            raise RuntimeError('Empty foreground control: '+row['file'])
        mask=ImageChops.lighter(ImageChops.difference(source,background),ImageChops.difference(image,background)).convert('L').point(lambda a:255 if a else 0)
        error=ImageStat.Stat(ImageChops.difference(source,image),mask).mean
        name=f'{Path(row["file"]).stem}-{mode}.png' if args.colors else f'{size}-{"dark" if dark else "black"}-{mode}.png'
        image.save(out/name)
        frames.append({'size':size,'dark':dark,'cached':cached,'mode':mode,'reference':row['file'],
            'font':row.get('font','latin'),'ink':ink,'surface':surface,'file':name,'foreground_rgb_mae':sum(error)/3,
            'sha256':hashlib.sha256(image.tobytes()).hexdigest(),'pausedRepeatIdentical':True})
for row in frames:
    pair=next(other for other in frames if other['reference']==row['reference'] and other['mode']==('cached' if row['mode']=='direct' else 'direct'))
    row['against_mode']=pair['mode']
    row['against_max_delta']=max(v[1] for v in ImageChops.difference(Image.open(out/row['file']),Image.open(out/pair['file'])).getextrema())
apk=root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk'
(out/'conditions.json').write_text(json.dumps({'api':api,'serial':args.serial,'fingerprint':adb('shell','getprop','ro.build.fingerprint').decode().strip(),
    'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'browser_conditions_sha256':hashlib.sha256((reference/'conditions.json').read_bytes()).hexdigest(),
    'probe_sha256':hashlib.sha256((root/'sample/android-app/src/debug/kotlin/top/ghhccghk/accompanist/folia/sample/ForegroundCoverageActivity.kt').read_bytes()).hexdigest(),
    'viewport':[512,320],'baseline':191,'left':128,'frames':frames,
    'limits':'Static intact paragraphs; color-matrix LTR host direction, no motion/glow. Does not certify classic transforms/glow or physical devices.'},indent=2),encoding='utf8')
with (out/'errors.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['size','font','ink','surface','path','foreground_rgb_mae','against_mode','against_max_delta'])
    for r in frames:w.writerow([r['size'],r['font'],r['ink'],r['surface'],r['mode'],r['foreground_rgb_mae'],r['against_mode'],r['against_max_delta']])
print(out)
