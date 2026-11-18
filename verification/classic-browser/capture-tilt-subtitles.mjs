// Real original Tilt + its real subtitle overlay, unchanged source algorithms.
import fs from 'node:fs';import path from 'node:path';import {createHash} from 'node:crypto';
import {chromium} from 'playwright';import {startReferenceServer} from './serve.mjs';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();const fixture=path.join(root,'verification/tilt-fixtures/subtitles-v2');
if(fs.existsSync(fixture))throw Error('Preserve original subtitle oracle; refusing overwrite');
const server=await startReferenceServer(4195,{realSubtitles:true});let browser;
try {
 browser=await chromium.launch({headless:true});const captures=[];
 const output=path.join(root,'build/verification/tilt-subtitles-browser');fs.mkdirSync(output,{recursive:true});
 const times=[1000,1500,2400,3200,3500];
 for(const width of [380,800])for(const scale of [1,1.4,2]) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.addInitScript(()=>{delete Element.prototype.animate});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4195/?manual&theme=tilt&scene=wrap&tilt=0&split=.75&fontScale=${scale}&subtitles`);
  await page.waitForFunction(()=>window.ready);
  for(let ms=0;ms<=times.at(-1);ms+=10) {
   if(ms)await page.clock.runFor(10);await page.evaluate(t=>window.step(t),ms);
   if(!times.includes(ms))continue;
   const data=await page.evaluate(()=>({
    main:[...document.querySelectorAll('div.whitespace-nowrap')].map(el=>{
     const s=getComputedStyle(el);return {text:el.textContent.replaceAll('\u00a0',' '),font:parseFloat(s.fontSize),
      width:parseFloat(s.width),height:parseFloat(s.height),rect:el.getBoundingClientRect().toJSON(),motion:window.motionValues(el)};
    }),
    subtitles:[...document.querySelectorAll('[data-subtitle-track]')].map(el=>{
     const s=getComputedStyle(el);return {text:el.textContent,font:parseFloat(s.fontSize),lineHeight:parseFloat(s.lineHeight),
      rect:el.getBoundingClientRect().toJSON(),opacity:s.opacity};
    })
   }));
   const file=`${width}-${scale}-${ms}.png`;await page.screenshot({path:path.join(output,file)});
   captures.push({file,width,scale,mediaMs:ms,...data});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'rows.tsv'),['# file role index text64 font width height top',...captures.flatMap(c=>[
  ...c.main.map((r,i)=>[c.file,'main',i,Buffer.from(r.text).toString('base64'),r.font,r.width,r.height,r.rect.top-(r.motion.y??0)].join('\t')),
  ...c.subtitles.map((r,i)=>[c.file,'subtitle',i,Buffer.from(r.text).toString('base64'),r.font,r.rect.width,r.rect.height,r.rect.top].join('\t'))
 ])].join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,browser:browser.version(),motion:referenceMotionVersions(),
  DPR:1,height:600,clock:'10ms media/RAF/Motion with actual event thresholds; WAAPI disabled as in previous Tilt matrix',
  scope:'Actual original Tilt and VisualizerSubtitleOverlay. Host shell/background-only isolation, fixed112px bottom, source translation-only valid layout, original default typography with parity font700, actual default subtitle opacity0.6 and background pad off. No algorithm replacement or collision suppression. Prior v1 injected opacity1 mismatched Kotlin and was preserved as invalid under .work/tilt-subtitles-opacity1, not selected out by image quality.',
  captures,sourceHashes:['src/components/visualizer/tilt/VisualizerTilt.tsx','src/components/visualizer/VisualizerSubtitleOverlay.tsx','src/components/visualizer/subtitleFontSizes.ts','package-lock.json'].map(file=>({file,sha256:hash(path.join(upstream,file))})),
  hostHashes:['verification/classic-browser/main.tsx','verification/classic-browser/style.css','verification/classic-browser/serve.mjs','verification/classic-browser/capture-tilt-subtitles.mjs',
   'verification/classic-browser/subtitle-host.ts','verification/classic-fixtures/scenes.tsv','verification/assets/FoliaParityLatin-Bold.ttf','verification/classic-browser/pnpm-lock.yaml'].map(file=>({file,sha256:hash(path.join(root,file))}))},null,2)+'\n');
 console.log(JSON.stringify({frames:captures.length,mainRows:captures.reduce((n,c)=>n+c.main.length,0),subtitleRows:captures.reduce((n,c)=>n+c.subtitles.length,0)}));
}finally{await browser?.close();await server.close()}
