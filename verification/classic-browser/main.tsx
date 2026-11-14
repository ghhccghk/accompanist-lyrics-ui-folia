import React from 'react';
import { createRoot } from 'react-dom/client';
import { flushSync } from 'react-dom';
import { motionValue } from 'framer-motion';
import Classic from 'pinned-classic';
import { getLineRenderEndTime } from '../../.work/folia-major/src/utils/lyrics/renderHints';
import { setGlowBlurQuantized } from '../../.work/folia-major/src/utils/glowBlurQuantize';
import fontUrl from '../assets/NotoSans-Bold.ttf?url';

// Windows text-shadow branch is the baseline. No source patches and no remote media.
setGlowBlurQuantized(false);
const face = new FontFace('Parity Noto', `url(${fontUrl})`, { weight: '700' });
await face.load(); document.fonts.add(face);
const currentTime = motionValue(0);
const audioPower = motionValue(0);
const themes = { animationIntensity: 'normal', fontStyle: 'sans', fontFamily: 'Parity Noto', fontWeight: 700,
  primaryColor: '#dfe5ec', accentColor: '#ffffff', secondaryColor: '#adb9c8', wordColors: [] };
const lines = [
  { startTime: .7, endTime: 1.9, fullText: 'Amber echo', words: [
    { text: 'Amber ', startTime: .7, endTime: 1.3 }, { text: 'echo', startTime: 1.3, endTime: 1.9 }] },
  { startTime: 3.2, endTime: 4.1, fullText: 'Silver light', words: [
    { text: 'Silver ', startTime: 3.2, endTime: 3.65 }, { text: 'light', startTime: 3.65, endTime: 4.1 }] },
];
const params = new URLSearchParams(location.search);
const root = createRoot(document.getElementById('root')!);
(window as any).step = (ms: number) => {
  const seconds = ms / 1000;
  // Set the selected line before emitting time, matching host→runtime→GlowWord ownership.
  const index = lines.findIndex((l, i) => seconds >= l.startTime && seconds < getLineRenderEndTime(l) && (!lines[i + 1] || seconds < lines[i + 1].startTime));
  flushSync(() => root.render(<Classic currentTime={currentTime} currentLineIndex={index} lines={lines}
    theme={themes} audioPower={audioPower} audioBands={{}} classicTuning={{ enableWordRotation: true,
      breathingFloatMultiplier: params.has('breathe') ? 1 : 0, wordSpacing: .7, avoidOverlap: params.has('avoid'), useLegacyLayout: false }} />));
  flushSync(() => currentTime.set(seconds));
};
(window as any).step(0);
(window as any).ready = true;
