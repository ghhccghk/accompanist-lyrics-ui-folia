import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
// Primitive CSS-filter diagnostic: same sigma units used by original body/line filters.
const directory=path.join(root,'build/verification/css-blur-upstream');fs.mkdirSync(directory,{recursive:true});
const rows=[`# CSS blur primitive; Chromium pinned by runtime, Folia ${revision}`,'# sigma\talphaStdDev\talphaMass'];
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:192,height:192},deviceScaleFactor:1});
 for(const sigma of [.25,.5,.6,1,10,20]) {
  await page.setContent(`<html><body style="margin:0;background:transparent"><div style="position:absolute;left:80px;top:80px;width:32px;height:32px;background:white;filter:blur(${sigma}px)"></div></body></html>`);
  const png=await page.screenshot({path:path.join(directory,`${sigma}.png`),omitBackground:true});
  if(sigma===10||sigma===20) {
   const fixture=path.join(root,'verification/classic-fixtures/css-blur');fs.mkdirSync(fixture,{recursive:true});
   fs.writeFileSync(path.join(fixture,`${sigma}.png`),png);
  }
  const values=await page.evaluate(async data=>{
   const bytes=Uint8Array.from(atob(data),c=>c.charCodeAt(0));const image=await createImageBitmap(new Blob([bytes],{type:'image/png'}));
   const canvas=document.createElement('canvas');canvas.width=192;canvas.height=192;const context=canvas.getContext('2d');context.drawImage(image,0,0);
   const pixels=context.getImageData(0,0,192,192).data;let mass=0,moment=0;
   for(let i=0;i<192*192;i++){const a=pixels[i*4+3];mass+=a;moment+=a*Math.pow(i%192-95.5,2);}
   image.close();return [Math.sqrt(moment/mass),mass];
  },png.toString('base64'));
  rows.push([sigma,...values].join('\t'));
 }
 fs.writeFileSync(path.join(root,'verification/classic-fixtures/css-blur.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(directory,'conditions.json'),JSON.stringify({browser:browser.version(),viewport:[192,192],density:1,primitive:'32px white square at (80,80), transparent exterior, CSS blur(sigma)',revision},null,2));
}finally{await browser.close();}
