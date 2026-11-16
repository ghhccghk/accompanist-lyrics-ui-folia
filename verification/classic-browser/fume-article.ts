// Geometry diagnostic of the real original article builder, not the full Fume renderer.
import { buildArticleLayout } from '../../.work/folia-major/src/components/visualizer/fume/fumeArticleLayout';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
import inputs from '../fume-fixtures/article-inputs.tsv?raw';
import { prepareWithSegments, walkLineRanges, measureNaturalWidth } from '@chenglou/pretext';
import { createStaticBlockSnapshot } from '../../.work/folia-major/src/components/visualizer/fume/fumeCanvasText';
const font = new FontFace('Fume Parity', `url(${fontUrl})`, { weight: '700' });
await font.load(); document.fonts.add(font);
const scene = new URLSearchParams(location.search).get('scene') ?? '0';
const lines = inputs.split(/\r?\n/).filter(r => !r.startsWith('#') && r.split('\t')[0] === scene)
  .map(r => { const f = r.split('\t'); return { fullText: new TextDecoder().decode(Uint8Array.from(atob(f[2]), c => c.charCodeAt(0))),
    startTime: Number(f[3]), endTime: Number(f[4]), isChorus: f[5] === 'true', words: [] }; });
const viewport = { width: innerWidth, height: innerHeight };
const theme = { name: 'FumeFixture', fontStyle: 'sans', fontFamily: 'Fume Parity', fontWeight: 700 };
const steps = buildArticleLayout(lines, viewport, theme, 1, { heroScale: 1 });
let step; do { step = steps.next(); } while (!step.done);
const a = step.value;
if (!a) throw Error('Missing article');
const canvas = document.querySelector('canvas')!; canvas.width = innerWidth; canvas.height = innerHeight;
const ctx = canvas.getContext('2d')!;
ctx.fillStyle = '#162435'; ctx.fillRect(0, 0, innerWidth, innerHeight);
// Full article framed at 92%, not original animated camera. Same fixed transform on both sides.
const scale = Math.min(innerWidth / a.width, innerHeight / a.height) * .92;
ctx.translate(innerWidth / 2, innerHeight / 2); ctx.scale(scale, scale); ctx.translate(-a.width / 2, -a.height / 2);
for (const b of a.blocks) {
  ctx.strokeStyle = b.variant === 'hero' ? '#54cae1' : '#52677f'; ctx.lineWidth = 1 / scale;
  ctx.strokeRect(b.x, b.y, b.width, b.height);
  const snapshot = createStaticBlockSnapshot(b, theme, '#dfe5ec', 0, 'transparent', 1);
  if (!snapshot) throw Error('Original snapshot failed');
  ctx.drawImage(snapshot.canvas, b.x - snapshot.padding, b.y - snapshot.padding);
}
(window as any).fumeArticle = { width: a.width, height: a.height, viewportHeight: a.viewportHeight, columns: a.columns,
  gap: a.gap, bounds: a.paperBounds, blocks: a.blocks.map(b => ({ index: b.sourceLineIndex, text: b.line.fullText,
    hero: b.variant === 'hero', x: b.x, y: b.y, width: b.width, height: b.height, innerWidth: b.innerWidth,
    fontPx: b.fontPx, lineHeight: b.lineHeight, lineCount: b.renderLines.length,
    rows: b.renderLines.map(r => ({ text: r.text, left: r.left, top: r.top, width: r.width })) })) };
(window as any).ready = true;
(window as any).fumeProbe = (text: string, px: number, width: number) => {
  const spec = `700 ${px}px 'Fume Parity'`;
  ctx.font = spec;
  const prepared = prepareWithSegments(text, spec, { whiteSpace: 'pre-wrap' });
  return { native: ctx.measureText(text).width, trimmed: ctx.measureText(text.trimEnd()).width,
    natural: measureNaturalWidth(prepared), count: walkLineRanges(prepared, width, () => {}), segments: prepared.segments,
    widths: prepared.widths };
};
