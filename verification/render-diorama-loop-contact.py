"""Native real-core loop review artifact; deliberately not a source image comparison."""
from pathlib import Path
import hashlib
import json
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
native = root/'build/verification/diorama-loop'
out = root/'verification/diorama-loop'
result = ET.parse(root/'library/build/test-results/jvmTest/TEST-top.ghhccghk.accompanist.lyrics.folia.DioramaLoopTest.xml').getroot()
assert int(result.attrib['tests']) == 3 and int(result.attrib['failures']) == 0 and int(result.attrib['errors']) == 0
out.mkdir(parents=True, exist_ok=True)
times = [0, 700, 1600, 2099, 2100, 3199, 3200, 3201, 3500, 4500]
sheet = Image.new('RGB', (1200, 5*326), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
hashes = {}
for i, t in enumerate(times):
    path = native/f'core-loop-{t}.png'
    hashes[path.name] = hashlib.sha256(path.read_bytes()).hexdigest()
    x, y = (i % 3)*400, (i//3)*326
    draw.text((x+4, y+4), f'Native real core loop {t}ms', fill='white')
    im = Image.open(path).convert('RGB'); im.thumbnail((400, 300));sheet.paste(im, (x, y+24))
draw.text((4, 4*326+4), 'Native integration/lifecycle only. Original numeric camera/window/material controls are separate.', fill='white')
sheet.save(out/'native-contact.png')
report = dict(scope='Native real-core loop pictures, not original raster equality, real window or device acceptance.',
              source_finite=(native/'numeric.txt').read_text(), source_windows=(native/'windows.txt').read_text(),
              jvm_class=dict(tests=int(result.attrib['tests']), failures=int(result.attrib['failures'])),
              native_png_sha256=hashes)
(out/'validation.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
print(json.dumps(report, indent=2))
