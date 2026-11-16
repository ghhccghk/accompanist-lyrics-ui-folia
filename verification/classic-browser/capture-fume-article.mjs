import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { chromium } from 'playwright';
import { checkReference, root, revision } from './reference.mjs';
checkReference();
const browser = await chromium.launch({ headless: true });
try {
  const folder = path.join(root, 'build/verification/fume-article-browser'); fs.mkdirSync(folder, { recursive: true });
  fs.writeFileSync(path.join(folder, 'conditions.json'), JSON.stringify({ reference: revision, browser: browser.version(),
    deviceScaleFactor: 1, viewportHeight: 600, widths: [380, 800, 1440], scenes: [0, 2, 4],
    fontSha256: createHash('sha256').update(fs.readFileSync(path.join(root, 'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
    framing: '92% full article; diagnostic block outlines; real original static text snapshots, no glow/passed alpha' }, null, 2));
  const rows = [`# untouched original article ${revision}; real pinned OFL font; geometry diagnostic, not Fume animation`,
    '# scene\tviewportW\tviewportH\tcolumns\tgap\tarticleW\tarticleH\tindex\thero\tx\ty\twidth\theight\tinnerWidth\tfontPx\tlineHeight\tlineCount'];
  const baselines = [`# Chromium Canvas middle-baseline shift, source snapshot font; ${revision}`, '# fontPx\tshiftPx'];
  const lineRows = [`# original renderLines ${revision}`, '# scene\twidth\tblockIndex\trow\ttextB64\tleft\ttop\twidth'];
  for (const scene of [0, 2, 4]) for (const width of [380, 800, 1440]) {
    const page = await browser.newPage({ viewport: { width, height: 600 }, deviceScaleFactor: 1 });
    await page.goto(`http://127.0.0.1:4173/fume-article.html?scene=${scene}`);
    await page.waitForFunction(() => window.ready);
    const a = await page.evaluate(() => window.fumeArticle);
    const shifts = await page.evaluate(a => a.blocks.map(b => [b.fontPx, window.fumeProbe(b.text, b.fontPx, b.innerWidth).middleShift]), a);
    for (const pair of shifts) baselines.push(pair.join('\t'));
    fs.writeFileSync(path.join(folder, `${scene}-${width}.json`), JSON.stringify(a, null, 2));
    await page.screenshot({ path: path.join(folder, `${scene}-${width}.png`) });
    for (const b of a.blocks) rows.push([scene, width, 600, a.columns, a.gap, a.width, a.height, b.index,
      b.hero, b.x, b.y, b.width, b.height, b.innerWidth, b.fontPx, b.lineHeight, b.lineCount].join('\t'));
    for (const b of a.blocks) b.rows.forEach((r, i) => lineRows.push([scene, width, b.index, i,
      Buffer.from(r.text).toString('base64'), r.left, r.top, r.width].join('\t')));
    await page.close();
  }
  fs.writeFileSync(path.join(root, 'verification/fume-fixtures/article-browser.tsv'), rows.join('\n') + '\n');
  fs.writeFileSync(path.join(root, 'verification/fume-fixtures/article-baselines.tsv'), baselines.join('\n') + '\n');
  fs.writeFileSync(path.join(root, 'verification/fume-fixtures/article-rows.tsv'), lineRows.join('\n') + '\n');
  console.log(JSON.stringify({ layouts: 9, blocks: rows.length - 2, folder }));
} finally { await browser.close(); }
