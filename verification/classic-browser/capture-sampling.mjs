// Phase and compositor transform diagnostic on the unchanged original GlowWord.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/sampling-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const frames=[];
 for(const dpr of [1,2]) {
 const page=await browser.newPage({viewport:{width:600,height:400},deviceScaleFactor:dpr});
 for(const kind of ['layout','motion']) for(const phase of [0,.25,.5,.75]) for(const transform of [false,true]) {
  const pose=transform?'transform':'identity';
  await page.goto(`http://127.0.0.1:4173/raster.html?size=48&text=Amber&${kind==='layout'?'phase':'motionphase'}=${phase}${transform?'&transform':''}`);
  await page.waitForSelector('.z-10');
  await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
  const metrics=await page.locator('.z-10').evaluate(el=>({bounds:el.getBoundingClientRect().toJSON(),font:getComputedStyle(el).font,
   transform:getComputedStyle(el.parentElement).transform,layoutWidth:parseFloat(getComputedStyle(el).width),layoutHeight:parseFloat(getComputedStyle(el).height)}));
  const name=`dpr${dpr}-${kind}-${phase}-${pose}.png`;
  await page.screenshot({path:path.join(out,name),omitBackground:true});
  frames.push({dpr,kind,phase,pose,file:name,metrics});
 }
 await page.close();
 }
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),font:'FoliaParityLatin-Bold.ttf',
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),viewport:[600,400],
  origin:[128,128],scale:1.6,rotationDegrees:-4,background:'transparent',timing:'Static original body; no glow or animation variables',frames},null,2));
}finally {await browser.close();}
