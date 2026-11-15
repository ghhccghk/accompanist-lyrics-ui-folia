// Canonical interruptions: sample actual Motion immediately before each boundary,
// so incoming transitions start from the outgoing continuous-time value.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/classic-interrupted-upstream');fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const rows=[`# Original classic interrupted entry, ${revision}`,
 '# Boundary left limits1us are sampled before status changes; continuous media reconstruction',
 '# durationMs\tmediaMs\tlineStartMs\topacity\tscale\tblur'];
try {
 for(const duration of [150,300]) {
  const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&dense&denseCount=6&denseStep=60&denseDuration=${duration}`);
  await page.waitForFunction(()=>window.ready);
  const times=[731,761,791,821,851,881,941,1001,1041,1101,1241,1401,1701,2001];
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:202},(_,i)=>i*10),...events,
   ...events.filter(x=>x>0).map(x=>x-.001),...times])].filter(t=>t<=2010).sort((a,b)=>a-b);
  const states=[];
  for(const tick of ticks) {
   await page.evaluate(t=>window.step(t),tick);
   if(!times.includes(tick))continue;
   const values=await page.locator('[data-overlap-fit-line]').evaluateAll(es=>es.map(el=>{
    const v=window.motionValues(el);return {startMs:Math.round(Number(el.getAttribute('data-overlap-fit-line'))*1000),
     opacity:v.opacity??1,scale:v.scale??1,blur:parseFloat(getComputedStyle(el).filter.replace('blur(',''))||0};
   }).filter(v=>v.opacity>0));
   states.push({mediaMs:tick,values});
   if(values.length)for(const v of values)rows.push([duration,tick,v.startMs,v.opacity,v.scale,v.blur].join('\t'));
   else rows.push([duration,tick,'-','-','-','-'].join('\t'));
   await page.screenshot({path:path.join(output,`${duration}-${tick}ms.png`)});
  }
  fs.writeFileSync(path.join(output,`${duration}-conditions.json`),JSON.stringify({revision,browser:browser.version(),
   viewport:[800,600],font:'FoliaParityLatin-Bold.ttf',duration,step:60,count:6,
   timing:'Manual Motion with exact events and their1us left limits; continuous canonical media model',states},null,2));
  await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/interrupted-lines.tsv'),rows.join('\n')+'\n');
}finally {await browser.close();}
