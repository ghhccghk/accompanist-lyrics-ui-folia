import fs from 'node:fs';
import path from 'node:path';
import {chromium} from 'playwright';
import {checkReference,root,revision} from './reference.mjs';
checkReference();
const rows=[`# actual original ${revision}; untransformed computed CSS row dimensions at 1500ms`,
 '# mode\tscene\twidth\trow\ttextB64\tfont\twidthCss\theightCss'];
const browser=await chromium.launch({headless:true});
try {
 for(const [mode,probability,split] of [['normal',0,.75],['tilt',1,.75],['single',1,0]])
 for(const scene of ['wrap','arabic'])for(const width of [380,800]) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  await page.addInitScript(()=>{delete Element.prototype.animate});
  await page.goto(`http://127.0.0.1:4173/?manual&theme=tilt&scene=${scene}&tilt=${probability}&split=${split}`);
  await page.waitForFunction(()=>window.ready);
  for(let ms=0;ms<=1500;ms+=10)await page.evaluate(t=>window.step(t),ms);
  const measured=await page.locator('div.whitespace-nowrap').evaluateAll(es=>es.map(el=>{
   const s=getComputedStyle(el);return [el.textContent.replaceAll('\u00a0',' '),s.fontSize,s.width,s.height];
  }));
  measured.forEach((r,i)=>rows.push([mode,scene,width,i,Buffer.from(r[0]).toString('base64'),...r.slice(1).map(parseFloat)].join('\t')));
  await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/tilt-fixtures/mounted-geometry.tsv'),rows.join('\n')+'\n');
 console.log(rows.join('\n'));
}finally{await browser.close()}
