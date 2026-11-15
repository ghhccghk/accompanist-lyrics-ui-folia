import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {root,revision,checkReference} from './reference.mjs';
checkReference();
const browser=await chromium.launch({headless:true});
const output=path.join(root,'build/verification/classic-manual-upstream');
fs.mkdirSync(output,{recursive:true});
const numeric=[`# original mounted Classic Motion values and DOM paint, manual timing; pinned ${revision}`,
  '# mode\tmediaMs\tlineStartMs\twordIndex\ta\tb\tc\td\tx\ty\topacity\tbodyBlur\tr\tg\tb\tlineScale\tlineOpacity\tlineBlur\tglyphOpacities\tscene'];
try {
  for(const [scene,width] of [['',800],['arabic',800],['wrap',380],['group',600],['cluster',800],['fast',800],['instant',800],['cjk',600],['whitespace',800]]) {
  for(const avoid of [false,true]) {
    const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
    const errors=[];page.on('pageerror',e=>errors.push(e.message));
    await page.addInitScript(()=>{delete Element.prototype.animate;});
    const epoch=new Date('2026-01-01T00:00:00Z');
    await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
    await page.goto(`http://127.0.0.1:4173/?manual${avoid?'&avoid':''}${scene?'&scene='+scene:''}`);
    await page.waitForFunction(()=>window.ready===true);
    const checkpoints=scene?[700,750,800,850,900,1000,1200,1500,1900,2100,2300,2800,3000]:[0,700,750,850,1000,1200,1300,1500,1800,1900,2100,2220,2300,2500,3000,3200,3400,3700,4100,4400,4700];
    const events=await page.evaluate(()=>window.eventTimes);
    const ticks=[...new Set([...Array.from({length:Math.max(...checkpoints)/10+1},(_,i)=>i*10),...events,...checkpoints])].sort((a,b)=>a-b);
    const frames=[];
    for(const tick of ticks) {
      await page.evaluate(t=>window.step(t),tick);
      if(checkpoints.includes(tick)) {
        const mode=avoid?'avoid':'default';
        await page.screenshot({path:path.join(output,`${scene?scene+'-':''}${mode}-${tick}ms.png`)});
        const words=await page.locator('.inline-block').evaluateAll(els=>els.map(el=>{
          const style=getComputedStyle(el),body=getComputedStyle(el.querySelector('.z-10'));
          const m=new DOMMatrix(style.transform),p=getComputedStyle(el.parentElement),lineMatrix=new DOMMatrix(p.transform);
          // DOM matrix serialization has only six significant digits. Read the actual
          // original Motion values rather than relaxing the numeric comparison tolerance.
          const values=window.motionValues(el),scale=values.scale??1,angle=(values.rotate??0)*Math.PI/180;
          const parent=window.motionValues(el.parentElement);
          return {text:el.textContent,transform:style.transform,opacity:style.opacity,filter:body.filter,debug:window.motionDebug(el),
            color:body.color,lineOpacity:p.opacity,lineTransform:p.transform,lineFilter:p.filter,
            numeric:[Number(el.parentElement.getAttribute('data-overlap-fit-line'))*1000,
              Array.from(el.parentElement.children).indexOf(el),Math.cos(angle)*scale,Math.sin(angle)*scale,
              -Math.sin(angle)*scale,Math.cos(angle)*scale,values.x??m.e,values.y??m.f,values.opacity??Number(style.opacity),
              parseFloat(body.filter.replace('blur(',''))||0,...body.color.match(/[\d.]+/g).map(Number).slice(0,3),
              parent.scale??lineMatrix.a,parent.opacity??Number(p.opacity),parseFloat(p.filter.replace('blur(',''))||0,
              Array.from(el.querySelectorAll('[aria-hidden] > span')).map(g=>getComputedStyle(g).opacity).join(',')]};
        }));
        frames.push({mediaMs:tick,words});
        words.forEach(word=>numeric.push([mode,tick,...word.numeric,scene||'baseline'].join('\t')));
      }
    }
    if(errors.length)throw new Error(errors.join('\n'));
    fs.writeFileSync(path.join(output,`${scene?scene+'-':''}${avoid?'avoid':'default'}-frames.json`),JSON.stringify({revision,browser:browser.version(),
      clock:'Original Motion manual timing/frameSteps; frozen browser clock; boundary events include strict-passed epsilon .001ms, JS generator rounds elapsed milliseconds.',
      viewport:[width,600],font:scene==='arabic'?'NotoSansArabic-Bold.ttf':scene==='cjk'?'FoliaParityCJK-Bold.otf':'FoliaParityLatin-Bold.ttf',frames},null,2));
    await page.close();
  }
  }
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/browser-motion.tsv'),numeric.join('\n')+'\n');
} finally {await browser.close();}
