// Execute the unmodified pinned layout/overlap modules, never a rewritten JS oracle.
import fs from 'node:fs';
import path from 'node:path';
import { referenceModules, root, revision } from './reference.mjs';
const { server, load } = await referenceModules();
try {
  const { getOrBuildPartitaLayout, buildPartitaWordConfigs } = await load('components/visualizer/partita/partitaLayout.ts');
  const { getPartitaOverlapSpacing } = await load('components/visualizer/partita/partitaOverlap.ts');
  const { resolveLyricTextDirection } = await load('utils/lyrics/textDirection.ts');
  const cases = [
    ['one', ['Stay']],
    ['latin', ['We ', 'follow ', 'the ', 'quiet ', 'light ', 'home']],
    ['sticky', ['It', '’', 's ', 'our ', 'song', '!', ' ', 'We', "'", 're ', 'here', '.']],
    ['cjk', ['让', '我', '们', '沿', '着', '星', '光', '走', '向', '远', '方']],
    ['arabic', ['نحن ', 'نتبع ', 'ضوء ', 'الليل ', 'معاً']],
    ['clusters', ['Á ', '👩‍👩‍👧‍👦 ', 'sings ', 'ﬁne ', 'échos']],
  ];
  const out = [`# pinned ${revision}; original Partita layout / word poses / mixed-unit overlap; font=48 CSS px`,
    '# case\tinputWords(~)\tstartUs\theight\tintensity\tsemantic\trow\tunitCount\tdisplayWords(~)\tx\ty\tscale\trotate\tpassedRotate\tmarginBottom\ttotalGraphemes\tperspective\twordPoses(;)\twidthsEm(,)\tgaps(,)\ttop\tbottom'];
  for (const [name, texts] of cases) for (const start of [0, 1.234567]) for (const height of [180, 600, 960])
    for (const intensity of ['calm', 'normal', 'chaotic']) for (const semantic of [false, true]) {
      const words = texts.map((text, i) => ({ text, startTime: start + i * .375, endTime: start + (i + 1) * .375 }));
      const line = { fullText: texts.join(''), words, startTime: start, endTime: words.at(-1).endTime };
      const theme = { animationIntensity: intensity };
      const layout = getOrBuildPartitaLayout(new Map(), line, theme, height,
        { showGuideLines: true, useSemanticLayout: semantic, staggerMin: 20, staggerMax: 100, avoidOverlap: true });
      const rows = layout.columns.flatMap(c => c.words);
      const measured = rows.map(row => ({ config: row.config,
        widthsEm: row.displayWords.map((_, i) => .5 + i * .375),
        wordConfigs: buildPartitaWordConfigs(row.displayWords, row.config, intensity),
        rtl: resolveLyricTextDirection(row.displayWords.map(w => w.text).join(' ')) === 'rtl' }));
      const spacings = getPartitaOverlapSpacing(measured);
      function css(s) {
        if (!s) return 0;
        const expr = s.replace(/(-?[\d.]+)(rem|em|px)/g, (_, n, u) => String(Number(n) * (u === 'rem' ? 16 : u === 'em' ? 48 : 1)))
          .replaceAll('max(', 'Math.max(').replaceAll('calc(', '(');
        if (!/^[\d\s.,()+*\-Mathmax]+$/.test(expr)) throw Error(`Unexpected CSS expression ${s}`);
        return Function(`return (${expr})`)();
      }
      rows.forEach((row, i) => {
        const c = row.config, m = measured[i], spacing = spacings[i];
        out.push([name, texts.join('~'), Math.round(start * 1e6), height, intensity, semantic, i,
          row.chunkUnits.length, row.displayWords.map(w => w.text).join('~'), c.x, c.y, c.scale, c.rotate,
          c.passedRotate, css(c.marginBottom), layout.totalGraphemes, layout.lineConfig.perspective,
          m.wordConfigs.map(p => [p.x, p.y, p.scale, p.rotate, p.passedRotate].join(',')).join(';'),
          m.widthsEm.join(','), spacing.words.map(w => css(w.marginInlineEnd)).join(','),
          css(spacing.marginTop), css(spacing.marginBottom)].join('\t'));
      });
    }
  fs.mkdirSync(path.join(root, 'verification/partita-fixtures'), { recursive: true });
  fs.writeFileSync(path.join(root, 'verification/partita-fixtures/layout.tsv'), out.join('\n') + '\n');
  console.log(`Saved ${out.length - 2} original Partita rows`);
} finally { await server.close(); }
