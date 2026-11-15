// Actual pinned Classic's six-dot branch; no renderer replacements.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/classic-interlude-upstream');fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const layout=[`# pinned ${revision}; actual DOM layout`,
 '# scene\twidth\tmode\tindex\tleft\ttop\twidthPx\theightPx\tmargin\tlineHeight\trowGap\tpaddingTop\tpaddingBottom'];
const motion=[`# pinned ${revision}; actual Motion values`,
 '# scene\twidth\tmode\tmediaMs\tindex\tx\ty\tscale\trotate\topacity'];
try {
 for(const scene of ['interlude','interlude-single']) for(const width of [800,380]) for(const avoid of [false,true]) {
  const mode=avoid?'avoid':'default';
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&scene=${scene}${avoid?'&avoid':''}`);
  await page.waitForFunction(()=>window.ready);
  const times=[750,850,1000,1200,1500,1900,2100,2300,2500];
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:251},(_,i)=>i*10),...events,
   ...events.filter(t=>t>0).map(t=>t-.001),...times])].sort((a,b)=>a-b);
  const frames=[];
  for(const tick of ticks) {
   await page.evaluate(t=>window.step(t),tick);
   if(!times.includes(tick))continue;
   const values=await page.locator('[data-overlap-fit-line] > div').evaluateAll(es=>es.map(el=>window.motionValues(el)));
   values.forEach((v,i)=>motion.push([scene,width,mode,tick,i,v.x??0,v.y??0,v.scale??1,v.rotate??0,v.opacity??1].join('\t')));
   frames.push({mediaMs:tick,values});
   await page.screenshot({path:path.join(output,`${scene}-${width}-${mode}-${tick}ms.png`)});
   if(tick===750) {
    const metrics=await page.locator('[data-overlap-fit-line] > div').evaluateAll(es=>es.map(el=>{
     const s=getComputedStyle(el),p=getComputedStyle(el.parentElement);
     return [el.offsetLeft,el.offsetTop,el.offsetWidth,el.offsetHeight,parseFloat(s.marginInlineEnd),el.parentElement.clientHeight,
      parseFloat(p.rowGap)||0,parseFloat(p.paddingTop)||0,parseFloat(p.paddingBottom)||0];
    }));
    metrics.forEach((v,i)=>layout.push([scene,width,mode,i,...v].join('\t')));
   }
  }
  if(errors.length)throw new Error(errors.join('\n'));
  fs.writeFileSync(path.join(output,`${scene}-${width}-${mode}-conditions.json`),JSON.stringify({revision,browser:browser.version(),
   font:'FoliaParityLatin-Bold.ttf',viewport:[width,600],clock:'Manual Motion; exact event boundaries and1us left limits',frames},null,2));
  await page.close();
 }
 for(const [name,rows] of [['interlude-layout',layout],['interlude-motion',motion]])
  fs.writeFileSync(path.join(root,`verification/classic-fixtures/${name}.tsv`),rows.join('\n')+'\n');
} finally {await browser.close();}
