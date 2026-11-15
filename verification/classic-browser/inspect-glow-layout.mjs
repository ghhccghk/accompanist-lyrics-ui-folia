import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';

// Inspect actual original inline spans after preserving the unchanged animated frame.
checkReference();
const output=path.join(root,'build/verification/glow-layout-browser');
fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
try {
 for(const [scene,width] of [['baseline',800],['arabic',800],['cjk',600]]) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');
  await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  const query=new URLSearchParams({manual:'',primary:'#415d8a',accent:'#91e8e1'});
  if(scene!=='baseline')query.set('scene',scene);
  await page.goto('http://127.0.0.1:4173/?'+query);
  await page.waitForFunction(()=>window.ready===true);
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:121},(_,i)=>i*10),...events.filter(t=>t<=1200)])].sort((a,b)=>a-b);
  for(const tick of ticks)await page.evaluate(t=>window.step(t),tick);
  const png=await page.screenshot();
  if(!png.equals(await page.screenshot()))throw new Error('Unstable frame '+scene);
  const existing=fs.readFileSync(path.join(root,`build/verification/classic-colors-browser/${scene}-cool-1200ms.png`));
  if(!png.equals(existing))throw new Error('Original frame changed before geometry inspection '+scene);
  fs.writeFileSync(path.join(output,`${scene}-animated.png`),png);
  const words=await page.locator('[data-overlap-fit-line] > div').evaluateAll(elements=>elements.map(element=>{
   const body=element.querySelector(':scope > span.z-10');
   const original={color:getComputedStyle(body).color,bodyFilter:getComputedStyle(body).filter,
    wordTransform:getComputedStyle(element).transform,wordOpacity:getComputedStyle(element).opacity};
   // Separate native DOM geometry diagnostic, after the animated PNG was saved.
   for(let node=element;node;node=node.parentElement)node.style.transform='none';
   const bodyBox=body.getBoundingClientRect();
   const segmenter=new Intl.Segmenter(undefined,{granularity:'grapheme'});
   const bodyRanges=Array.from(segmenter.segment(body.textContent),({segment,index})=>{
    const range=document.createRange();range.setStart(body.firstChild,index);range.setEnd(body.firstChild,index+segment.length);
    const bounds=range.getBoundingClientRect();
    return {text:segment,begin:index,end:index+segment.length,left:bounds.left-bodyBox.left,right:bounds.right-bodyBox.left};
   });
   const glowRanges=Array.from(element.querySelectorAll(':scope > [aria-hidden] > span'),span=>{
    const bounds=span.getBoundingClientRect(),style=getComputedStyle(span);
    return {text:span.textContent,left:bounds.left-bodyBox.left,right:bounds.right-bodyBox.left,
     opacity:style.opacity,shadow:style.textShadow,color:style.color};
   });
   return {text:body.textContent,direction:getComputedStyle(body).direction,font:getComputedStyle(body).font,
    original,bodyRanges,glowRanges};
  }));
  if(errors.length)throw new Error(errors.join('\n'));
  fs.writeFileSync(path.join(output,`${scene}.json`),JSON.stringify({revision,browser:browser.version(),width,height:600,timeMs:1200,
   animatedSha256:createHash('sha256').update(png).digest('hex'),
   diagnostic:'Actual mounted Classic. Animated PNG exactly matches prior color control. Ancestor transforms removed only after screenshot, solely for inline body Range and glow span geometry.',words},null,2));
  await page.close();
 }
 console.log(output);
} finally {await browser.close();}
