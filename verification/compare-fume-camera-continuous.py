"""Report continuous camera calibration, including residuals against actual source rates.

No pass threshold or registration: this is trajectory evidence, not visual acceptance.
"""
from pathlib import Path
import csv
import hashlib
import json
import math
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
source = root / 'verification/fume-fixtures/camera-continuous.tsv'
native = root / 'build/verification/fume-camera-continuous/native.tsv'

def read(file):
    return list(csv.reader((s for s in file.read_text(encoding='utf-8').splitlines()
                            if s and not s.startswith('#')), delimiter='\t'))

expected, actual = read(source), read(native)
assert len(expected) == len(actual)
adaptation_error = max(abs(a - b) for e, n in zip(expected, actual)
                       for a, b in zip(map(float, e[6].split(',')), map(float, n[6].split(','))))
scenarios = []
for scenario in range(4):
    for rate in ['30', '60', '144', 'irregular']:
        group = [r for r in actual if r[0] == str(scenario) and r[1] == rate]
        distances, scales, screens = [], [], []
        for row in group:
            p, q = [list(map(float, row[i].split(','))) for i in [6, 7]]
            distance = math.hypot(p[0] - q[0], p[1] - q[1])
            distances.append(distance); scales.append(abs(p[6] - q[6]))
            screens.append(distance * max(p[6], q[6]))
        scenarios.append(dict(scenario=scenario, rate=rate, samples=len(group), nodes=int(group[0][5]),
                              maximum_world_distance=max(distances), maximum_scale_error=max(scales),
                              maximum_screen_distance=max(screens)))

out = root / 'verification/fume-camera-continuous'; out.mkdir(exist_ok=True)
report = dict(reference='cc6226e464050a456a2913e062374f23d3844cc5',
              conditions='synthetic measured camera blocks; 800x600; media 0..8s; float origin9.25; smooth/stepped x static/float',
              adaptation='log(A)/h spring; h=1/60 coefficient calibration only; adaptive RK4 with 1e-7px local trial error, printing/retarget knots, immutable event-prepared nodes',
              status='Internal calibration. Dynamic source residuals remain; NO complete-theme or device acceptance. Not connected to sample.',
              native_adapted_source_maximum_error=adaptation_error, scenarios=scenarios,
              source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
              native_sha256=hashlib.sha256(native.read_bytes()).hexdigest())
(out / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')

pic = Image.new('RGB', (1100, 760), 'white'); draw = ImageDraw.Draw(pic)
group = [r for r in actual if r[0] == '0' and r[1] == '60']
draw.text((75, 15), 'Fume continuous media camera vs original60Hz - CALIBRATION, not visual acceptance', fill='black')
for row, (title, indices) in enumerate([('Camera Y (world px)', [1]), ('Camera scale', [6]), ('Position error (screen px)', [])]):
    top, bottom, left, right = 65 + row * 220, 230 + row * 220, 80, 1065
    poses = [[list(map(float, r[i].split(','))) for i in [6, 7]] for r in group]
    if indices:
        series = [[p[side][indices[0]] for p in poses] for side in [0, 1]]
        labels = ['Native continuous', 'Original60Hz']
    else:
        series = [[math.hypot(p[0][0] - p[1][0], p[0][1] - p[1][1]) * max(p[0][6], p[1][6]) for p in poses]]
        labels = ['Native / source distance']
    low, high = min(map(min, series)), max(map(max, series)); span = max(high - low, .001)
    draw.text((left, top - 22), title, fill='black')
    for j in range(5):
        y = top + (bottom - top) * j / 4
        draw.line((left, y, right, y), fill='#e5e5e5')
        draw.text((5, y - 5), f'{high - span * j / 4:.3f}', fill='black')
    for side, values in enumerate(series):
        color = ['#2882d2', '#dd6928'][side]
        draw.line([(left + float(r[2]) / 8 * (right - left), bottom - (v - low) / span * (bottom - top))
                   for r, v in zip(group, values)], fill=color, width=2)
        draw.text((right - 225, top + 10 + side * 17), labels[side], fill=color)
    for t in range(9): draw.text((left + t / 8 * (right - left), bottom + 5), str(t), fill='black')
draw.text((80, 738), 'Media time (seconds). Original30/144Hz and irregular residuals are separately recorded in comparison.json.', fill='black')
pic.save(out / 'trajectories.png')
print(json.dumps(report, indent=2))
