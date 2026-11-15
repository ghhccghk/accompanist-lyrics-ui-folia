// Observe fractional untransformed line boxes on the unchanged upstream classic.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/line-centers-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});
const rows=[`# actual original line CSS height and world center ${revision}`,
 '# intensity\tscene\twidth\theight\tavoid\tlineHeight\tlineTop\tcenterX\tcenterY\tavailableHeight'];
try {
 for(const intensity of ['calm','normal','chaotic']) for(const [scene,width] of [['baseline',800],['wrap',380]]) for(const height of [600,601,602]) for(const avoid of [false,true]) {
  const page=await browser.newPage({viewport:{width,height}});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&intensity=${intensity}${scene==='wrap'?'&scene=wrap':''}${avoid?'&avoid':''}`);
  await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  for(const tick of [...new Set([...Array.from({length:151},(_,i)=>i*10),...events.filter(t=>t<=1500)])].sort((a,b)=>a-b))await page.evaluate(t=>window.step(t),tick);
  const v=await page.locator('[data-overlap-fit-line]').evaluate(el=>{
   const r=el.getBoundingClientRect(),height=r.height/new DOMMatrix(getComputedStyle(el).transform).a;
   const stage=el.parentElement,s=getComputedStyle(stage);
   return [height,r.y+(r.height-height)/2,r.x+r.width/2,r.y+r.height/2,stage.clientHeight-parseFloat(s.paddingTop)-parseFloat(s.paddingBottom)];
  });
  await page.screenshot({path:path.join(out,`${intensity}-${scene}-${width}x${height}-${avoid?'avoid':'default'}.png`)});
  rows.push([intensity,scene,width,height,avoid,...v].join('\t'));await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/line-centers.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),density:1,mediaMs:1500,
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  note:'Actual unchanged Classic; same replay/events as intensity capture; no captions/chrome/breathing; observed original used line heights, centers and integer clientHeight budget.'},null,2));
}finally{await browser.close();}
