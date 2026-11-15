import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const directory=path.join(root,'build/verification/halo-upstream');
const fixtures=path.join(root,'verification/classic-fixtures/halo');
fs.mkdirSync(directory,{recursive:true});
fs.mkdirSync(fixtures,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
  const page=await browser.newPage({viewport:{width:320,height:320},deviceScaleFactor:1});
  await page.goto('http://127.0.0.1:4173/halo.html');
  await page.waitForSelector('[aria-hidden] > span');
  const glyph=page.locator('[aria-hidden] > span');
  for(const opacity of [1,.5,.1]) {
    await glyph.evaluate((g,o)=>g.style.setProperty('opacity',String(o),'important'),opacity);
    await page.screenshot({path:path.join(directory,`halo-${opacity}.png`),omitBackground:true});
    fs.copyFileSync(path.join(directory,`halo-${opacity}.png`),path.join(fixtures,`halo-${opacity}.png`));
  }
  const style=await glyph.evaluate(g=>({shadow:getComputedStyle(g).textShadow,rect:g.getBoundingClientRect().toJSON()}));
  // A positive sub-threshold sigma selects Chromium's shadow mask raster path
  // without spreading coverage. Ordinary foreground text uses different coverage.
  for (const radius of [.01,20,40]) {
    await glyph.evaluate((g,r)=>{
      g.style.setProperty('text-shadow',`0 0 ${r}px #fff`,'important');
      g.style.setProperty('color','transparent','important');
      g.style.setProperty('opacity','1','important');
    },radius);
    const name=radius===.01?'shadow-source.png':`single-${radius}.png`;
    await page.screenshot({path:path.join(directory,name),omitBackground:true});
    fs.copyFileSync(path.join(directory,name),path.join(fixtures,name));
  }
  await glyph.evaluate(g=>{
    g.style.setProperty('text-shadow','none','important');
    g.style.setProperty('color','#fff','important');
    g.style.setProperty('opacity','1','important');
  });
  await page.screenshot({path:path.join(directory,'source.png'),omitBackground:true});
  fs.copyFileSync(path.join(directory,'source.png'),path.join(fixtures,'source.png'));
  fs.writeFileSync(path.join(directory,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
    font:'FoliaParityLatin-Bold.ttf',viewport:[320,320],density:1,style,
    isolation:'Original GlowWord. Identity layout, body hidden, glyph opacity forced. Positive .01px shadow captures the shadow glyph coverage; ordinary foreground source.png is retained separately. Single 20/40px shadows isolate kernel before original combined opacity comparison.'},null,2));
  fs.copyFileSync(path.join(directory,'conditions.json'),path.join(fixtures,'conditions.json'));
} finally {await browser.close();}
