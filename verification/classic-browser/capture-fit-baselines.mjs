// Original GlowWord baseline at the fitted sizes observed in short-stage.tsv.
// Identity pose isolates inline layout from animation and glyph coverage.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/fit-baselines-upstream');fs.mkdirSync(out,{recursive:true});
const sizes=[...new Set(fs.readFileSync(path.join(root,'verification/classic-fixtures/short-stage.tsv'),'utf8')
 .split(/\r?\n/).filter(r=>r&&!r.startsWith('#')).map(r=>Number(r.split('\t')[6])))].sort((a,b)=>a-b);
const browser=await chromium.launch({headless:true});
const rows=[`# Actual original identity GlowWord CSS baseline ${revision}`,
 '# fontSize\twordHeight\tbaselineOffset\tcanvasAscent\tcanvasDescent'];
try {
 const origins=[];
 for(const intensity of ['calm','normal','chaotic']) {
  const fixture=await browser.newPage({viewport:{width:380,height:600}});
  await fixture.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await fixture.clock.install({time:epoch});await fixture.clock.pauseAt(epoch);
  await fixture.goto(`http://127.0.0.1:4173/?manual&avoid&scene=wrap&intensity=${intensity}`);
  await fixture.waitForFunction(()=>window.ready);
  const events=await fixture.evaluate(()=>window.eventTimes);
  for(const tick of [...new Set([...Array.from({length:151},(_,i)=>i*10),...events.filter(t=>t<=1500)])].sort((a,b)=>a-b))await fixture.evaluate(t=>window.step(t),tick);
  const originalFrame=await fixture.screenshot();
  const word=fixture.locator('[data-overlap-fit-line] .will-change-transform').first();
  const originalMetrics=await word.evaluate(el=>{
   const body=el.querySelector('.z-10'),s=getComputedStyle(body);
   const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';body.appendChild(marker);
   // offsetTop is layout-only. Clearing the real word's transform to measure
   // a rect would change its compositing history even after restoration.
   if(marker.offsetParent!==body)throw new Error('Unexpected baseline offset parent');
   const baseline=marker.offsetTop;marker.remove();
   const ctx=document.createElement('canvas').getContext('2d');ctx.font=`${s.fontWeight} ${s.fontSize} ${s.fontFamily}`;
   const m=ctx.measureText(body.textContent);return {size:parseFloat(s.fontSize),font:s.font,fontFamily:s.fontFamily,offsetHeight:body.offsetHeight,baseline,ascent:m.fontBoundingBoxAscent,descent:m.fontBoundingBoxDescent};
  });
  if(!originalFrame.equals(await fixture.screenshot()))throw new Error('Original classic probe changed restored frame');
  sizes.push(originalMetrics.size);origins.push({intensity,scene:'wrap',width:380,height:600,...originalMetrics});await fixture.close();
 }
 const page=await browser.newPage({viewport:{width:600,height:400}});const probes=[];
 for(const size of [...new Set(sizes)].sort((a,b)=>a-b)) {
  await page.goto(`http://127.0.0.1:4173/raster.html?font=latin&size=${size}&text=Amber`);
  await page.waitForSelector('.z-10');
  await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
  const untouched=await page.screenshot({path:path.join(out,`${size}px-untouched.png`)});
  const metrics=await page.locator('.z-10').evaluate(el=>{
   const r=el.getBoundingClientRect(),s=getComputedStyle(el);
   const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';el.appendChild(marker);
   const baseline=marker.getBoundingClientRect().top-r.y;
   const ctx=document.createElement('canvas').getContext('2d');ctx.font=`${s.fontWeight} ${s.fontSize} ${s.fontFamily}`;
   const m=ctx.measureText('Amber');return [r.height,baseline,m.fontBoundingBoxAscent,m.fontBoundingBoxDescent];
  });
  const marked=await page.screenshot({path:path.join(out,`${size}px-marker.png`)});
  const markedHeight=await page.locator('.z-10').evaluate(el=>el.getBoundingClientRect().height);
  await page.locator('.z-10').evaluate(el=>el.lastElementChild.remove());
  const restored=await page.screenshot({path:path.join(out,`${size}px-restored.png`)});
  if(!untouched.equals(restored))throw new Error('Marker removal did not restore frame');
  probes.push({size,unmodifiedHeight:metrics[0],markedHeight,markerChangesPixels:!untouched.equals(marked)});
  rows.push([size,...metrics].join('\t'));
  await page.screenshot({path:path.join(out,`${size}px.png`)});
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/fit-baselines.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
  origins,probes,note:'Actual original GlowWord at observed fitted Classic CSS sizes; identity variants, glow hidden; no source changes. Marker probe records whether measuring changes the actual frame.'},null,2));
}finally{await browser.close();}
