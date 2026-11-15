"""Build a same-file mixed Arabic/Latin comparison face from pinned OFL inputs.

fontTools 4.55.3; verification only, never a library or release sample dependency.
Source attribution and input SHA-256 values: verification/assets/README.md.
"""
from pathlib import Path
import hashlib
import os
from io import BytesIO
from fontTools.merge import Merger
from fontTools import subset
from fontTools.ttLib import TTFont

assets = Path(__file__).resolve().parent / 'assets'
assert os.environ.get('PYTHONHASHSEED') == '0', 'Run with PYTHONHASHSEED=0 for deterministic table ordering'
inputs = {
    'NotoSans-Bold.ttf': 'c976e4b1b99edc88775377fcc21692ca4bfa46b6d6ca6522bfda505b28ff9d6a',
    'NotoSansArabic-Bold.ttf': 'ed2711b387750ae991b6a980b8dd16fdd65e6702b97e9f52adf3a8edf09ef4df',
}
for name, expected in inputs.items():
    assert hashlib.sha256((assets / name).read_bytes()).hexdigest() == expected, name
font = Merger().merge([str(assets / name) for name in inputs])
if 'STAT' in font: del font['STAT']
names = {1: 'Folia Parity Bidi', 2: 'Bold', 3: 'FoliaParityBidi-Bold-1',
         4: 'Folia Parity Bidi Bold', 6: 'FoliaParityBidi-Bold',
         16: 'Folia Parity Bidi', 17: 'Bold'}
for name_id, value in names.items():
    font['name'].removeNames(nameID=name_id)
    font['name'].setName(value, name_id, 3, 1, 0x409)
font['OS/2'].usWeightClass = 700
font['OS/2'].usWidthClass = 5
# Include ASCII controls for reusable static diagnostics, and the synthetic
# Arabic letters; subset keeps OpenType shaping closure and hinting.
options = subset.Options()
options.name_IDs = ['*']; options.name_legacy = True; options.name_languages = ['*']
subsetter = subset.Subsetter(options=options)
subsetter.populate(unicodes=list(range(32, 127)) + [ord(c) for c in 'مرحبالعالم'])
subsetter.subset(font)
assert set(map(ord, 'Glow مرحبا بالعالم2026!')).issubset(font.getBestCmap()), 'Comparison text must never use fallback'
font['head'].modified = 0
font['head'].created = TTFont(assets / 'NotoSans-Bold.ttf', recalcTimestamp=False)['head'].created
font.recalcTimestamp = False
output = assets / 'FoliaParityBidi-Bold.ttf'
buffer = BytesIO(); font.save(buffer)
data = buffer.getvalue()
if not output.exists() or output.read_bytes() != data:
    output.write_bytes(data)
print(output.name, hashlib.sha256(output.read_bytes()).hexdigest())
