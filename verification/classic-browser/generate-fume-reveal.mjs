// Execute the untouched, pinned upstream functions, never a JS rewrite of the port.
import fs from 'node:fs';
import path from 'node:path';
import { referenceModules, root, revision } from './reference.mjs';
const { server, load } = await referenceModules();
try {
  const reveal = await load('components/visualizer/fume/fumeReveal.ts');
  const style = await load('components/visualizer/fume/fumeTextStyle.ts');
  const math = await load('components/visualizer/fume/fumeMath.ts');
  const hints = await load('utils/lyrics/renderHints.ts');
  const w = (text, startTime, endTime, syllables = []) => ({ text, startTime, endTime, syllables });
  const cases = [
    { fullText: 'Amber  echo', startTime: .7, endTime: 2.8, words: [w('Amber', .7, 1.6), w('echo', 1.8, 2.8)] },
    { fullText: 'one two', startTime: 0, endTime: .05, words: [] },
    { fullText: 'a', startTime: 0, endTime: 0, words: [w('a', 0, 0)] },
    { fullText: 'AB CD', startTime: .7, endTime: 2.8, words: [w('AB ', .7, 1.6,
      [w('A', .7, .8), w('B ', 1.5, 1.6)]), w('CD', 1.8, 2.8)] },
    { fullText: 'ABCD', startTime: 0, endTime: 1, words: [w('ABCD', 0, 1,
      [w('A', .7, .8), w('B', .1, .2), w('CD', .6, .9)])] },
    { fullText: '向远方 星光', startTime: 0, endTime: 2, words: [w('向远方', 0, 1), w('星光', 1, 2)] },
    { fullText: 'مرحبا بالعالم', startTime: .7, endTime: 2.8, words: [w('مرحبا', .7, 1.6), w('بالعالم', 1.8, 2.8)] },
    { fullText: 'Á 👩‍👩‍👧‍👦 🇨🇳', startTime: 0, endTime: .12, words: [w('Á', 0, .02), w('👩‍👩‍👧‍👦', .04, .06), w('🇨🇳', .08, .12)] },
    { fullText: 'tail', startTime: 0, endTime: .1, words: [w('ta', 0, .05), w('il', .05, .12)] },
    { fullText: 'unmatched text', startTime: 0, endTime: 1, words: [w('', 0, 0), w('x', 0, .5)] },
    { fullText: '', startTime: 0, endTime: 1, words: [] },
    ...['\t', '\n', '\u00a0', '\ufeff', '\u0085', '\u001c', '\u202f'].map(space =>
      ({ fullText: `A${space}B`, startTime: 0, endTime: 1, words: [w('A', 0, .5), w('B', .5, 1)] })),
  ];
  const b64 = s => Buffer.from(s).toString('base64');
  const descriptor = words => words.map(word => [b64(word.text), word.startTime, word.endTime,
    word.syllables.map(s => [b64(s.text), s.startTime, s.endTime].join('@')).join(';')].join('~')).join('|') || '-';
  const header = `# Folia ${revision}; generated from unmodified functions; synthetic text only`;
  const inputs = [header, '# id\ttextB64\tstart\tend\twords(textB64~start~end~syllables(textB64@start@end;...); | between words)'];
  const ranges = [header, '# id\twordIndex\tstart\tend\ttimings(start,end;...)'];
  const samples = [header, '# id\ttime\tprintedCount\tcontinuousProgress\tcutoffWithoutNext\tcutoffNextAt1.5'];
  let seed = 421;
  for (const [id, line] of cases.entries()) {
    inputs.push([id, b64(line.fullText), line.startTime, line.endTime, descriptor(line.words)].join('\t'));
    const gs = math.splitGraphemes(line.fullText), rs = reveal.buildWordRangesFromWords(line, gs);
    for (const r of rs) ranges.push([id, r.wordIndex, r.start, r.end,
      r.graphemeTimings.map(t => [t.startTime, t.endTime].join(',')).join(';') || '-'].join('\t'));
    const times = [-.1, 0, .000001, .02, .05, .08, .1, .119999, .12, .5, .7, 1, 1.5, 1.6, 1.8, 2, 2.8, 3.2,
      line.startTime - 1e-9, line.startTime, line.startTime + 1e-9, line.endTime - 1e-9, line.endTime, line.endTime + 1e-9];
    for (let i = 0; i < 64; i++) { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; times.push(seed / 2 ** 32 * 4 - .2); }
    for (const t of times) samples.push([id, t,
      reveal.resolvePrintedGraphemeCount(line, rs, gs.length, t),
      reveal.resolvePrintedGraphemeProgress(line, rs, gs.length, t),
      reveal.resolveLinePassCutoffTime(line, undefined), reveal.resolveLinePassCutoffTime(line, 1.5)].join('\t'));
  }
  const metrics = [header, '# kind\targuments...\texpected'];
  for (const start of [0, .7]) for (const duration of [-1, 0, .01, .4, 2])
    for (const cutoff of [-1, 0, .7001, .8, 4]) for (const time of [-.1, 0, .7, .8, 1, 3])
      metrics.push(['progress', start, duration, time, cutoff, reveal.resolveVisualProgressWithCutoff(start, duration, time, cutoff)].join('\t'));
  for (const time of [-1, 0, .7, 1, 4, 80, Infinity, NaN]) for (const at of [0, .7, Infinity])
    for (const duration of [-1, 0, 2.4, 130, Infinity])
      metrics.push(['dim', time, at, duration, style.resolvePassedDimAmount(time, at, duration)].join('\t'));
  for (const progress of [-1, 0, .05, .2, .8, .95, 1, 2]) for (const peak of [-1, .05, .8, .95, 2])
    metrics.push(['glow', progress, peak, math.resolveDelayedGlowEnvelope(progress, peak)].join('\t'));
  for (const hero of [false, true]) for (const dimmed of [false, true]) {
    const s = style.resolvePassedTextStyle(hero ? 'hero' : 'body', dimmed ? 'dimmed' : 'standard');
    metrics.push(['style', hero, dimmed, s.opacity, s.glowMultiplier, s.shadowAlphaBase, s.shadowAlphaTrail].join('\t'));
  }
  // Clear the original rounded-key memo before each independent duration query.
  // Keeping this behavior out of the port avoids seek/layout history affecting results.
  for (const ids of [[], [0], [0, 1, 4], [4, 0, 1], [0, 8, 1]]) for (const ratio of [0, .3, .6, .999, 1, 1.2]) {
    style.resolveFumePassedFadeDuration([{ startTime: 91, endTime: 92, words: [] }], .12345);
    metrics.push(['duration', ids.join(',') || '-', ratio, style.resolveFumePassedFadeDuration(ids.map(i => cases[i]), ratio)].join('\t'));
  }
  const folder = path.join(root, 'verification/fume-fixtures'); fs.mkdirSync(folder, { recursive: true });
  for (const [name, rows] of Object.entries({ inputs, ranges, samples, metrics })) fs.writeFileSync(path.join(folder, `${name}.tsv`), rows.join('\n') + '\n');
  console.log(JSON.stringify({ revision, cases: cases.length, ranges: ranges.length - 2,
    samples: samples.length - 2, metrics: metrics.length - 2 }));
} finally { await server.close(); }
