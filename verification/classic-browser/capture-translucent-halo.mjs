import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';

// Actual original shadow targets. Ink alpha belongs to each shadow, span opacity to the pair.
checkReference();
const output=path.join(root,'verification/classic-fixtures/halo-translucent');
fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const frames=[];
try {
 for(const reveal of ['normal','fast','instant']) for(const alpha of [0,64,128,192,255]) {
  const page=await browser.newPage({viewport:{width:320,height:320},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  const ink='#91e8e1'+alpha.toString(16).padStart(2,'0');
  await page.goto('http://127.0.0.1:4173/halo.html?'+new URLSearchParams({ink,reveal}));
  await page.waitForFunction(()=>window.ready===true);
  const glyph=page.locator('[aria-hidden] > span');
  for(const opacity of [1,.5,.1]) {
   await glyph.evaluate((element,value)=>element.style.setProperty('opacity',String(value),'important'),opacity);
   const file=`${reveal}-a${alpha}-o${opacity}.png`;
   const png=await page.screenshot({omitBackground:true});
   if(!png.equals(await page.screenshot({omitBackground:true})))throw new Error('Unstable original '+file);
   fs.writeFileSync(path.join(output,file),png);
   frames.push({reveal,alpha,ink,opacity,file,sha256:createHash('sha256').update(png).digest('hex'),
    style:await glyph.evaluate(el=>({color:getComputedStyle(el).color,shadow:getComputedStyle(el).textShadow,opacity:getComputedStyle(el).opacity}))});
  }
  // Single actual shadows distinguish per-shadow ink alpha from group opacity.
  for(const radius of reveal==='normal'?[20,40]:reveal==='fast'?[18,32]:[14,24]) {
   await glyph.evaluate((element,{ink,radius})=>{
    element.style.setProperty('opacity','1','important');
    element.style.setProperty('text-shadow',`0 0 ${radius}px ${ink}`,'important');
   },{ink,radius});
   const file=`${reveal}-a${alpha}-single-${radius}.png`;
   const png=await page.screenshot({omitBackground:true});
   if(!png.equals(await page.screenshot({omitBackground:true})))throw new Error('Unstable single '+file);
   fs.writeFileSync(path.join(output,file),png);
   frames.push({reveal,alpha,ink,opacity:1,singleRadius:radius,file,sha256:createHash('sha256').update(png).digest('hex')});
  }
  if(errors.length)throw new Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),width:320,height:320,density:1,
  font:'FoliaParityLatin-Bold.ttf',fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  isolation:'Actual GlowWord and wordGlow waiting shadow targets; body hidden, identity word pose, only span opacity forced. No replacement blur/paint.',frames},null,2));
 console.log(output);
} finally {await browser.close();}
