import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const browser=await chromium.launch({headless:true});
try {
  const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  await page.addInitScript(()=>{
    delete Element.prototype.animate;
  });
  // Freeze before mounting, so module/font loading cannot move the breathing phase.
  const epoch=new Date('2026-01-01T00:00:00Z');
  await page.clock.install({time:epoch});
  await page.clock.pauseAt(epoch);
  await page.goto('http://127.0.0.1:4173/?breathe');
  await page.waitForFunction(()=>window.ready===true);
  const checkpoints=[0,16,32,160,400,800,1200,1744,1750,1760,2400,3000,3500,4000,4800,5248,5264,6000,6992,7000,7024,8750,10500,12250,14000];
  const rows=[`# original Classic breathing DOM; ${revision}; Chromium ${browser.version()}`,
    '# wallMs\ty\tscale\tlastRafMs\tmountTimeMs'];
  let now=0;
  const mount=await page.evaluate(()=>window.mountTime);
  for(const t of checkpoints) {
    await page.clock.runFor(t-now); now=t;
    const frame=await page.locator('.h-\\[70vh\\]').evaluate(el=>{
      const m=new DOMMatrix(getComputedStyle(el).transform);return {y:m.m42,scale:m.a,last:window.rafTimes.at(-1)?.[1]??0,mount:window.mountTime};
    });
    if(frame.mount!==mount) throw new Error('Page reloaded during capture; discard the trajectory');
    rows.push([t,frame.y,frame.scale,frame.last,frame.mount].join('\t'));
  }
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/breathing.tsv'),rows.join('\n')+'\n');
} finally {await browser.close();}
