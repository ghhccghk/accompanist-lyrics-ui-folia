"""Original 12-second quiet sine WAV for native transport verification, never sample content."""
import argparse
import array
import math
from pathlib import Path
import wave

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('output', type=Path)
args = parser.parse_args()
args.output.parent.mkdir(parents=True, exist_ok=True)
samples = array.array('h', (int(1200 * math.sin(2 * math.pi * 440 * n / 48000)) for n in range(12 * 48000)))
import sys
if sys.byteorder != 'little':
    samples.byteswap()
with wave.open(str(args.output), 'wb') as output:
    output.setparams((1, 2, 48000, 0, 'NONE', 'not compressed'))
    output.writeframes(samples.tobytes())
print(args.output)
