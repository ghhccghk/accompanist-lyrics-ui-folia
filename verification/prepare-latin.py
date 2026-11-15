"""Make a renamed static OFL parity face without ambiguous variable-family STAT data.

Glyph outlines, hinting, advances and shaping tables are retained byte for byte.
The original pinned font is preserved for regression/diagnosis.
"""
from pathlib import Path
from hashlib import sha256
from fontTools.ttLib import TTFont

assets = Path(__file__).resolve().parent / 'assets'
source = assets / 'NotoSans-Bold.ttf'
assert sha256(source.read_bytes()).hexdigest() == 'c976e4b1b99edc88775377fcc21692ca4bfa46b6d6ca6522bfda505b28ff9d6a'
font = TTFont(source, recalcTimestamp=False)
del font['STAT']
names = {1: 'Folia Parity Latin', 2: 'Bold', 3: 'FoliaParityLatin-Bold 1.0',
         4: 'Folia Parity Latin Bold', 6: 'FoliaParityLatin-Bold'}
for record in font['name'].names:
    if record.nameID in names:
        record.string = names[record.nameID].encode(record.getEncoding())
target = assets / 'FoliaParityLatin-Bold.ttf'
font.save(target)
# No outline, hinting, metric or shaping changes are used to tune the pixels.
original = TTFont(source)
result = TTFont(target)
for tag in original.keys():
    if tag not in ('GlyphOrder', 'head', 'name', 'STAT'):
        assert original.getTableData(tag) == result.getTableData(tag), tag
print(target.name, sha256(target.read_bytes()).hexdigest())
