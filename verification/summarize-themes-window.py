"""Read complete native-window artifacts after Gradle exits; no recapture/fitting."""
from pathlib import Path
import csv
import hashlib
import argparse
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output',default='verification/themes-native-window',help='Repository-relative report directory; preserve prior versioned evidence')
args=parser.parse_args()
source=root/'build/verification/themes-native-window'
out=root/args.output
assert (source/'SUCCESS').exists(), 'Native-window run has not succeeded'
rows=list(csv.DictReader((source/'checks.tsv').read_text(encoding='utf-8').splitlines(), delimiter='\t'))
themes=['Classic','Partita','Tilt','Cadenza','Pendolo','Cappella','Fume']
assert len(rows)==105
for theme in themes:
    selected={r['check']:r for r in rows if r['theme']==theme}
    assert len(selected)==15
    baseline=selected['baseline']['sha256']
    for label in ['reverse-seek','resize-restore','font-restore','settings-restore','offset','song-restore','remount','theme-revisit']:
        assert selected[label]['sha256']==baseline, (theme,label)
    for label in ['baseline','narrow','large-font','reduced']:
        p=source/(theme+'-'+label+'.png')
        assert hashlib.sha256(p.read_bytes()).hexdigest()==selected[label]['sha256'], p
out.mkdir(parents=True,exist_ok=True)
for name in ['checks.tsv','conditions.txt']:
    (out/name).write_text((source/name).read_text(encoding='utf-8'),encoding='utf-8')
sheet=Image.new('RGB',(1000,7*300),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,theme in enumerate(themes):
    for j,label in enumerate(['baseline','narrow','large-font']):
        draw.text((j*333+3,i*300+3),theme+' | '+label,fill='white')
        picture=Image.open(source/(theme+'-'+label+'.png')).convert('RGB');picture.thumbnail((327,270))
        sheet.paste(picture,(j*333,i*300+25))
sheet.save(out/'contact.png')
files=[p for folder in ['library/src/commonMain','library/src/jvmMain','sample/desktop-app/src/classicVerification'] for p in (root/folder).rglob('*.kt')]
files+=[root/'sample/desktop-app/build.gradle.kts',root/'gradle/libs.versions.toml']
(out/'source-sha256.tsv').write_text('path\tsha256\n'+'\n'.join(p.relative_to(root).as_posix()+'\t'+hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files))+'\n',encoding='utf-8')
print('105 native-window checks; seven themes; manifest/image hashes verified. Not upstream visual/audio/device acceptance.')
