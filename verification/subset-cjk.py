"""Reproduce the verification-only OFL subset; original file stays outside Git.
Usage: python verification/subset-cjk.py .work/cjk-font/NotoSansCJKsc-Bold.otf
fontTools 4.55.3. Provenance/license: verification/assets/README.md.
"""
from pathlib import Path
import hashlib
import sys
import argparse
from fontTools import subset
from fontTools.ttLib import TTFont

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('source',type=Path)
parser.add_argument('--subtitle',action='store_true',help='Separate subtitle face with covered U+2026; leave the original body subset unchanged')
args=parser.parse_args()
source = args.source
assert hashlib.sha256(source.read_bytes()).hexdigest() == 'b5f0d1a190a7f9b43c310a8850630af12553df32c4c050543f9059732d9b4c0a'
font = TTFont(source, recalcTimestamp=False)
options = subset.Options()
options.name_IDs = ['*']
options.name_legacy = True
options.name_languages = ['*']
subsetter = subset.Subsetter(options=options)
subsetter.populate(unicodes=list(range(0x20, 0x7f)) + [ord(c) for c in '合成歌词光影流动世界字幕计时中英混排'] + ([0x2026] if args.subtitle else []))
subsetter.subset(font)
family = 'Folia Parity Subtitle' if args.subtitle else 'Folia Parity CJK'
postscript = 'FoliaParitySubtitle-Bold' if args.subtitle else 'FoliaParityCJK-Bold'
names = {1: family, 2: 'Bold', 3: family + ' Bold subset 1', 4: family + ' Bold',
         6: postscript, 16: family, 17: 'Bold'}
for record in font['name'].names:
    if record.nameID in names:
        record.string = names[record.nameID].encode(record.getEncoding())
if 'CFF ' in font:
    font['CFF '].cff.fontNames = [postscript]
    top = font['CFF '].cff.topDictIndex[0]
    top.FamilyName = family
    top.FullName = family + ' Bold'
output = Path(__file__).resolve().parent / f'assets/{postscript}.otf'
font.save(output)
print(output, hashlib.sha256(output.read_bytes()).hexdigest())
