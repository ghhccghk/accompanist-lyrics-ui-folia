// Actual pinned Classic/GlowWord for every original intensity, not a CSS imitation.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/classic-intensity-upstream');fs.mkdirSync(out,{recursive:true});
const fixtures=path.join(root,'verification/classic-fixtures');
const motion=[`# pinned original mounted Motion ${revision}`,'# intensity\tscene\tmode\tms\tindex\tx\ty\tscale\trotate\talpha\tlineScale\tlineAlpha\tlineBlur'];
const geometry=[`# pinned original Classic configs and DOM ${revision}`,'# intensity\tscene\tmode\tindex\tx\ty\tscale\trotate\tpassedRotate\tmargin\tleft\ttop\twidth\theight\tlineHeight'];
const breath=[`# pinned original Classic real breathing ${revision}`,'# intensity\twallMs\ty\tscale\tlastRaf\tmount'];
const times=[750,1000,1500,1900,2100,2300,2500,2800,3000];
const browser=await chromium.launch({headless:true});
try {
 for(const intensity of ['calm','normal','chaotic']) {
  for(const [scene,width] of [['baseline',800],['wrap',380]]) for(const avoid of [false,true]) {
   const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
   const errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.addInitScript(()=>{delete Element.prototype.animate;});
   const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
   await page.goto(`http://127.0.0.1:4173/?manual&intensity=${intensity}${scene==='wrap'?'&scene=wrap':''}${avoid?'&avoid':''}`);
   await page.waitForFunction(()=>window.ready);
   const events=await page.evaluate(()=>window.eventTimes);
   const ticks=[...new Set([...Array.from({length:301},(_,i)=>i*10),...events.filter(t=>t<=3000),...times])].sort((a,b)=>a-b);
   for(const tick of ticks) {
    await page.evaluate(t=>window.step(t),tick);
    if(!times.includes(tick))continue;
    const mode=avoid?'avoid':'default';
    await page.screenshot({path:path.join(out,`${intensity}-${scene}-${mode}-${tick}ms.png`)});
    const values=await page.locator('[data-overlap-fit-line] > div').evaluateAll(els=>els.map(el=>{
     const v=window.motionValues(el),p=window.motionValues(el.parentElement),c=window.motionConfig(el);
     return {motion:[...['x','y','scale','rotate','opacity'].map(k=>v[k]),p.scale??1,p.opacity??1,parseFloat(getComputedStyle(el.parentElement).filter.replace('blur(',''))||0],
      geometry:[c.x,c.y,c.scale,c.rotate,c.passedRotate,parseFloat(c.marginRight),el.offsetLeft,el.offsetTop,el.offsetWidth,el.offsetHeight,el.parentElement.offsetHeight]};
    }));
    values.forEach((v,i)=>{
     motion.push([intensity,scene,mode,tick,i,...v.motion].join('\t'));
     if(tick===1500)geometry.push([intensity,scene,mode,i,...v.geometry].join('\t'));
    });
   }
   if(errors.length)throw new Error(errors.join('\n'));await page.close();
  }
  // Wall-clock RAF trajectory of the actual unchanged outer wrapper, two periods.
  const page=await browser.newPage({viewport:{width:800,height:600}});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?breathe&intensity=${intensity}`);await page.waitForFunction(()=>window.ready);
  const period={calm:8500,normal:7000,chaotic:5800}[intensity];
  const mount=await page.evaluate(()=>window.mountTime);let previous=0;
  for(const ms of [...new Set([16,160,800,...Array.from({length:17},(_,i)=>i*period/8)])].sort((a,b)=>a-b)) {
   await page.clock.runFor(ms-previous);previous=ms;
   const v=await page.locator('.h-\\[70vh\\]').evaluate(el=>{const m=new DOMMatrix(getComputedStyle(el).transform);return [m.m42,m.a,window.rafTimes.at(-1)?.[1]??0,window.mountTime];});
   if(v[3]!==mount)throw new Error('Unexpected reference reload');breath.push([intensity,ms,...v].join('\t'));
  }
  await page.close();
 }
 for(const [file,rows] of [['intensity-motion.tsv',motion],['intensity-geometry.tsv',geometry],['intensity-breathing.tsv',breath]])fs.writeFileSync(path.join(fixtures,file),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),times,height:600,density:1,
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  note:'Actual pinned Classic/GlowWord; default/avoid; fixed synthetic baseline/wrap; captions/chrome excluded, breathing disabled for frames; separate real DOM breathing traces. No performance claim.'},null,2));
} finally {await browser.close();}
