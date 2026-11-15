import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const grayscale=process.argv.includes('--grayscale');
const output=path.join(root,`build/verification/subtitle-${grayscale?'grayscale-':''}upstream`);fs.mkdirSync(output,{recursive:true});
const rows=[`# Mounted original subtitle DOM and Motion values; pinned ${revision}`,'# width\tlayout\tmediaMs\touterAlpha\touterY\tinnerAlpha\tinnerY'];
const layouts=[`# Actual original subtitle row geometry; pinned ${revision}`,'# width\tlayout\tmediaMs\trow\ttext\ttop\theight\tlastBaseline'];
const ink=[`# Original subtitle ink bounds, fixed ${revision}`,'# width\tlayout\tmediaMs\trow\ttop\tbottom'];
const browser=await chromium.launch({headless:true,args:grayscale?['--disable-lcd-text']:[]});
try {
 for(const width of [800,380]) for(const layout of ['translation','current+translation+romanization']) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/subtitle.html?paint&layout=${encodeURIComponent(layout)}`);await page.waitForFunction(()=>window.ready===true);
  const times=[0,50,120,240,700,725,750,800,850,1000,1500,2500,3200,3250,3350,3500,4800,5000,5050,5150,5500];
  const observations=[];
  for(let tick=0;tick<=5500;tick+=5) {
   await page.evaluate(t=>window.step(t),tick);
   if(times.includes(tick)) {
    const png=await page.screenshot({path:path.join(output,`${width}-${layout}-${tick}ms.png`)});
    const data=await page.locator('.z-20').evaluate(el=>{
      const outer=window.motionValues(el),innerEl=el.querySelector('.max-w-4xl'),inner=innerEl?window.motionValues(innerEl):{};
      return {outer,inner,elements:[el,...el.querySelectorAll('*')].map(e=>({className:e.className,rect:e.getBoundingClientRect().toJSON(),font:getComputedStyle(e).font})),rows:Array.from(el.querySelectorAll('[data-subtitle-track],p')).map(row=>{
        const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';row.appendChild(marker);
        const baseline=marker.getBoundingClientRect().top;marker.remove();
        return {text:row.textContent,source:row.getAttribute('data-subtitle-track'),baseline,rect:row.getBoundingClientRect().toJSON(),font:getComputedStyle(row).font,opacity:getComputedStyle(row).opacity};
      })};
    });
    observations.push({mediaMs:tick,...data});rows.push([width,layout,tick,data.outer.opacity??1,data.outer.y??0,data.inner.opacity??1,data.inner.y??0].join('\t'));
    if([1500,2500,3500,5500].includes(tick))data.rows.forEach((r,i)=>layouts.push([width,layout,tick,i,r.text,r.rect.top,r.rect.height,r.baseline].join('\t')));
    if([1500,2500,3500,5500].includes(tick)) {
     const boxes=await page.evaluate(async ({png,rows})=>{
      const bytes=Uint8Array.from(atob(png),c=>c.charCodeAt(0));const image=await createImageBitmap(new Blob([bytes],{type:'image/png'}));
      const canvas=document.createElement('canvas');canvas.width=innerWidth;canvas.height=innerHeight;const context=canvas.getContext('2d');context.drawImage(image,0,0);
      const pixels=context.getImageData(0,0,innerWidth,innerHeight).data;image.close();
      return rows.map(r=>{let top=innerHeight,bottom=-1;
       for(let y=Math.max(0,Math.floor(r.rect.top));y<Math.min(innerHeight,Math.ceil(r.rect.bottom));y++) for(let x=0;x<innerWidth;x++) {
        const i=(y*innerWidth+x)*4;if(Math.max(pixels[i]-22,pixels[i+1]-36,pixels[i+2]-53)>20){top=Math.min(top,y);bottom=Math.max(bottom,y);}
       }return [top,bottom];});
     },{png:png.toString('base64'),rows:data.rows});
     boxes.forEach((b,i)=>ink.push([width,layout,tick,i,...b].join('\t')));
    }
   }
  }
  fs.writeFileSync(path.join(output,`${width}-${layout}.json`),JSON.stringify({revision,browser:browser.version(),antialias:grayscale?'--disable-lcd-text':'browser default',font:'FoliaParityLatin-Bold.ttf',observations},null,2));
  await page.close();
 }
 if(!grayscale) fs.writeFileSync(path.join(root,'verification/classic-fixtures/subtitle-motion.tsv'),rows.join('\n')+'\n');
 if(!grayscale) fs.writeFileSync(path.join(root,'verification/classic-fixtures/subtitle-layout.tsv'),layouts.join('\n')+'\n');
 if(!grayscale) fs.writeFileSync(path.join(root,'verification/classic-fixtures/subtitle-ink.tsv'),ink.join('\n')+'\n');
}finally {await browser.close();}
