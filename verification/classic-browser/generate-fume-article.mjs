// Original article + font-search code, with a deliberately synthetic measurement seam.
// This isolates placement/selection. It is not font or visual parity evidence.
import fs from 'node:fs';
import path from 'node:path';
import { referenceModules, root, upstream, revision } from './reference.mjs';
const { server } = await referenceModules();
try {
  const dir = path.join(upstream, 'src/components/visualizer/fume');
  const work = path.join(root, '.work');
  const measureSource = fs.readFileSync(path.join(dir, 'fumeTextMeasure.ts'), 'utf8');
  const start = measureSource.indexOf('export const chooseSingleLineFontPx =');
  const end = measureSource.indexOf('\n};', start) + 3;
  if (start < 0 || end <= start) throw Error('Missing original font-search declaration');
  const count = `const gs=s=>[...new Intl.Segmenter(undefined,{granularity:'grapheme'}).segment(s)];
  const countLines=(p,width)=>p.text.split('\\n').reduce((n,s)=>n+Math.max(1,Math.ceil(gs(s).length*p.font*.57/width)),0);`;
  fs.writeFileSync(path.join(work, 'fume-measure-fixture.ts'), `${count}
    export const countRenderableGraphemes=text=>gs(text).filter(value=>value.segment.trim().length>0).length;
    export const lineMeasuresFor=(_,text,variant)=>({text,variant,preparedByFontPx:new Map()});
    export const prepareLineAt=(m,font)=>{const p={text:m.text,font,segments:[m.text]};m.preparedByFontPx.set(font,p);return p};
    const fitsOnOneLine=(m,font,width)=>countLines(prepareLineAt(m,font),width)<=1;
    const sizeRegime=()=>null;
    const fitIn=()=>{throw Error('fixture always measures candidates')};
    ${measureSource.slice(start,end)}
    export const buildSegmentMetas=p=>({graphemes:gs(p.text).map(g=>g.segment),segmentMetas:[]});
    export const buildFontSpec=()=>'';
    export const buildGlyphOffsets=()=>[];export const buildRenderSegments=()=>[];
    export const buildWordRangeIndexByOffset=()=>[];export const cursorToGlobalOffset=()=>0;
  `);
  fs.writeFileSync(path.join(work, 'fume-pretext-fixture.ts'), 'export const layoutWithLines=()=>({lines:[]});');
  let source = fs.readFileSync(path.join(dir, 'fumeArticleLayout.ts'), 'utf8');
  source = source.replace(/from '([^']+)'/g, (_, spec) => {
    const target = spec === '@chenglou/pretext' ? './fume-pretext-fixture.ts'
      : spec === './fumeTextMeasure' ? './fume-measure-fixture.ts'
      : spec.startsWith('.') ? path.resolve(dir, spec).replaceAll('\\', '/') : spec;
    return `from '${target}'`;
  });
  fs.writeFileSync(path.join(work, 'fume-article-original.ts'), source);
  const { buildArticleLayout } = await server.ssrLoadModule(path.join(work, 'fume-article-original.ts'));
  const texts = [
    ['A short synthetic opening', 'Echoes remain'],
    ['Alpha', ' ', 'An invented sentence longer than its neighbors', '星光 向远方', 'مرحبا بالعالم', 'Á 👩‍🚀',
      'Synthetic chorus', 'Another quiet line', 'The final invented phrase', 'A retained echo', '十一', 'Tail'],
    Array.from({ length: 48 }, (_, i) => `Synthetic article line ${i} ${'echo '.repeat(i % 6)}`),
    ['\t', 'A\nB', '👩‍👩‍👧‍👦', '', 'Two lines\nfor fallback', 'A'.repeat(140)],
    ['A wholly invented sentence with a deliberately long paragraph. '.repeat(60),
      'An explicit synthetic first row\nAnd an explicit second row', 'A final echo'],
  ];
  const header = `# original ${revision}; measurement seam: each EGC advance=font*.57, hard newlines; NOT visual evidence`;
  const inputs = [header, '# scene\tindex\ttextB64\tstart\tend\tchorus'];
  texts.forEach((set, scene) => set.forEach((text, i) => inputs.push([scene, i, Buffer.from(text).toString('base64'),
    i * 2, i * 2 + 1.6, scene === 1 && i === 6].join('\t'))));
  const rows = [header, '# scene\tviewportW\tviewportH\tfontScale\theroScale\tcolumns\tgap\tdensity\tarticleW\tarticleH\tviewH\tpaperL\tpaperT\tpaperR\tpaperB\tfirstStart\tlastEnd\tblockIndex\tsourceIndex\thero\tx\ty\twidth\theight\tinnerWidth\tfontPx\tlineHeight\tlineCount'];
  const oldInfo = console.info; console.info = () => {};
  for (const [scene, set] of texts.entries()) for (const width of [380, 800, 1440]) for (const height of [240, 600])
    for (const fontScale of [.75, 1.4]) for (const heroScale of [.8, 1.3]) {
      const lines = set.map((fullText, i) => ({ fullText, startTime: i * 2, endTime: i * 2 + 1.6,
        isChorus: scene === 1 && i === 6, words: [] }));
      const steps = buildArticleLayout(lines, { width, height }, { name: 'FumeFixture', fontStyle: 'sans' }, fontScale, { heroScale });
      let step; do { step = steps.next(); } while (!step.done);
      const a = step.value;
      if (!a) throw Error('Expected article');
      a.blocks.forEach((b, i) => rows.push([scene, width, height, fontScale, heroScale, a.columns, a.gap,
        // density is recoverable from the final font searches but not exposed upstream; record sentinel.
        '-', a.width, a.height, a.viewportHeight, a.paperBounds.left, a.paperBounds.top, a.paperBounds.right,
        a.paperBounds.bottom, a.firstRenderableStartTime, a.lastChronologicalRenderEndTime, i,
        b.sourceLineIndex, b.variant === 'hero', b.x, b.y, b.width, b.height, b.innerWidth,
        b.fontPx, b.lineHeight, b.layout?.lines?.length === 0 ? b.height / b.lineHeight : -1].join('\t')));
    }
  console.info = oldInfo;
  const folder = path.join(root, 'verification/fume-fixtures');
  fs.writeFileSync(path.join(folder, 'article-inputs.tsv'), inputs.join('\n') + '\n');
  fs.writeFileSync(path.join(folder, 'article-layout.tsv'), rows.join('\n') + '\n');
  console.log(JSON.stringify({ layouts: texts.length * 24, blocks: rows.length - 2, revision }));
} finally { await server.close(); }
