// CSS blur(0px) is mathematically identity but may introduce a raster surface.
// Observe that property on the actual original; do not change its source/curves.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();const out=path.join(root,'build/verification/line-isolation-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});const rows=[];
try {
 for(const [scene,width,intensity,ms] of [['baseline',800,'normal',1500],['wrap',380,'calm',2800]]) {
  const page=await browser.newPage({viewport:{width,height:600}});
  await page.addInitScript(()=>{delete Element.prototype.animate;});const epoch=new Date('2026-01-01T00:00:00Z');
  await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&avoid&intensity=${intensity}${scene==='wrap'?'&scene=wrap':''}`);await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  for(const t of [...new Set([...Array.from({length:ms/10+1},(_,i)=>i*10),...events.filter(t=>t<=ms)])].sort((a,b)=>a-b))await page.evaluate(t=>window.step(t),t);
  for(const [tag,filter] of [['none','none'],['zero','blur(0px)'],['tiny','blur(0.001px)'],['restored','none']]) {
   const metrics=await page.locator('[data-overlap-fit-line]').evaluate((el,filter)=>{
    el.style.filter=filter;const r=el.getBoundingClientRect();return {top:r.y,height:r.height,transform:getComputedStyle(el).transform,filter:getComputedStyle(el).filter};
   },filter);
   const file=`${scene}-${tag}.png`,image=await page.screenshot({path:path.join(out,file)});
   if(!image.equals(await page.screenshot()))throw new Error('Unstable '+file);
   rows.push({scene,width,intensity,ms,file,metrics});
  }
  await page.close();
 }
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),rows},null,2));
}finally{await browser.close();}
