// Actual original Tilt at explicit lyric scales. No source algorithm changes.
import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{chromium}from'playwright';import{startReferenceServer}from'./serve.mjs';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
checkReference();const nativeDefault=process.argv.includes('--native-default-metrics');
const fixture=path.join(root,`verification/tilt-fixtures/font-scale${nativeDefault?'-native-default':''}-v1`);
if(fs.existsSync(fixture))throw Error('Preserve original font-scale oracle; refusing overwrite');
const server=await startReferenceServer(4193);let browser;
try {
 browser=await chromium.launch({headless:true});const captures=[];
 const output=path.join(root,`build/verification/tilt-font-scale${nativeDefault?'-native-default':''}-browser`);fs.mkdirSync(output,{recursive:true});
 for(const width of [380,800])for(const scale of [1,1.4,2])for(const tilt of [0,1]) {
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.addInitScript(()=>{delete Element.prototype.animate});
  if(nativeDefault)await page.addInitScript(()=>{
   // A second, explicitly controlled initial INPUT to pretext's retained
   // measurement context. Keep the untouched natural-default oracle separately.
   // This is not a replacement for its failures or visual acceptance. Segoe UI
   // matches this Windows Skia host's measured SansSerif; no font file is copied.
   const getContext=OffscreenCanvas.prototype.getContext;
   OffscreenCanvas.prototype.getContext=function(...args){
    const context=getContext.apply(this,args);
    if(this.width===1&&this.height===1&&args[0]==='2d'&&context)context.font='10px "Segoe UI"';
    return context;
   };
  });
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4193/?manual&theme=tilt&scene=wrap&tilt=${tilt}&split=.75&fontScale=${scale}`);
  await page.waitForFunction(()=>window.ready);
  for(let ms=0;ms<=2400;ms+=10) {
   if(ms>0)await page.clock.runFor(10);
   await page.evaluate(t=>window.step(t),ms);
  }
  const rows=await page.locator('div.whitespace-nowrap').evaluateAll(es=>es.map(el=>{
   const s=getComputedStyle(el);return {text:el.textContent.replaceAll('\u00a0',' '),tilt:s.fontStyle==='italic',
    font:parseFloat(s.fontSize),width:parseFloat(s.width),height:parseFloat(s.height),rect:el.getBoundingClientRect().toJSON()};
  }));
  const file=`${width}-${scale}-${tilt}.png`;await page.screenshot({path:path.join(output,file)});
  if(errors.length)throw Error(errors.join('\n'));
  captures.push({file,width,scale,tilt,mediaMs:2400,rows});await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,browser:browser.version(),motion:referenceMotionVersions(),DPR:1,height:600,
  scope:'Untouched mounted original VisualizerTilt. Explicit lyricsFontScale, fixed wrap synthetic scene and CSS/RAF clock at 10ms. Existing shell/background/subtitles isolated by existing host aliases. Not a subtitle, device or complete theme acceptance.',
  initialMetricFont:nativeDefault?'10px Segoe UI (host-injected context input, matching measured Windows Skia SansSerif)':'Untouched browser default 10px sans-serif',
  captures,sourceSha256:hash(path.join(upstream,'src/components/visualizer/tilt/VisualizerTilt.tsx')),
  hashes:['verification/classic-browser/main.tsx','verification/classic-browser/style.css','verification/classic-browser/serve.mjs',
   'verification/classic-fixtures/scenes.tsv','verification/partita-fixtures/scenes.tsv','verification/assets/FoliaParityLatin-Bold.ttf','verification/classic-browser/pnpm-lock.yaml']
   .map(file=>({file,sha256:hash(path.join(root,file))})),originalLockSha256:hash(path.join(upstream,'package-lock.json'))},null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'rows.tsv'),['# file width scale tilt index text64 font width height',...captures.flatMap(c=>c.rows.map((r,i)=>[
  c.file,c.width,c.scale,c.tilt,i,Buffer.from(r.text).toString('base64'),r.font,r.width,r.height].join('\t')))].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,rows:captures.reduce((n,c)=>n+c.rows.length,0)}));
}finally{await browser?.close();await server.close()}
