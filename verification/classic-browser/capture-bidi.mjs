// Pinned Classic: inherited paragraph direction, not just reversed word boxes.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/classic-bidi-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});
const rows=[`# pinned ${revision}; original intact body Range rectangles`,
 '# scene\tword\tchar\tleft\tright\tdirection'];
try {
 for(const scene of ['bidi-rtl','bidi-ltr']) for(const avoid of [false,true]) {
  const mode=avoid?'avoid':'default';
  const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&scene=${scene}${avoid?'&avoid':''}`);
  await page.waitForFunction(()=>window.ready);
  const times=[750,1000,1200,1500,1900,2100,2300,2500];
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:251},(_,i)=>i*10),...events,...events.filter(t=>t>0).map(t=>t-.001)])].sort((a,b)=>a-b);
  for(const tick of ticks) {
   await page.evaluate(t=>window.step(t),tick);
   if(times.includes(tick))await page.screenshot({path:path.join(out,`${scene}-${mode}-${tick}ms.png`)});
  }
  // Normalize transforms on the actual original body, only for the separate
  // text-layout diagnostic. The complete animation PNGs above stay unchanged.
  await page.reload();await page.waitForFunction(()=>window.ready);await page.evaluate(()=>window.step(1500));
  const metrics=await page.locator('[data-overlap-fit-line] > div').evaluateAll(es=>es.map(el=>{
   for(let node=el;node;node=node.parentElement)node.style.transform='none';
   const body=el.querySelector('.z-10');const bounds=body.getBoundingClientRect();
   return {text:body.textContent,direction:getComputedStyle(body).direction,characters:Array.from(body.textContent).map((char,i)=>{
    const range=document.createRange();range.setStart(body.firstChild,i);range.setEnd(body.firstChild,i+1);
    const r=range.getBoundingClientRect();return {char,left:r.left-bounds.left,right:r.right-bounds.left};
   })};
  }));
  if(!avoid)metrics.forEach((word,i)=>word.characters.forEach(c=>rows.push([scene,i,c.char===' '?'SPACE':c.char,c.left,c.right,word.direction].join('\t'))));
  fs.writeFileSync(path.join(out,`${scene}-${mode}-conditions.json`),JSON.stringify({revision,browser:browser.version(),font:'FoliaParityBidi-Bold.ttf',
   fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityBidi-Bold.ttf'))).digest('hex'),viewport:[800,600],
   clock:'Actual manual Motion; boundary left limits1us',layoutDiagnostic:'Original intact body Range bounds with transforms removed only after animation captures',metrics},null,2));
  await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/bidi-body-layout.tsv'),rows.join('\n')+'\n');
}finally {await browser.close();}
