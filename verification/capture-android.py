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
args=parser.parse_args()
if args.label and not all(c.isascii() and (c.isalnum() or c=='-') for c in args.label):parser.error('Invalid evidence label')
root=Path(__file__).resolve().parents[1]
def adb(*command,binary=False):
    result=subprocess.run([args.adb,'-s',args.serial,*command],capture_output=True,check=True,timeout=30)
    return result.stdout if binary else result.stdout.decode('utf8').strip()
sdk=adb('shell','getprop','ro.build.version.sdk')
output=root/'build/verification'/f'classic-android-api{sdk}{"-"+args.label if args.label else ""}'
output.mkdir(parents=True,exist_ok=True)
conditions={'serial':args.serial,'sdk':sdk,'device':adb('shell','getprop','ro.product.model'),
    'screen':adb('shell','wm','size'),'systemDensity':adb('shell','wm','density'),
    'egl':adb('shell','getprop','ro.hardware.egl'),'composeDensity':1,'fontScale':1,
    'buildFingerprint':adb('shell','getprop','ro.build.fingerprint'),
    'viewportPlacement':'top-left 0,0; physical pixels; system bars hidden',
    'revision':subprocess.run(['git','rev-parse','HEAD'],cwd=root,capture_output=True,text=True,check=True).stdout.strip(),
    'fonts':{name:hashlib.sha256((root/'verification/assets'/name).read_bytes()).hexdigest()
        for name in ['FoliaParityLatin-Bold.ttf','FoliaParityCJK-Bold.otf','NotoSansArabic-Bold.ttf']},
    'apkSha256':hashlib.sha256((root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk').read_bytes()).hexdigest(),
    'activitySha256':hashlib.sha256((root/'sample/android-app/src/debug/kotlin/com/mocharealm/accompanist/folia/sample/ClassicParityActivity.kt').read_bytes()).hexdigest(),
    'frames':[]}
adb('shell','am','force-stop','com.mocharealm.accompanist.folia.sample')
# Android's starting-window/splash compositor is independent of our media clock
# and frame-commit callback. Settle that window at a known empty scene first;
# every actual checkpoint below still requires an unchanged second capture.
adb('shell','am','start','--activity-no-animation','-n','com.mocharealm.accompanist.folia.sample/.ClassicParityActivity',
    '--es','scene','baseline','--el','mediaMs','0','--ei','width','800')
deadline=time.monotonic()+15
blank_captures=0
while blank_captures<2:
    startup=Image.open(io.BytesIO(adb('exec-out','screencap','-p',binary=True))).convert('RGB').crop((0,0,800,600))
    blank_captures=blank_captures+1 if startup.getextrema()==((22,22),(36,36),(53,53)) else 0
    if time.monotonic()>deadline:raise RuntimeError('Starting window did not settle to the exact empty scene')
conditions['startup']='Two exact empty-scene captures before media checkpoints; no device animation settings changed'
observed={}
for scene in args.scenes:
    widths=[800,380] if scene=='subtitle' else [380 if scene=='wrap' else 600 if scene in ['group','cjk'] else 800]
    times=[1500,5500] if scene=='subtitle' else [750,800,1000,1200,1500,1900,2100,2800,3000] if scene!='baseline' else [750,1000,1500,2100,2300,3400,4400]
    if args.verify_seek and scene=='baseline':times += [1500,750,1500]
    for width in widths:
        for mode in [False,True]:
            for ms in times:
                adb('logcat','-c')
                adb('shell','am','start','--activity-no-animation','--activity-single-top','-n','com.mocharealm.accompanist.folia.sample/.ClassicParityActivity',
                    '--es','scene',scene,'--el','mediaMs',str(ms),'--ei','width',str(width),
                    '--ez','avoid',str(mode if scene!='subtitle' else False).lower(),
                    '--ez','backdrop',str(mode if scene=='subtitle' else False).lower())
                ready=f'READY {scene} {ms} {width} {str(mode if scene!="subtitle" else False).lower()} {str(mode if scene=="subtitle" else False).lower()}'
                deadline=time.monotonic()+15
                while True:
                    logs=adb('logcat','-d','-s','FoliaParity:I','AndroidRuntime:E')
                    if 'FATAL EXCEPTION' in logs: raise RuntimeError(logs)
                    if ready in logs: break
                    if time.monotonic()>deadline: raise RuntimeError(f'No rendered frame: {ready}\n{logs}')
                    time.sleep(.05)
                raw=adb('exec-out','screencap','-p',binary=True)
                image=Image.open(io.BytesIO(raw)).convert('RGB').crop((0,0,width,600))
                label=('backdrop' if mode else 'transparent') if scene=='subtitle' else ('avoid' if mode else 'default')
                name=f'{scene}-{width}-{label}-{ms}ms.png'
                digest=hashlib.sha256(image.tobytes()).hexdigest()
                revisited=name in observed
                if revisited and observed[name]!=digest: raise RuntimeError(f'Seek did not reconstruct {name}')
                observed[name]=digest
                image.save(output/name)
                # The host passes paused media time. Verify a second actual device capture.
                second=Image.open(io.BytesIO(adb('exec-out','screencap','-p',binary=True))).convert('RGB').crop((0,0,width,600))
                if ImageChops.difference(image,second).getbbox() is not None:
                    second.save(output/name.replace('.png','-repeat.png'))
                    raise RuntimeError(f'Paused surface changed at {name}')
                conditions['frames'].append({'scene':scene,'width':width,'mode':label,'mediaMs':ms,'file':name,
                    'pausedRepeatIdentical':True,'seekRevisit':revisited,'sha256':digest})
                (output/'conditions.json').write_text(json.dumps(conditions,ensure_ascii=False,indent=2),encoding='utf8')
            print(f'API{sdk} {scene}/{width}/{label}: captured, paused repeat identical',flush=True)
(output/'conditions.json').write_text(json.dumps(conditions,ensure_ascii=False,indent=2),encoding='utf8')
print(output)
