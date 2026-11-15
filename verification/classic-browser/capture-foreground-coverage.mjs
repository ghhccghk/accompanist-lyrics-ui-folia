// Same original GlowWord as the advance probe, with actual opaque surface backgrounds.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/foreground-coverage-browser');
fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:512,height:320},deviceScaleFactor:1});
 const frames=[];
 for(const size of [19.74,19.744,36,48,72]) for(const dark of [false,true]) {
  await page.goto('http://127.0.0.1:4173/raster.html?text=Synthetic&size='+size);
  await page.waitForSelector('.z-10');await page.evaluate(()=>document.fonts.ready);
  await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
  await page.locator('.z-10').evaluate((el,dark)=>{
   const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';
   el.appendChild(marker);const baseline=marker.getBoundingClientRect().top;marker.remove();
   el.parentElement.parentElement.style.top=(128+191-baseline)+'px';
   document.body.style.backgroundColor=dark?'#162435':'#000';
  },dark);
  const file='synthetic-'+size+'-'+(dark?'dark':'black')+'.png';
  await page.screenshot({path:path.join(output,file)});
  frames.push({size,dark,file});
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
  viewport:[512,320],font:'FoliaParityLatin-Bold.ttf',
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  baseline:191,left:128,frames},null,2));
 console.log(output);
}finally {await browser.close();}
