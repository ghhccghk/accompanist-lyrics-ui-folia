// Actual unchanged classic at matching CSS and physical viewport sizes.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/layout-phase-public-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const frames=[];
 for(const dpr of [1,2]) for(const width of [800,380]) for(const avoid of [false,true]) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:dpr});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual${avoid?'&avoid':''}`);await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  const times=[750,1000,1500,1900,2100];
  const ticks=[...new Set([...Array.from({length:211},(_,i)=>i*10),...events.filter(t=>t<=2100),...times])].sort((a,b)=>a-b);
  for(const tick of ticks) {
   await page.evaluate(t=>window.step(t),tick);
   if(times.includes(tick)) {
    const file=`dpr${dpr}-${width}-${avoid?'avoid':'default'}-${tick}ms.png`;
    await page.screenshot({path:path.join(out,file)});
    const metrics=await page.locator('[data-overlap-fit-line] > div').evaluateAll(els=>els.map(el=>({
     offsetLeft:el.offsetLeft,offsetTop:el.offsetTop,width:getComputedStyle(el).width,height:getComputedStyle(el).height,
     wordTransform:getComputedStyle(el).transform,lineTransform:getComputedStyle(el.parentElement).transform,
     wordValues:window.motionValues(el),lineValues:window.motionValues(el.parentElement)})));
    frames.push({dpr,width,avoid,mediaMs:tick,file,metrics});
   }
  }
  if(errors.length)throw new Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),font:'FoliaParityLatin-Bold.ttf',
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  height:600,clock:'Actual mounted manual Motion; unchanged baseline event grid; no captions/chrome/breathing',frames},null,2));
}finally {await browser.close();}
