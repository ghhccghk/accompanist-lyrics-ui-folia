import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { root, revision } from './reference.mjs';

const browser = await chromium.launch({ headless: true });
const directory = path.join(root, 'build/verification/classic-upstream');
fs.mkdirSync(directory, { recursive: true });
try {
  const checkpoints = [0, 700, 750, 850, 1000, 1200, 1300, 1500, 1800, 1900, 2100, 2220, 2300, 2500, 3000, 3200, 3400, 3700, 4100, 4400, 4700];
  for (const avoid of [false, true]) {
    const page = await browser.newPage({ viewport: { width: 800, height: 600 }, deviceScaleFactor: 1 });
    const errors = [];
    page.on('pageerror', e => errors.push(e.message));
    // Framer's JS fallback and RAF share the Playwright virtual clock. Browser compositor
    // WAAPI has an independent real clock; leaving it enabled would make screenshots invalid.
    await page.addInitScript(() => { delete Element.prototype.animate; });
    await page.goto('http://127.0.0.1:4173/' + (avoid ? '?avoid' : ''));
    await page.waitForFunction(() => window.ready === true);
    // Install after modules/fonts initialize, then replay monotonically from empty media time.
    await page.clock.install();
    await page.clock.pauseAt(new Date(Date.now() + 100));
    const frames = [];
    let media = 0;
    for (const target of checkpoints) {
      while (media < target) {
        const next = Math.min(target, media + 10);
        await page.evaluate(t => window.step(t), next);
        await page.clock.runFor(next - media);
        media = next;
      }
      const mode = avoid ? 'avoid' : 'default';
      await page.screenshot({ path: path.join(directory, `${mode}-${target}ms.png`) });
      const words = await page.locator('.inline-block').evaluateAll(elements => elements.map(el => {
        const style = getComputedStyle(el);
        const body = el.querySelector('.z-10');
        const bs = getComputedStyle(body);
        return { text: body.textContent, left: el.offsetLeft, top: el.offsetTop,
          width: el.offsetWidth, height: el.offsetHeight, margin: style.marginInlineEnd,
          transform: style.transform, opacity: style.opacity, color: bs.color, blur: bs.filter,
          glyphs: Array.from(el.querySelectorAll('[aria-hidden] > span')).map(g => ({text:g.textContent,opacity:getComputedStyle(g).opacity})) };
      }));
      frames.push({ mediaMs: target, words });
    }
    if (errors.length) throw new Error(errors.join('\n'));
    fs.writeFileSync(path.join(directory, `${avoid ? 'avoid' : 'default'}-frames.json`), JSON.stringify({ revision,
      browser: browser.version(), viewport: [800, 600], density: 1, font: 'NotoSans-Bold.ttf',
      mediaStepMs: 10, note: 'Original classic/GlowWord; backdrop/chrome/subtitles excluded; breathing disabled; Framer JS fallback (WAAPI disabled to share virtual clock); monotonic replay.', frames }, null, 2));
    await page.close();
  }
} finally { await browser.close(); }
