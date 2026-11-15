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
from PIL import Image, ImageChops, ImageStat

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb',default='adb');parser.add_argument('--serial',required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
browser_path=root/'build/verification/font-advances-browser/conditions.json'
browser=json.loads(browser_path.read_text(encoding='utf8'))
def adb(*command,binary=False):
    return subprocess.run([args.adb,'-s',args.serial,*command],capture_output=True,text=not binary,check=True,timeout=30).stdout
api=adb('shell','getprop','ro.build.version.sdk').strip()
out=root/f'build/verification/font-advances-api{api}';out.mkdir(parents=True,exist_ok=True)
adb('logcat','-c')
adb('shell','am','start','--activity-no-animation','--activity-single-top','-n',
    'top.ghhccghk.accompanist.folia.sample/.ClassicParityActivity','--es','scene','baseline',
    '--el','mediaMs','0','--ez','metricsProbe','true')
deadline=time.monotonic()+15
while True:
    log=adb('logcat','-d','-s','FoliaMetrics:I','FoliaRaster:I','AndroidRuntime:E')
    if 'FATAL EXCEPTION' in log:raise RuntimeError(log)
    if 'RASTER_DONE' in log:break
    if time.monotonic()>deadline:raise RuntimeError('Font probe did not finish\n'+log)
    time.sleep(.05)
rows=[json.loads(line[line.index('{'):]) for line in log.splitlines() if '{' in line and 'FoliaMetrics' in line]
compose=[json.loads(line[line.index('{'):]) for line in log.splitlines() if '{' in line and 'FoliaRaster' in line]
assert len(rows)==20,'Every native size/mode must be present'
assert len(compose)==5,'Every Compose size must be present'
(out/'native-log.txt').write_text(log,encoding='utf8')
font=root/'verification/assets/FoliaParityLatin-Bold.ttf'
with TTFont(font) as f:
    cmap=f.getBestCmap();units=f['head'].unitsPerEm;metrics=f['hmtx'].metrics
    details=[]
    rasters=[]
    for row in rows:
        reference=next(x for x in browser['rows'] if abs(x['size']-row['size'])<.00001)
        assert reference['text']==row['text']
        name=row['raster'];assert '/' not in name and '\\' not in name
        (out/name).write_bytes(adb('exec-out','run-as','top.ghhccghk.accompanist.folia.sample','cat','files/'+name,binary=True))
        a=Image.open(browser_path.parent/reference['raster']).getchannel('A')
        b=Image.open(out/name).getchannel('A')
        assert a.size==b.size==(512,320)
        union=ImageChops.lighter(a,b).point(lambda x:255 if x else 0)
        rasters.append((row['mode'],row['size'],sum(a.getdata())/255,sum(b.getdata())/255,
            ImageStat.Stat(ImageChops.difference(a,b),union).mean[0],str(a.getbbox()),str(b.getbbox())))
        for char,advance,original in zip(row['text'],row['advances'],reference['advances'],strict=True):
            ideal=metrics[cmap[ord(char)]][0]/units*row['size']
            details.append((row['mode'],row['size'],char,ideal,advance,advance-ideal,original,advance-original))
with (out/'advances.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['mode','size','char','font_units_scaled','native_advance','font_units_delta',
        'browser_advance','browser_delta']);w.writerows(details)
with (out/'raster-errors.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['mode','size','browser_alpha_area','native_alpha_area','foreground_alpha_mae',
        'browser_bounds','native_bounds']);w.writerows(rasters)
compose_errors=[]
for row in compose:
    name=row['raster'];assert '/' not in name and '\\' not in name
    (out/name).write_bytes(adb('exec-out','run-as','top.ghhccghk.accompanist.folia.sample','cat','files/'+name,binary=True))
    reference=next(x for x in browser['rows'] if abs(x['size']-row['size'])<.00001)
    native=next(x for x in rows if x['mode']=='linear-no-hint' and x['size']==row['size'])
    a=Image.open(browser_path.parent/reference['raster']).getchannel('A')
    b=Image.open(out/name).getchannel('A');c=Image.open(out/native['raster']).getchannel('A')
    assert a.size==b.size==c.size==(512,320)
    union=ImageChops.lighter(a,b).point(lambda x:255 if x else 0)
    compose_errors.append((row['size'],row['advance'],sum(b.getdata())/255,
        ImageStat.Stat(ImageChops.difference(a,b),union).mean[0],ImageChops.difference(b,c).getextrema()[1]))
with (out/'compose-raster-errors.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['size','compose_advance','compose_alpha_area','browser_alpha_mae','native_max_alpha_delta']);w.writerows(compose_errors)
(out/'conditions.json').write_text(json.dumps({'sdk':api,'serial':args.serial,
    'fingerprint':adb('shell','getprop','ro.build.fingerprint').strip(),
    'fontSha256':hashlib.sha256(font.read_bytes()).hexdigest(),
    'browserConditionsSha256':hashlib.sha256(browser_path.read_bytes()).hexdigest(),
    'apkSha256':hashlib.sha256((root/'sample/android-app/build/outputs/apk/debug/android-app-debug.apk').read_bytes()).hexdigest(),
    'note':'Native shaping diagnostics;64x is a metric control, never used to render production lyrics',
    'rows':rows,'compose':compose},indent=2),encoding='utf8')
print(out)
