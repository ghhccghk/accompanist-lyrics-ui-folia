"""Capture the debug-only public FoliaLyricsView at fixed physical-pixel conditions.

Install the debug APK first. Does not alter device density, fonts, or system settings.
"""
from pathlib import Path
import argparse
import hashlib
import io
import json
import subprocess
import time
from PIL import Image, ImageChops

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial',required=True)
parser.add_argument('--adb',default='adb')
parser.add_argument('--label',default='',help='Keep separate evidence directories, e.g. linear')
parser.add_argument('--verify-seek',action='store_true',help='Revisit baseline frames in reverse order and require identical pixels')
parser.add_argument('--scenes',nargs='+',default=['baseline','arabic','wrap','group','cluster','fast','instant','cjk','whitespace','subtitle'])
parser.add_argument('--intensity',choices=['calm','normal','chaotic'],default='normal')
parser.add_argument('--height',type=int,default=600,help='Component viewport height in physical pixels (debug fixture only)')
parser.add_argument('--times',nargs='+',type=int,help='Explicit media checkpoints; keeps original default schedules unchanged')
args=parser.parse_args()
if not 100<=args.height<=1280:parser.error('Height must be in 100..1280')
if args.label and not all(c.isascii() and (c.isalnum() or c=='-') for c in args.label):parser.error('Invalid evidence label')
root=Path(__file__).resolve().parents[1]
def adb(*command,binary=False):
    result=subprocess.run([args.adb,'-s',args.serial,*command],capture_output=True,check=True,timeout=30)
    return result.stdout if binary else result.stdout.decode('utf8').strip()
sdk=adb('shell','getprop','ro.build.version.sdk')
output=root/'build/verification'/f'classic-android-api{sdk}{"-"+args.label if args.label else ""}'
output.mkdir(parents=True,exist_ok=True)
conditions={'serial':args.serial,'sdk':sdk,'device':adb('shell','getprop','ro.product.model'),
    'immersiveModeConfirmations':adb('shell','settings','get','secure','immersive_mode_confirmations'),
    'screen':adb('shell','wm','size'),'systemDensity':adb('shell','wm','density'),
    'egl':adb('shell','getprop','ro.hardware.egl'),'composeDensity':1,'fontScale':1,
    'buildFingerprint':adb('shell','getprop','ro.build.fingerprint'),
    'viewportPlacement':'top-left 0,0; physical pixels; system bars hidden',
    'revision':subprocess.run(['git','rev-parse','HEAD'],cwd=root,capture_output=True,text=True,check=True).stdout.strip(),
    # A capture can precede its local commit; HEAD alone would misidentify the APK.
    'rendererSourceSha256':{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
        for source_set in ['commonMain','androidMain']
        for p in sorted((root/'library/src'/source_set).rglob('*.kt'))},
    'fonts':{name:hashlib.sha256((root/'verification/assets'/name).read_bytes()).hexdigest()
        for name in ['FoliaParityLatin-Bold.ttf','FoliaParityCJK-Bold.otf','NotoSansArabic-Bold.ttf','FoliaParityBidi-Bold.ttf']},
    'apkSha256':hashlib.sha256((root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk').read_bytes()).hexdigest(),
    'activitySha256':hashlib.sha256((root/'sample/android-app/src/debug/kotlin/com/mocharealm/accompanist/folia/sample/ClassicParityActivity.kt').read_bytes()).hexdigest(),
    'animationIntensity':args.intensity,'viewportHeight':args.height,'frames':[]}
adb('shell','am','force-stop','com.mocharealm.accompanist.folia.sample')
# Android's starting-window/splash compositor is independent of our media clock
# and frame-commit callback. Settle that window at a known empty scene first;
# every actual checkpoint below still requires an unchanged second capture.
adb('shell','am','start','--activity-no-animation','-n','com.mocharealm.accompanist.folia.sample/.ClassicParityActivity',
    '--es','scene','baseline','--el','mediaMs','0','--ei','width','800','--ei','height',str(args.height))
deadline=time.monotonic()+15
blank_captures=0
while blank_captures<2:
    startup=Image.open(io.BytesIO(adb('exec-out','screencap','-p',binary=True))).convert('RGB').crop((0,0,800,args.height))
    blank_captures=blank_captures+1 if startup.getextrema()==((22,22),(36,36),(53,53)) else 0
    if time.monotonic()>deadline:
        startup.save(output/'startup-failed.png')
        (output/'startup-failed-conditions.json').write_text(json.dumps(conditions,ensure_ascii=False,indent=2),encoding='utf8')
        raise RuntimeError('Starting window did not settle to the exact empty scene; diagnostic PNG/conditions saved')
conditions['startup']='Two exact empty-scene captures before media checkpoints; no device animation settings changed'
observed={}
request_token=0
for scene in args.scenes:
    widths=[800,380] if scene in ['subtitle','interlude','interlude-single'] else [380 if scene=='wrap' else 600 if scene in ['group','cjk'] else 800]
    times=[1500,5500] if scene=='subtitle' else [750,800,1000,1200,1500,1900,2100,2800,3000] if scene!='baseline' else [750,1000,1500,2100,2300,3400,4400]
    if scene.startswith('interrupted-'):times=[731,761,791,821,851,881,941,1001,1041,1101,1241,1401,1701,2001]
    if scene=='dense':times=[801,901,1001,1051,1101,1201,1301]
    if scene in ['interlude','interlude-single']:times=[750,1000,1500,1900,2300,2500]
    if scene.startswith('bidi-'):times=[750,1000,1200,1500,1900,2100,2300,2500]
    if args.times:times=list(args.times)
    if args.verify_seek and args.times:times+=times[::-1]
    elif args.verify_seek and scene=='baseline':times += [1500,750,1500]
    if args.verify_seek and not args.times and (scene in ['dense','interlude','interlude-single'] or scene.startswith(('bidi-','interrupted-'))):times+=times[::-1]
    for width in widths:
        for mode in ([False] if scene=='dense' or scene.startswith('interrupted-') else [False,True]):
            for ms in times:
                request_token+=1
                adb('logcat','-c')
                adb('shell','am','start','--activity-no-animation','--activity-single-top','-n','com.mocharealm.accompanist.folia.sample/.ClassicParityActivity',
                    '--es','scene',scene,'--es','intensity',args.intensity,'--el','mediaMs',str(ms),'--ei','width',str(width),
                    '--ei','height',str(args.height),
                    '--ez','avoid',str(mode if scene!='subtitle' else False).lower(),
                    '--ez','backdrop',str(mode if scene=='subtitle' else False).lower(),
                    '--el','captureToken',str(request_token))
                ready=f'READY {scene} {ms} {width} {str(mode if scene!="subtitle" else False).lower()} {str(mode if scene=="subtitle" else False).lower()} token={request_token}'
                deadline=time.monotonic()+15
                while True:
                    logs=adb('logcat','-d','-s','FoliaParity:I','AndroidRuntime:E')
                    if 'FATAL EXCEPTION' in logs: raise RuntimeError(logs)
                    if any(line.rstrip().endswith(ready) for line in logs.splitlines()): break
                    if time.monotonic()>deadline: raise RuntimeError(f'No rendered frame: {ready}\n{logs}')
                    time.sleep(.05)
                raw=adb('exec-out','screencap','-p',binary=True)
                image=Image.open(io.BytesIO(raw)).convert('RGB').crop((0,0,width,args.height))
                # These fixed scenes leave at least one outer backdrop corner clear.
                # A stable system education/scrim overlay can pass pause/revisit
                # equality while invalidating the entire visual comparison.
                if not any(image.getpixel(point)==(22,36,53) for point in [(0,0),(width-1,0),(0,args.height-1),(width-1,args.height-1)]):
                    image.save(output/f'{scene}-{ms}ms-obscured.png')
                    (output/'obscured-conditions.json').write_text(json.dumps(conditions,ensure_ascii=False,indent=2),encoding='utf8')
                    raise RuntimeError(f'Fixed-scene backdrop corners obscured at {scene}/{ms}; inspect saved screenshot')
                label=('backdrop' if mode else 'transparent') if scene=='subtitle' else ('avoid' if mode else 'default')
                name=f'{scene}-{width}-{label}-{ms}ms.png'
                digest=hashlib.sha256(image.tobytes()).hexdigest()
                revisited=name in observed
                if revisited and observed[name]!=digest: raise RuntimeError(f'Seek did not reconstruct {name}')
                observed[name]=digest
                image.save(output/name)
                # The host passes paused media time. Verify a second actual device capture.
                second=Image.open(io.BytesIO(adb('exec-out','screencap','-p',binary=True))).convert('RGB').crop((0,0,width,args.height))
                if ImageChops.difference(image,second).getbbox() is not None:
                    second.save(output/name.replace('.png','-repeat.png'))
                    raise RuntimeError(f'Paused surface changed at {name}')
                conditions['frames'].append({'scene':scene,'width':width,'mode':label,'mediaMs':ms,'file':name,
                    'requestToken':request_token,'pausedRepeatIdentical':True,'seekRevisit':revisited,'sha256':digest})
                (output/'conditions.json').write_text(json.dumps(conditions,ensure_ascii=False,indent=2),encoding='utf8')
            print(f'API{sdk} {scene}/{width}/{label}: captured, paused repeat identical',flush=True)
(output/'conditions.json').write_text(json.dumps(conditions,ensure_ascii=False,indent=2),encoding='utf8')
print(output)
