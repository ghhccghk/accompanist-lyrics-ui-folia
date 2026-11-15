import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/raster-upstream');fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const metrics=[`# original GlowWord static body, pinned ${revision}`, '# scene\tfontFile\tfontSize\ttext\twidth\theight\tbaselineOffset'];
try {
  const page=await browser.newPage({viewport:{width:600,height:400},deviceScaleFactor:1});
  for(const [scene,font,size,text] of [['','latin',48,'Amber'],['latin36','latin',36,'Amber'],['latin72','latin',72,'Amber'],['latin-small','latin',19.74,'Amber'],['arabic48','arabic',48,'مرحبا'],['arabic36','arabic',36,'مرحبا'],['cjk36','cjk',36,'合成歌词'],['cjk48','cjk',48,'合成歌词']]) {
  for(const pose of (scene?['identity','transform']:['identity','transform','original'])) {
    await page.goto(`http://127.0.0.1:4173/raster.html?font=${font}&size=${size}&text=${encodeURIComponent(text)}&${pose==='transform'?'transform':pose==='original'?'original':''}`);
    await page.waitForSelector('.z-10');
    await page.locator('[aria-hidden]').evaluate(el=>el.style.display='none');
    const styles=await page.locator('.z-10').evaluate(el=>{
      const marker=document.createElement('span');marker.style.cssText='display:inline-block;width:0;height:0';el.appendChild(marker);
      const baseline=marker.getBoundingClientRect().top;marker.remove();
      const s=getComputedStyle(el);return {font:s.font,rect:el.getBoundingClientRect().toJSON(),baseline,lineHeight:s.lineHeight,
        fontSynthesis:s.fontSynthesis,textRendering:s.textRendering,fontSmoothing:s.webkitFontSmoothing};
    });
    const name=`${scene?scene+'-':''}${pose}`;
    await page.screenshot({path:path.join(output,`${name}.png`),omitBackground:true});
    fs.writeFileSync(path.join(output,`${name}.json`),JSON.stringify({revision,browser:browser.version(),viewport:[600,400],styles},null,2));
    if(pose==='identity') metrics.push([scene||'baseline',font==='arabic'?'NotoSansArabic-Bold.ttf':font==='cjk'?'FoliaParityCJK-Bold.otf':'FoliaParityLatin-Bold.ttf',size,text,styles.rect.width,styles.rect.height,styles.baseline-128].join('\t'));
  }
  }
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/body-layout.tsv'),metrics.join('\n')+'\n');
} finally {await browser.close();}
