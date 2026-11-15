// Observe useOverlapFit on short viewports. No renderer or upstream mutations.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/short-stage-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});
const rows=[`# Actual original short stage layout ${revision}`,
 '# intensity\twidth\theight\tavoid\tavailableHeight\tlineHeight\tfirstWordFontPx'];
try {
 for(const intensity of ['calm','normal','chaotic']) for(const height of [100,150,180,200,240,300]) for(const avoid of [false,true]) {
  const page=await browser.newPage({viewport:{width:800,height}});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&intensity=${intensity}${avoid?'&avoid':''}`);
  await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  for(const tick of [...new Set([...Array.from({length:151},(_,i)=>i*10),...events.filter(t=>t<=1500)])].sort((a,b)=>a-b))await page.evaluate(t=>window.step(t),tick);
  const data=await page.locator('[data-overlap-fit-line]').evaluate(el=>{
   const area=el.parentElement,s=getComputedStyle(area),r=el.getBoundingClientRect();
   const word=el.querySelector('.will-change-transform');
   if(!word)throw new Error('Missing original word');
   return [area.clientHeight-parseFloat(s.paddingTop)-parseFloat(s.paddingBottom),
    r.height/new DOMMatrix(getComputedStyle(el).transform).a,parseFloat(getComputedStyle(word).fontSize)];
  });
  const file=`${intensity}-800x${height}-${avoid?'avoid':'default'}.png`;
  const png=await page.screenshot({path:path.join(out,file)});
  if(!png.equals(await page.screenshot()))throw new Error(`Paused frame changed: ${file}`);
  rows.push([intensity,800,height,avoid,...data].join('\t'));await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/short-stage.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),density:1,mediaMs:1500,
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  note:'Original Classic, normal replay and event boundaries; every frame paused-identical. No captions/chrome/breathing.'},null,2));
}finally{await browser.close();}
