"""Report analytic-kernel evidence, deliberately distinct from mounted UI."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
reference = json.loads((root / 'verification/pendolo-fixtures/tick-reference.json').read_text(encoding='utf8'))
result = dict(reference=reference, status='Physical kernel only; mounted useSpring/frame-velocity and UI acceptance pending.')
for name in ['response', 'track']:
    rows = [r.split('\t') for r in (root / f'build/verification/pendolo-tick/{name}-residual.tsv').read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
    result[name] = dict(fields=len(rows), maximum_error=max(float(r[-1]) for r in rows))
out = root / 'verification/pendolo-tick'
out.mkdir(exist_ok=True)
(out / 'comparison.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf8')
print(json.dumps({k: result[k] for k in ['response', 'track']}, indent=2))
