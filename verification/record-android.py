"""Record the actual debug Activity surface and strictly verify paused PNGs.

Uses a monotonic media anchor in the Activity, not accumulated frame events.
The video is runtime evidence, not an FPS benchmark or timestamp-exact golden.
"""
from pathlib import Path
from PIL import Image, ImageChops
import argparse
import hashlib
import io
import json
import subprocess
import time
import uuid

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb',default='adb');parser.add_argument('--serial',required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
def adb(*command,binary=False):
    r=subprocess.run([args.adb,'-s',args.serial,*command],capture_output=True,check=True,timeout=30)
    return r.stdout if binary else r.stdout.decode('utf8').strip()
api=adb('shell','getprop','ro.build.version.sdk')
out=root/f'build/verification/classic-android-api{api}-recording';out.mkdir(parents=True,exist_ok=True)
def frame(ms,play=False):
    adb('logcat','-c')
    adb('shell','am','start','--activity-no-animation','--activity-single-top','-n',
        'top.ghhccghk.accompanist.folia.sample/.ClassicParityActivity','--es','scene','baseline',
        '--el','mediaMs',str(ms),'--ei','width','800','--ez','play',str(play).lower())
    deadline=time.monotonic()+15
    while True:
        logs=adb('logcat','-d','-s','FoliaParity:I','AndroidRuntime:E')
        if 'FATAL EXCEPTION' in logs:raise RuntimeError(logs)
        if f'READY baseline {ms} 800 false false' in logs:
            return logs
        if time.monotonic()>deadline:raise RuntimeError('No rendered media frame\n'+logs)
        time.sleep(.05)
def screenshot():
    return Image.open(io.BytesIO(adb('exec-out','screencap','-p',binary=True))).convert('RGB').crop((0,0,800,600))
frame(0)
deadline=time.monotonic()+15
while screenshot().getextrema()!=((22,22),(36,36),(53,53)):
    if time.monotonic()>deadline:raise RuntimeError('Starting surface did not settle')
path=f'/sdcard/folia-classic-verification-{uuid.uuid4().hex}.mp4'
record=subprocess.Popen([args.adb,'-s',args.serial,'shell','screenrecord','--time-limit','15','--bit-rate','6000000',path],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
try:
    deadline=time.monotonic()+5
    while subprocess.run([args.adb,'-s',args.serial,'shell','test','-f',path],capture_output=True,timeout=5).returncode!=0:
        if record.poll() is not None:raise RuntimeError(record.communicate()[1].decode('utf8',errors='replace'))
        if time.monotonic()>deadline:raise RuntimeError('Recorder did not create its output')
        time.sleep(.05)
    # screenrecord creates its file before its first encoded frame. This delay
    # only places empty lead-in in the recording; it does not drive media time.
    time.sleep(.4)
    start=time.monotonic()
    logs=[frame(0,True)]
    checks=[]
    for delay,ms in [(5.1,1500),(7.2,3700)]:
        while time.monotonic()-start<delay:time.sleep(.05)
        logs.append(frame(ms))
        a=screenshot();time.sleep(.4);b=screenshot()
        a.save(out/f'paused-{ms}ms.png');b.save(out/f'paused-{ms}ms-repeat.png')
        if ImageChops.difference(a,b).getbbox():raise RuntimeError(f'Paused device surface changed at {ms}ms')
        checks.append({'mediaMs':ms,'pausedRepeatIdentical':True,'sha256':hashlib.sha256(a.tobytes()).hexdigest()})
    stdout,stderr=record.communicate(timeout=15)
    if record.returncode:raise RuntimeError(stderr.decode('utf8',errors='replace'))
    adb('pull',path,str(out/'classic-play-pause-seek.mp4'))
    info={'sdk':api,'serial':args.serial,'fingerprint':adb('shell','getprop','ro.build.fingerprint'),
        'deviceScreen':adb('shell','wm','size'),'viewport':[800,600],'density':1,'fontScale':1,
        'clock':'Debug-only Activity: withFrameNanos absolute anchor; public FoliaLyricsView',
        'sequence':'Play from 0ms through both lines, paused reverse seek to1500ms, paused seek to3700ms',
        'video':'Actual screenrecord surface, requested15s, no audio; runtime observation only, not exact golden or performance result',
        'apkSha256':hashlib.sha256((root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk').read_bytes()).hexdigest(),
        'checks':checks,'deviceOutput':path,'recorderStderr':stderr.decode('utf8',errors='replace')}
    (out/'conditions.json').write_text(json.dumps(info,indent=2),encoding='utf8')
    (out/'clock-log.txt').write_text('\n'.join(logs),encoding='utf8')
    print(out)
finally:
    if record.poll() is None:
        record.terminate();record.communicate(timeout=5)
