import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const bodyOnly=process.argv.includes('--body-only');
const output=path.join(root,`build/verification/classic-colors-${bodyOnly?'body-':''}browser`);fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const palettes=[{name:'warm',primary:'#808080',accent:'#f2b45d'},{name:'cool',primary:'#415d8a',accent:'#91e8e1'}];
const checkpoints=[750,850,1000,1200,1500,1900,2100,2300,2800],frames=[];
try {
 for(const [scene,width,font] of [['baseline',800,'FoliaParityLatin-Bold.ttf'],['arabic',800,'NotoSansArabic-Bold.ttf'],['cjk',600,'FoliaParityCJK-Bold.otf']]) for(const palette of palettes) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  const query=new URLSearchParams({manual:'',primary:palette.primary,accent:palette.accent});
  if(scene!=='baseline')query.set('scene',scene);
  await page.goto('http://127.0.0.1:4173/?'+query);await page.waitForFunction(()=>window.ready===true);
  if(bodyOnly)await page.addStyleTag({content:'[aria-hidden="true"] { visibility: hidden !important; }'});
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:281},(_,i)=>i*10),...events.filter(t=>t<=2800),...checkpoints])].sort((a,b)=>a-b);
  for(const tick of ticks) {
   await page.evaluate(t=>window.step(t),tick);
   if(!checkpoints.includes(tick))continue;
   const file=`${scene}-${palette.name}-${tick}ms.png`,png=await page.screenshot();
   if(!png.equals(await page.screenshot()))throw new Error('Unstable frozen reference '+file);
   fs.writeFileSync(path.join(output,file),png);
   const bodies=await page.locator('.inline-block > span.z-10').evaluateAll(els=>els.map(el=>({text:el.textContent,color:getComputedStyle(el).color,filter:getComputedStyle(el).filter})));
   const expectedBodies=tick===2800?0:scene==='cjk'?4:2;
   if(bodies.length!==expectedBodies)throw new Error(`Unexpected body count ${file}: ${bodies.length} != ${expectedBodies}`);
   frames.push({scene,width,font,palette:palette.name,primary:palette.primary,accent:palette.accent,time:tick,file,bodies,
    sha256:createHash('sha256').update(png).digest('hex')});
  }
  if(errors.length)throw new Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
  clock:'Original Classic/Motion, frozen manual timing; strict-passed epsilon and 10ms ticks as existing oracle',
  density:1,height:600,background:'#162435',bodyOnly,fonts:[...new Set(frames.map(f=>f.font))].map(font=>({font,
   sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',font))).digest('hex')})),frames},null,2));
 console.log(output);
 if(!bodyOnly) {
  const rows=['# Actual pinned Classic computed body color; scene\tpalette\ttimeMs\twordIndex\tr\tg\tb'];
  for(const frame of frames) frame.bodies.forEach((body,index)=>{
   const rgb=/^rgb\((\d+), (\d+), (\d+)\)$/.exec(body.color);
   if(!rgb)throw new Error('Unexpected original body color '+body.color);
   rows.push([frame.scene,frame.palette,frame.time,index,...rgb.slice(1)].join('\t'));
  });
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/body-colors.tsv'),rows.join('\n')+'\n');
 }
}finally{await browser.close();}
