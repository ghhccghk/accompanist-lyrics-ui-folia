// Same original GlowWord as the advance probe, with actual opaque surface backgrounds.
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const colors=process.argv.includes('--colors');
const output=path.join(root,'build/verification/foreground-coverage'+(colors?'-colors':'')+'-browser');
fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:512,height:320},deviceScaleFactor:1});
 const frames=[];
 const fonts=[{font:'latin',text:'Synthetic',asset:'FoliaParityLatin-Bold.ttf'},
  {font:'arabic',text:'مرحبا',asset:'NotoSansArabic-Bold.ttf'},
  {font:'cjk',text:'合成歌词',asset:'FoliaParityCJK-Bold.otf'}];
 const grid=colors?fonts.flatMap(f=>[19.74,48].flatMap(size=>['#ffffff','#808080','#f2b45d'].flatMap(ink=>
  ['#000000','#e8edf2'].map(surface=>({...f,size,ink,surface}))))):
  [19.74,19.744,36,48,72].flatMap(size=>[false,true].map(dark=>({...fonts[0],size,dark,ink:'#ffffff',surface:dark?'#162435':'#000000'})));
 for(const row of grid) {
  const {size,dark,text,font,ink,surface}=row;
  await page.goto('http://127.0.0.1:4173/raster.html?'+new URLSearchParams({text,size:String(size),font,ink}));
  await page.waitForSelector('.z-10');await page.evaluate(()=>document.fonts.ready);
  await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
  await page.locator('.z-10').evaluate((el,surface)=>{
   const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';
   el.appendChild(marker);const baseline=marker.getBoundingClientRect().top;marker.remove();
   el.parentElement.parentElement.style.top=(128+191-baseline)+'px';
   document.body.style.backgroundColor=surface;
  },surface);
  const file=colors?`${font}-${size}-${ink.slice(1)}-${surface.slice(1)}.png`:'synthetic-'+size+'-'+(dark?'dark':'black')+'.png';
  await page.screenshot({path:path.join(output,file)});
  // Capture twice without advancing media time. An empty/unstable oracle is not evidence.
  const repeat=await page.screenshot();
  if(!repeat.equals(fs.readFileSync(path.join(output,file))))throw new Error('Unstable reference '+file);
  frames.push(colors?{...row,file}:{size,dark,file});
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),
  viewport:[512,320],font:'FoliaParityLatin-Bold.ttf',
  fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  ...(colors?{fonts:fonts.map(f=>({...f,sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',f.asset))).digest('hex')}))}:{}),
  baseline:191,left:128,frames},null,2));
 console.log(output);
}finally {await browser.close();}
