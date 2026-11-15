"""Exercise debug-only native audio probe on an explicitly named Android device."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb',default='adb')
p.add_argument('--serial',required=True)
p.add_argument('--attach-lyrics',action='store_true',help='Also verify in-place lyrics; capture finite intro waiting dots')
args=p.parse_args()
root=Path(__file__).resolve().parents[1]
package='com.mocharealm.accompanist.folia.sample'
def adb(*command):
    return subprocess.run([args.adb,'-s',args.serial,*command],check=True,capture_output=True,timeout=30).stdout
sdk=adb('shell','getprop','ro.build.version.sdk').decode().strip()
output=root/'build/verification/local-audio'/f'android{sdk}{"-attached" if args.attach_lyrics else ""}'
output.mkdir(parents=True,exist_ok=True)
wav=root/'.work/local-audio/synthetic.wav'
subprocess.run(['python',str(root/'verification/generate-local-audio.py'),str(wav)],check=True)
apk=root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk'
adb('install','-r',str(apk))
adb('push',str(wav),'/data/local/tmp/folia-local-audio.wav')
adb('shell','run-as',package,'mkdir','-p','files')
adb('shell','run-as',package,'cp','/data/local/tmp/folia-local-audio.wav','files/verification.wav')
adb('shell','am','force-stop',package)
adb('logcat','-c')
adb('shell','am','start','-n',package+'/.LocalAudioVerificationActivity','--ez','attachLyrics',str(args.attach_lyrics).lower())
deadline=time.monotonic()+25
while True:
    logs=adb('logcat','-d','-s','FoliaLocalAudio:I','AndroidRuntime:E').decode('utf8')
    if 'FAILED' in logs or 'FATAL EXCEPTION' in logs: raise RuntimeError(logs)
    if 'SUCCESS' in logs: break
    if time.monotonic()>deadline: raise RuntimeError('No native audio completion:\n'+logs)
    time.sleep(.2)
(output/'native.txt').write_bytes(adb('shell','run-as',package,'cat','files/local-audio-verification.txt'))
(output/'paused.png').write_bytes(adb('exec-out','screencap','-p'))
(output/'conditions.json').write_text(json.dumps({
    'serial':args.serial,'api':sdk,'fingerprint':adb('shell','getprop','ro.build.fingerprint').decode().strip(),
    'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'input_sha256':hashlib.sha256(wav.read_bytes()).hexdigest(),
    'source_sha256':{str(f.relative_to(root)):hashlib.sha256(f.read_bytes()).hexdigest() for f in (root/'sample').rglob('*.kt') if 'build' not in f.parts},
    'note':'Native MediaPlayer cursor, pause and seek. Generated source audio. No human listening or physical output loopback claim; system picker interaction is separate.'
},indent=2),encoding='utf8')
print(output)
