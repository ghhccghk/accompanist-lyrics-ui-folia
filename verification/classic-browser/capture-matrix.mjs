import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision} from './reference.mjs';
const browser=await chromium.launch({headless:true});
const output=path.join(root,'build/verification/classic-matrix-upstream');fs.mkdirSync(output,{recursive:true});
const layout=[`# pinned ${revision}; exact source DOM offset*; integer rounded`,
  '# scene\tmode\tviewportWidth\tviewportHeight\tindex\tleft\ttop\twidth\theight\tmargin\tlineHeight\trowGap\tpaddingTop\tpaddingBottom\tfontSize'];
try {
  for(const [scene,width,height] of [['arabic',800,600],['wrap',380,600],['group',600,600],['cluster',800,600],['fast',800,600],['instant',800,600]]) {
    for(const avoid of [false,true]) {
      const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:1});const errors=[];
      page.on('pageerror',e=>errors.push(e.message));
      await page.addInitScript(()=>{delete Element.prototype.animate;});
      await page.goto(`http://127.0.0.1:4173/?scene=${scene}${avoid?'&avoid':''}`);
      await page.waitForFunction(()=>window.ready===true);
      await page.clock.install();await page.clock.pauseAt(new Date(Date.now()+100));
      let media=0;
      for(const target of [700,750,800,850,900,1000,1200,1500,1900,2100,2300,2800,3000]) {
        while(media<target) {const next=Math.min(target,media+10);await page.evaluate(t=>window.step(t),next);await page.clock.runFor(next-media);media=next;}
        const mode=avoid?'avoid':'default';
        await page.screenshot({path:path.join(output,`${scene}-${mode}-${target}ms.png`)});
        if(target===750) {
          const metrics=await page.locator('.inline-block').evaluateAll(els=>els.map(el=>{
            const style=getComputedStyle(el),parent=getComputedStyle(el.parentElement);
            return [el.offsetLeft,el.offsetTop,el.offsetWidth,el.offsetHeight,parseFloat(style.marginInlineEnd),el.parentElement.clientHeight,
              parseFloat(parent.rowGap)||0,parseFloat(parent.paddingTop),parseFloat(parent.paddingBottom),parseFloat(style.fontSize)];
          }));
          metrics.forEach((v,i)=>layout.push([scene,mode,width,height,i,...v].join('\t')));
        }
      }
      if(errors.length)throw new Error(errors.join('\n'));
      await page.close();
    }
  }
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/browser-matrix-layout.tsv'),layout.join('\n')+'\n');
  fs.writeFileSync(path.join(output,'conditions.txt'),`Pinned ${revision}; Chromium ${browser.version()}; JS fallback, 10ms monotonic time; fonts in verification/assets; fixed scene TSV; no subtitles/chrome/waiting text; breathing disabled.\n`);
}finally{await browser.close();}
