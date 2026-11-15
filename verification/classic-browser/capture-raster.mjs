import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/raster-upstream');fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
  const page=await browser.newPage({viewport:{width:600,height:400},deviceScaleFactor:1});
  for(const pose of ['identity','transform','original']) {
    await page.goto(`http://127.0.0.1:4173/raster.html?${pose==='transform'?'transform':pose==='original'?'original':''}`);
    await page.waitForSelector('.z-10');
    await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
    const styles=await page.locator('.z-10').evaluate(el=>{
      const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';el.appendChild(marker);
      const baseline=marker.getBoundingClientRect().top;marker.remove();
      const s=getComputedStyle(el);return {font:s.font,rect:el.getBoundingClientRect().toJSON(),baseline,lineHeight:s.lineHeight,
        fontSynthesis:s.fontSynthesis,textRendering:s.textRendering,fontSmoothing:s.webkitFontSmoothing};
    });
    await page.screenshot({path:path.join(output,`${pose}.png`),omitBackground:true});
    fs.writeFileSync(path.join(output,`${pose}.json`),JSON.stringify({revision,browser:browser.version(),viewport:[600,400],styles},null,2));
  }
} finally {await browser.close();}
