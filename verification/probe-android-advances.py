"""Compare actual Android shaped advances with fixed font hmtx units.

Diagnostic Synthetic text has no kerning in the original browser probe. Font
tables are a control here, not a shaping implementation or replacement golden.
Requires the existing verification fontTools dependency and installed debug APK.
"""
from pathlib import Path
import argparse
import csv
import hashlib
import json
import subprocess
import time
from fontTools.ttLib import TTFont

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb',default='adb');parser.add_argument('--serial',required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
browser_path=root/'build/verification/font-advances-browser/conditions.json'
browser=json.loads(browser_path.read_text(encoding='utf8'))
def adb(*command):
    return subprocess.run([args.adb,'-s',args.serial,*command],capture_output=True,text=True,check=True,timeout=30).stdout
api=adb('shell','getprop','ro.build.version.sdk').strip()
out=root/f'build/verification/font-advances-api{api}';out.mkdir(parents=True,exist_ok=True)
adb('logcat','-c')
adb('shell','am','start','--activity-no-animation','--activity-single-top','-n',
    'com.mocharealm.accompanist.folia.sample/.ClassicParityActivity','--es','scene','baseline',
    '--el','mediaMs','0','--ez','metricsProbe','true')
deadline=time.monotonic()+15
while True:
    log=adb('logcat','-d','-s','FoliaMetrics:I','AndroidRuntime:E')
    if 'FATAL EXCEPTION' in log:raise RuntimeError(log)
    if 'DONE' in log:break
    if time.monotonic()>deadline:raise RuntimeError('Font probe did not finish\n'+log)
    time.sleep(.05)
rows=[json.loads(line[line.index('{'):]) for line in log.splitlines() if '{' in line]
assert len(rows)==20,'Every native size/mode must be present'
(out/'native-log.txt').write_text(log,encoding='utf8')
font=root/'verification/assets/FoliaParityLatin-Bold.ttf'
with TTFont(font) as f:
    cmap=f.getBestCmap();units=f['head'].unitsPerEm;metrics=f['hmtx'].metrics
    details=[]
    for row in rows:
        reference=next(x for x in browser['rows'] if abs(x['size']-row['size'])<.00001)
        assert reference['text']==row['text']
        for char,advance,original in zip(row['text'],row['advances'],reference['advances'],strict=True):
            ideal=metrics[cmap[ord(char)]][0]/units*row['size']
            details.append((row['mode'],row['size'],char,ideal,advance,advance-ideal,original,advance-original))
with (out/'advances.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['mode','size','char','font_units_scaled','native_advance','font_units_delta',
        'browser_advance','browser_delta']);w.writerows(details)
(out/'conditions.json').write_text(json.dumps({'sdk':api,'serial':args.serial,
    'fingerprint':adb('shell','getprop','ro.build.fingerprint').strip(),
    'fontSha256':hashlib.sha256(font.read_bytes()).hexdigest(),
    'browserConditionsSha256':hashlib.sha256(browser_path.read_bytes()).hexdigest(),
    'apkSha256':hashlib.sha256((root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk').read_bytes()).hexdigest(),
    'note':'Native shaping diagnostics;64x is a metric control, never used to render production lyrics',
    'rows':rows},indent=2),encoding='utf8')
print(out)
