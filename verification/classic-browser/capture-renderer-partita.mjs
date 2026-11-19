import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';
import { startReferenceServer } from './serve.mjs';
import { revision, root } from './reference.mjs';
const output = path.resolve(process.argv[2] ?? path.join(root, '.work/review-upstream-partita'));
fs.mkdirSync(output, {recursive:true});
const server = await startReferenceServer(4197);
let browser;
try {
  browser = await chromium.launch({headless:true});
  const page = await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.addInitScript(() => { delete Element.prototype.animate; });
  const epoch = new Date('2026-01-01T00:00:00Z');
  await page.clock.install({time:epoch});
  await page.clock.pauseAt(epoch);
  await page.goto('http://127.0.0.1:4197/?manual&theme=partita');
  await page.waitForFunction(() => window.ready);
  const events = await page.evaluate(() => window.eventTimes);
  const samples = [850,1500,3300,3550];
  const last = Math.max(...samples);
  const ticks = [...new Set([...Array.from({length: last / 10 + 1}, (_, i) => i * 10),
    ...events.filter(t => t <= last), ...samples])].sort((a,b) => a-b);
  for (const tick of ticks) {
    await page.evaluate(ms=>window.step(ms), tick);
    if (samples.includes(tick)) await page.screenshot({path:path.join(output,`baseline-${tick}.png`)});
  }
  if(errors.length) throw Error(errors.join('\n'));
  console.log(JSON.stringify({revision, browser:browser.version(), samples, output}));
} finally {
  await browser?.close();
  await server.close();
}
