// Real original AnimatePresence: retain every line until its exit finishes.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/classic-dense-upstream');fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
 await page.addInitScript(()=>{delete Element.prototype.animate;});
 const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
 await page.goto('http://127.0.0.1:4173/?manual&dense');await page.waitForFunction(()=>window.ready);
 const times=[801,901,1001,1051,1101,1201,1301];
 const events=await page.evaluate(()=>window.eventTimes);
 const ticks=[...new Set([...Array.from({length:141},(_,i)=>i*10),...events,...times])].filter(t=>t<=1400).sort((a,b)=>a-b);
 const rows=[];
 for(const tick of ticks) {
  await page.evaluate(t=>window.step(t),tick);
  if(!times.includes(tick))continue;
  const lines=await page.locator('[data-overlap-fit-line]').evaluateAll(es=>es.map(el=>({
   startMs:Math.round(Number(el.getAttribute('data-overlap-fit-line'))*1000),
   text:el.textContent,values:window.motionValues(el)})));
  rows.push({mediaMs:tick,lines});
  await page.screenshot({path:path.join(output,`${tick}ms.png`)});
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
  viewport:[800,600],font:'FoliaParityLatin-Bold.ttf',scene:'D00..D29, starts700+10*i ms, duration1ms',rows},null,2));
 const table=[`# Original classic AnimatePresence, ${revision}`,
  '# Visible opacity>0 only; zero-alpha nodes can remain mounted until the whole exit batch completes',
  '# mediaMs\tlineStartsMs\tlineOpacities'];
 for(const row of rows) {
  const visible=row.lines.filter(l=>(l.values.opacity??1)>0);
  table.push([row.mediaMs,visible.map(l=>l.startMs).join(',')||'-',visible.map(l=>l.values.opacity??1).join(',')||'-'].join('\t'));
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/dense-exits.tsv'),table.join('\n')+'\n');
 console.log(rows.map(r=>[r.mediaMs,r.lines.length,r.lines.map(l=>l.startMs)]));
}finally {await browser.close();}
