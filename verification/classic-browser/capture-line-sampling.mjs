// Controlled parent layout vs transform phases on actual unchanged Classic.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/line-sampling-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});const rows=[];
try {
 for(const [scene,width,intensity,ms] of [['baseline',800,'normal',1500],['wrap',380,'calm',2800]]) for(const kind of ['line-layout','stage-layout','line-motion']) {
  const page=await browser.newPage({viewport:{width,height:600}});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&avoid&intensity=${intensity}${scene==='wrap'?'&scene=wrap':''}`);await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  for(const t of [...new Set([...Array.from({length:ms/10+1},(_,i)=>i*10),...events.filter(t=>t<=ms)])].sort((a,b)=>a-b))await page.evaluate(t=>window.step(t),t);
  for(const phase of [0,.25,.5,.75]) {
   const metrics=await page.evaluate(({kind,phase})=>{
    const line=document.querySelector('[data-overlap-fit-line]'),stage=line.parentElement;
    if(kind==='line-motion')line.style.transform=`translateY(${phase}px)`;
    else {const el=kind==='line-layout'?line:stage;el.style.position='relative';el.style.top=`${phase}px`;}
    return [line,stage].map(el=>{const s=getComputedStyle(el),r=el.getBoundingClientRect();return {top:r.y,height:r.height,willChange:s.willChange,transform:s.transform,filter:s.filter};});
   },{kind,phase});
   const name=`${scene}-${kind}-${phase}.png`,image=await page.screenshot({path:path.join(out,name)});
   const repeat=await page.screenshot();if(!image.equals(repeat))throw new Error(`Unstable ${name}`);
   rows.push({scene,width,intensity,ms,kind,phase,file:name,sha256:createHash('sha256').update(image).digest('hex'),metrics});
  }
  await page.close();
 }
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),note:'Controlled external CSS layout/motion phase only. Original source unchanged; actual mounted Motion replay before capture; no breathing.',rows},null,2));
}finally{await browser.close();}
