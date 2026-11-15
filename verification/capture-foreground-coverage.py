"""Separate Android hardware paragraph coverage from cached software raster input."""
from pathlib import Path
import argparse, csv, hashlib, io, json, subprocess, time
from PIL import Image, ImageChops, ImageStat

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb',default='adb');p.add_argument('--serial',required=True)
args=p.parse_args()
root=Path(__file__).resolve().parents[1]
package='com.mocharealm.accompanist.folia.sample'
def adb(*cmd):
    return subprocess.run([args.adb,'-s',args.serial,*cmd],capture_output=True,check=True,timeout=30).stdout
api=adb('shell','getprop','ro.build.version.sdk').decode().strip()
out=root/f'build/verification/foreground-coverage-api{api}';out.mkdir(parents=True,exist_ok=True)
reference=root/'build/verification/foreground-coverage-browser'
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
    for cached in [False,True]:
        token+=1
        size=row['size'];dark=row['dark']
        adb('logcat','-c')
        adb('shell','am','start','--activity-no-animation','--activity-single-top','-n',package+'/.ForegroundCoverageActivity',
            '--ef','size',str(size),'--ez','cached',str(cached).lower(),'--ez','dark',str(dark).lower(),
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
        bg=(22,36,53) if dark else (0,0,0)
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
        mask=ImageChops.lighter(ImageChops.difference(source,background),ImageChops.difference(image,background)).convert('L').point(lambda a:255 if a else 0)
        error=ImageStat.Stat(ImageChops.difference(source,image),mask).mean
        name=f'{size}-{"dark" if dark else "black"}-{"cached" if cached else "direct"}.png'
        image.save(out/name)
        frames.append({'size':size,'dark':dark,'cached':cached,'file':name,'foreground_rgb_mae':sum(error)/3,
            'sha256':hashlib.sha256(image.tobytes()).hexdigest(),'pausedRepeatIdentical':True})
for row in frames:
    pair=next(other for other in frames if other['size']==row['size'] and other['dark']==row['dark'] and other['cached']!=row['cached'])
    row['direct_cached_max_delta']=max(v[1] for v in ImageChops.difference(Image.open(out/row['file']),Image.open(out/pair['file'])).getextrema())
apk=root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk'
(out/'conditions.json').write_text(json.dumps({'api':api,'serial':args.serial,'fingerprint':adb('shell','getprop','ro.build.fingerprint').decode().strip(),
    'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'browser_conditions_sha256':hashlib.sha256((reference/'conditions.json').read_bytes()).hexdigest(),
    'probe_sha256':hashlib.sha256((root/'sample/android-app/src/debug/kotlin/com/mocharealm/accompanist/folia/sample/ForegroundCoverageActivity.kt').read_bytes()).hexdigest(),
    'viewport':[512,320],'baseline':191,'left':128,'frames':frames,
    'limits':'Synthetic static intact paragraph control; does not certify classic transforms/glow or physical devices.'},indent=2),encoding='utf8')
with (out/'errors.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['size','background','path','foreground_rgb_mae','direct_cached_max_delta'])
    for r in frames:w.writerow([r['size'],'dark' if r['dark'] else 'black','cached' if r['cached'] else 'direct',r['foreground_rgb_mae'],r['direct_cached_max_delta']])
print(out)
