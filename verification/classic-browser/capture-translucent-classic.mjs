import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {root,revision,checkReference} from './reference.mjs';

// Full original scene corresponding to TranslucentClassicTest's paused public entry.
checkReference();
const output=path.join(root,'build/verification/classic-translucent-browser');
fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const frames=[];
try {
 for(const alpha of [128,64,0,255]) {
  const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>{delete Element.prototype.animate;});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  const query=new URLSearchParams({manual:'',primary:'#415d8a',accent:'#91e8e1'+alpha.toString(16).padStart(2,'0')});
  await page.goto('http://127.0.0.1:4173/?'+query);await page.waitForFunction(()=>window.ready===true);
  const events=await page.evaluate(()=>window.eventTimes);
  const ticks=[...new Set([...Array.from({length:151},(_,i)=>i*10),...events.filter(t=>t<=1500)])].sort((a,b)=>a-b);
  for(const tick of ticks)await page.evaluate(t=>window.step(t),tick);
  const png=await page.screenshot();
  if(!png.equals(await page.screenshot()))throw new Error('Unstable original alpha='+alpha);
  const file=`alpha-${alpha}.png`;fs.writeFileSync(path.join(output,file),png);
  frames.push({alpha,file,sha256:createHash('sha256').update(png).digest('hex'),
   words:await page.locator('[data-overlap-fit-line] > div').evaluateAll(elements=>elements.map(element=>({
    bodyColor:getComputedStyle(element.querySelector(':scope > span.z-10')).color,
    shadows:Array.from(element.querySelectorAll(':scope > [aria-hidden] > span'),span=>({
     shadow:getComputedStyle(span).textShadow,opacity:getComputedStyle(span).opacity}))}))) });
  if(errors.length)throw new Error(errors.join('\n'));await page.close();
 }
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,browser:browser.version(),viewport:[800,600],density:1,timeMs:1500,
  font:'FoliaParityLatin-Bold.ttf',fontSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))).digest('hex'),
  primary:'#415d8a',accentRgb:'#91e8e1',text:'Amber echo',clock:'Actual mounted Classic/Motion, 10ms and exact strict-passed boundary events',frames},null,2));
 console.log(output);
} finally {await browser.close();}
