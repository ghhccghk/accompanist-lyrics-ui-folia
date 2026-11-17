"""Reject accidental system fallback in the new full-stage calibration matrix."""
from pathlib import Path
from fontTools.ttLib import TTFont
import base64,json
root=Path(__file__).resolve().parents[1]
rows={r.split('\t')[0]:r.split('\t')for r in(root/'verification/cadenza-fixtures/sceneInputs.tsv').read_text(encoding='utf-8').splitlines()if r and not r.startswith('#')}
fonts={'latin':'FoliaParityLatin-Bold.ttf','wrap':'FoliaParityLatin-Bold.ttf','arabic':'FoliaParityBidi-Bold.ttf','cjk':'FoliaParityChatCJK-Bold.otf'}
result={}
for scene,name in fonts.items():
    cmap=TTFont(root/'verification/assets'/name).getBestCmap()
    text=base64.b64decode(rows[scene][1]).decode('utf-8')+'Synthetic title0123456789:'
    missing=sorted({ord(c)for c in text if ord(c)not in cmap})
    assert not missing,(scene,name,missing)
    result[scene]={'font':name,'required_codepoints':len(set(text)),'missing':missing}
print(json.dumps(result))
