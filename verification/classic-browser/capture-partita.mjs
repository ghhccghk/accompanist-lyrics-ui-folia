import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { root, revision, checkReference } from './reference.mjs';
checkReference();
const out = path.join(root, 'build/verification/partita-browser');
const tightOnly=process.argv.includes('--tight-only');
const avoidOnly=process.argv.includes('--avoid-only');
if(tightOnly&&avoidOnly)throw Error('Choose one additional matrix at a time');
fs.mkdirSync(out, { recursive: true });
const trace = [`# Original PartitaChunk mounted Motion ${revision}`,
  '# intensity\tscene\twidth\tms\trow\tx\ty\tscale\trotate\talpha\tleft\ttop\twidth\theight\tlineHeight'];
const times = tightOnly ? [700,750,800,850,900,950,1000,1050,1150,1300,1500,1750]
  : [700, 750, 850, 1000, 1150, 1300, 1500, 1750, 2100, 2500, 2800, 3000];
const browser = await chromium.launch({ headless: true });
try {
  for (const intensity of ['calm', 'normal', 'chaotic']) for (const scene of (tightOnly?['tight']:avoidOnly?['wrap']:['wrap', 'arabic', 'group']))
    for (const width of [380, 800]) {
      const page = await browser.newPage({ viewport: { width, height: 600 }, deviceScaleFactor: 1 });
      const errors=[]; page.on('pageerror', e=>errors.push(e.message));
      await page.addInitScript(() => { delete Element.prototype.animate; });
      const epoch = new Date('2026-01-01T00:00:00Z');
      await page.clock.install({ time: epoch }); await page.clock.pauseAt(epoch);
      await page.goto(`http://127.0.0.1:4173/?manual&theme=partita&scene=${scene}&intensity=${intensity}${avoidOnly?'&avoid':''}`);
      await page.waitForFunction(() => window.ready);
      const events = await page.evaluate(() => window.eventTimes);
      const ticks = [...new Set([...Array.from({length:301},(_,i)=>i*10), ...events.filter(t=>t<=3000), ...times])].sort((a,b)=>a-b);
      for (const tick of ticks) {
        await page.evaluate(t => window.step(t), tick);
        if (!times.includes(tick)) continue;
        await page.screenshot({ path: path.join(out, `${intensity}-${scene}-${width}${avoidOnly?'-avoid':''}-${tick}ms.png`) });
        const values=await page.locator('.inline-flex').evaluateAll(els=>els.map(el=>{
          const v=window.motionValues(el);
          return [...['x','y','scale','rotate','opacity'].map(k=>v[k]??(k==='scale'||k==='opacity'?1:0)),
            el.offsetLeft,el.offsetTop,el.offsetWidth,el.offsetHeight,el.parentElement.offsetHeight];
        }));
        values.forEach((v,i)=>trace.push([intensity,scene,width,tick,i,...v].join('\t')));
      }
      if(errors.length) throw Error(errors.join('\n'));
      await page.close();
    }
  fs.writeFileSync(path.join(root,`verification/partita-fixtures/chunk-motion${tightOnly?'-tight':avoidOnly?'-avoid':''}.tsv`),trace.join('\n')+'\n');
  fs.writeFileSync(path.join(out,`conditions${tightOnly?'-tight':avoidOnly?'-avoid':''}.json`),JSON.stringify({revision,browser:browser.version(),times,
    height:600,dpr:1,fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf'].map(name=>({name,
      sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')})),
    note:'Actual unmodified original Partita/PartitaChunk/GlowWord. Text-only shell; subtitles/background omitted. Actual continuous float retained. Not a performance measurement.'},null,2));
  console.log(`Saved ${trace.length-2} chunk samples and ${(tightOnly||avoidOnly?6:18)*times.length} screenshots`);
} finally { await browser.close(); }
