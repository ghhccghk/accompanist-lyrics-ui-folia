// Capture unchanged mounted Folia Motion at a dense, deterministic media grid.
// This is a sampled sequence, not a wall-clock FPS or performance measurement.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/classic-continuous-upstream');
fs.mkdirSync(output,{recursive:true});
const times=[...new Set([...Array.from({length:121},(_,i)=>i*40),1500,3700])].sort((a,b)=>a-b);
const numeric=[`# Mounted original Classic dense sequence, pinned ${revision}`,
 '# mode\tmediaMs\tlineStartMs\twordIndex\ta\tb\tc\td\tx\ty\topacity\tbodyBlur\tr\tg\tb\tlineScale\tlineOpacity\tlineBlur\tglyphOpacities\tscene'];
const browser=await chromium.launch({headless:true});
try {
 for(const avoid of [false,true]) {
  const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');
  await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual${avoid?'&avoid':''}`);
  await page.waitForFunction(()=>window.ready===true);
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:481},(_,i)=>i*10),...events,...times])].filter(t=>t<=4800).sort((a,b)=>a-b);
  for(const tick of ticks) {
   await page.evaluate(t=>window.step(t),tick);
   if(times.includes(tick)) {
    const mode=avoid?'avoid':'default';
    await page.screenshot({path:path.join(output,`${mode}-${tick}ms.png`)});
    const values=await page.locator('.inline-block').evaluateAll(els=>els.map(el=>{
     const word=window.motionValues(el),parent=window.motionValues(el.parentElement);
     const body=getComputedStyle(el.querySelector('.z-10')),style=getComputedStyle(el.parentElement);
     const scale=word.scale??1,angle=(word.rotate??0)*Math.PI/180;
     return [Number(el.parentElement.getAttribute('data-overlap-fit-line'))*1000,
      Array.from(el.parentElement.children).indexOf(el),Math.cos(angle)*scale,Math.sin(angle)*scale,
      -Math.sin(angle)*scale,Math.cos(angle)*scale,word.x??0,word.y??0,word.opacity??1,
      parseFloat(body.filter.replace('blur(',''))||0,...body.color.match(/[\d.]+/g).map(Number).slice(0,3),
      parent.scale??1,parent.opacity??1,parseFloat(style.filter.replace('blur(',''))||0,
      Array.from(el.querySelectorAll('[aria-hidden] > span')).map(g=>getComputedStyle(g).opacity).join(',')];
    }));
    values.forEach(value=>numeric.push([mode,tick,...value,'baseline'].join('\t')));
   }
  }
  if(errors.length)throw new Error(errors.join('\n'));
  await page.close();
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),viewport:[800,600],
  font:'FoliaParityLatin-Bold.ttf',fontSha256:crypto.createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  source:'Unchanged pinned Classic with real mounted Motion, manual timing; same boundary event schedule as capture-manual.mjs',
  times,warning:'40ms media samples plus two seek checkpoints; not measured playback FPS'},null,2));
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/browser-sequence-motion.tsv'),numeric.join('\n')+'\n');
} finally {await browser.close();}
