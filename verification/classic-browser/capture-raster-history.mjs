// Same final pose, actual GlowWord, distinct real compositing histories.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const out=path.join(root,'build/verification/raster-history-upstream');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const frames=[];
 for(const dpr of [1,2]) for(const pose of ['identity','transform']) for(const history of [null,.3,1.6]) {
  const page=await browser.newPage({viewport:{width:600,height:400},deviceScaleFactor:dpr});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/raster.html?size=48&text=Amber${pose==='transform'?'&transform':''}${history===null?'':'&history='+history}`);
  await page.waitForSelector('.z-10');await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
  const name=`dpr${dpr}-${pose}-${history===null?'static':'from'+history}`;
  if(history!==null) {
   await page.screenshot({path:path.join(out,`${name}-initial.png`),omitBackground:true});
   await page.evaluate(()=>window.rasterStep());
   await page.clock.runFor(1000);
  }
  await page.screenshot({path:path.join(out,`${name}.png`),omitBackground:true});
  const metrics=await page.locator('.z-10').evaluate(el=>({bounds:el.getBoundingClientRect().toJSON(),transform:getComputedStyle(el.parentElement).transform,
   opacity:getComputedStyle(el.parentElement).opacity,color:getComputedStyle(el).color,blur:getComputedStyle(el).filter,font:getComputedStyle(el).font}));
  await page.clock.runFor(1000);
  await page.screenshot({path:path.join(out,`${name}-repeat.png`),omitBackground:true});
  frames.push({dpr,pose,history,file:name+'.png',repeat:name+'-repeat.png',metrics});await page.close();
 }
 fs.writeFileSync(path.join(out,'conditions.json'),JSON.stringify({revision,browser:browser.version(),font:'FoliaParityLatin-Bold.ttf',fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),viewport:[600,400],
  clock:'Frozen clock before navigation; first original layer paint at initial scale; real Motion JS fallback active transition300ms then1000ms virtual clock; body opaque white/no blur, glow hidden',frames},null,2));
}finally {await browser.close();}
