import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {chromium} from 'playwright';
import {checkReference,root,revision} from './reference.mjs';
checkReference();
const folder=path.join(root,'build/verification/tilt-browser');fs.mkdirSync(folder,{recursive:true});
const out=[`# actual mounted original Tilt ${revision}; CSS transform transitions retain the original clock`,
 '# mode\tscene\twidth\tms\trow\ttextB64\ttilt\tfont\tleft\ttop\twidth\theight\topacity\ty\tscale'];
const chars=[`# actual original Tilt char Motion targets and computed transform`,
 '# mode\tscene\twidth\tms\trow\tchar\topacity\ty\tscale\tcomputedTransform'];
const times=[700,750,850,1000,1200,1500,1900,2100,2400,3000,3200,3500];
const modes=[['normal',0,.75],['tilt',1,.75],['single',1,0]];
const browser=await chromium.launch({headless:true});
try {
 for(const [mode,probability,split] of modes)for(const scene of ['wrap','arabic'])for(const width of [380,800]) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{delete Element.prototype.animate});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&theme=tilt&scene=${scene}&tilt=${probability}&split=${split}`);
  await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:351},(_,i)=>i*10),...events.filter(t=>t<=3500),...times])].sort((a,b)=>a-b);
  let previous=0;
  for(const t of ticks) {
   // Source CSS transform 60ms transitions use the browser's real animation
   // timeline; advance it along with manual Motion, never remove the CSS rule.
   if(t>previous)await page.clock.runFor(t-previous);
   await page.evaluate(t=>window.step(t),t);previous=t;
   if(!times.includes(t))continue;
   await page.screenshot({path:path.join(folder,`${mode}-${scene}-${width}-${t}ms.png`)});
   const rows=await page.locator('div.whitespace-nowrap').evaluateAll(els=>els.map(el=>{
    const style=getComputedStyle(el),m=window.motionValues(el);
    return {text:el.textContent.replaceAll('\u00a0',' '),tilt:style.fontStyle==='italic',font:parseFloat(style.fontSize),
      box:[el.offsetLeft,el.offsetTop,el.offsetWidth,el.offsetHeight],motion:[m.opacity??1,m.y??0,m.scale??1],
      chars:[...el.children].map(child=>{const m=window.motionValues(child);return [m.opacity??1,m.y??0,m.scale??1,getComputedStyle(child).transform]})};
   }));
   rows.forEach((row,i)=>{
    out.push([mode,scene,width,t,i,Buffer.from(row.text).toString('base64'),row.tilt,row.font,...row.box,...row.motion].join('\t'));
    row.chars.forEach((v,c)=>chars.push([mode,scene,width,t,i,c,...v].join('\t')));
   });
  }
  if(errors.length)throw Error(errors.join('\n'));
  await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/tilt-fixtures/mounted-rows.tsv'),out.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/tilt-fixtures/mounted-chars.tsv'),chars.join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,browser:browser.version(),widths:[380,800],height:600,dpr:1,times,
   modes,fonts:['FoliaParityLatin-Bold.ttf','NotoSansArabic-Bold.ttf'].map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',name))).digest('hex')})),
   note:'Actual unchanged Tilt source and pretext 0.0.9 from original lock. Manual Motion plus advancing browser clock, original CSS transitions retained. Shell omits background/subtitles. Not a performance measurement.'},null,2));
 console.log(`Saved ${modes.length*2*2*times.length} original frames, ${out.length-2} rows, ${chars.length-2} character states`);
}finally{await browser.close()}
