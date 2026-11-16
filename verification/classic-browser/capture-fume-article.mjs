import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';
import { checkReference, root, revision } from './reference.mjs';
checkReference();
const browser = await chromium.launch({ headless: true });
try {
  const folder = path.join(root, 'build/verification/fume-article-browser'); fs.mkdirSync(folder, { recursive: true });
  const rows = [`# untouched original article ${revision}; real pinned OFL font; geometry diagnostic, not Fume animation`,
    '# scene\tviewportW\tviewportH\tcolumns\tgap\tarticleW\tarticleH\tindex\thero\tx\ty\twidth\theight\tinnerWidth\tfontPx\tlineHeight\tlineCount'];
  for (const scene of [0, 2]) for (const width of [380, 800, 1440]) {
    const page = await browser.newPage({ viewport: { width, height: 600 }, deviceScaleFactor: 1 });
    await page.goto(`http://127.0.0.1:4173/fume-article.html?scene=${scene}`);
    await page.waitForFunction(() => window.ready);
    const a = await page.evaluate(() => window.fumeArticle);
    fs.writeFileSync(path.join(folder, `${scene}-${width}.json`), JSON.stringify(a, null, 2));
    await page.screenshot({ path: path.join(folder, `${scene}-${width}.png`) });
    for (const b of a.blocks) rows.push([scene, width, 600, a.columns, a.gap, a.width, a.height, b.index,
      b.hero, b.x, b.y, b.width, b.height, b.innerWidth, b.fontPx, b.lineHeight, b.lineCount].join('\t'));
    await page.close();
  }
  fs.writeFileSync(path.join(root, 'verification/fume-fixtures/article-browser.tsv'), rows.join('\n') + '\n');
  console.log(JSON.stringify({ layouts: 6, blocks: rows.length - 2, folder }));
} finally { await browser.close(); }
