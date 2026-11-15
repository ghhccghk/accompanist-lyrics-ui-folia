// Measure real pinned GlowWord + browser font cache, without synthetic CSS substitutes.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/font-advances-browser');fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:600,height:400},deviceScaleFactor:1});
 const rows=[];
 for(const size of [19.74,19.744,36,48,72]) {
  await page.goto(`http://127.0.0.1:4173/raster.html?text=Synthetic&size=${size}`);
  await page.waitForSelector('.z-10');await page.evaluate(()=>document.fonts.ready);
  const measurement=await page.locator('.z-10').evaluate(el=>{
   const s=getComputedStyle(el),text=el.textContent;
   const c=document.createElement('canvas').getContext('2d');c.font=s.font;
   const prefixes=Array.from({length:text.length+1},(_,i)=>c.measureText(text.slice(0,i)).width);
   return {text,font:s.font,cssSize:s.fontSize,boxWidth:el.getBoundingClientRect().width,
    total:c.measureText(text).width,advances:prefixes.slice(1).map((x,i)=>x-prefixes[i])};
  });
  rows.push({size,...measurement});
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
  viewport:[600,400],density:1,font:'FoliaParityLatin-Bold.ttf',rows},null,2));
 console.log(output);
}finally {await browser.close();}
