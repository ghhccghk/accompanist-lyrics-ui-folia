import fs from 'node:fs';
import path from 'node:path';
import { keyframes } from 'framer-motion';
import { referenceModules, revision, root } from './reference.mjs';

const { server, load } = await referenceModules();
try {
  const { setGlowBlurQuantized } = await load('utils/glowBlurQuantize.ts');
  setGlowBlurQuantized(false);
  const { wordGlowVariants } = await load('components/visualizer/wordGlow.ts');
  const rows = [`# pinned Folia ${revision}; original wordGlowVariants + Framer keyframes`,
    '# mode\ttotal\twordStartUs\twordEndUs\tcharStartUs\tcharEndUs\telapsedUs\tpassedElapsedUs\topacity'];
  for (const mode of ['normal', 'fast', 'instant']) {
    for (const total of [1, 6]) {
      const wordStart = .7, wordEnd = mode === 'normal' ? 1.3 : mode === 'fast' ? .84 : .78;
      const duration = Math.max(wordEnd - wordStart, mode === 'normal' ? .1 : mode === 'fast' ? .12 : .08);
      const charStart = total === 1 ? wordStart : .9, charEnd = total === 1 ? wordEnd : 1;
      const custom = { activeColor: '#ffffff', wordRevealMode: mode, duration, total,
        charStartTime: charStart, charEndTime: charEnd, wordStartTime: wordStart };
      const active = wordGlowVariants.active(custom), passed = wordGlowVariants.passed(custom);
      const generator = () => keyframes({ keyframes: active.opacity, duration: active.transition.duration * 1000,
        times: active.transition.times, ease: active.transition.ease });
      const value = us => generator().next(Math.max(0, us / 1000 - (active.transition.delay || 0) * 1000)).value;
      for (const elapsedUs of [0, 25000, 50000, 100000, 150000, 200000, 250000, 300000, 400000, 500000, 600000, 800000]) {
        rows.push([mode, total, 700000, Math.round(wordEnd * 1e6), Math.round(charStart * 1e6), Math.round(charEnd * 1e6), elapsedUs, -1, value(elapsedUs)].join('\t'));
      }
      const stop = 350000;
      for (const elapsedUs of [0, 50000, 100000, 200000, 400000, 900000]) {
        const fade = keyframes({ keyframes: [value(stop), 0], duration: passed.transition.duration * 1000, ease: passed.transition.ease });
        rows.push([mode, total, 700000, Math.round(wordEnd * 1e6), Math.round(charStart * 1e6), Math.round(charEnd * 1e6), stop, elapsedUs, fade.next(elapsedUs / 1000).value].join('\t'));
      }
    }
  }
  fs.writeFileSync(path.join(root, 'verification/classic-fixtures/glow.tsv'), rows.join('\n') + '\n');
} finally { await server.close(); }
