// Continuous chronological observation of ONE existing matrix condition.
// Draw-only observations must preserve all eight previously frozen keyframes.
import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{createRequire}from'node:module';import{createServer}from'vite';import{chromium}from'playwright';
import{root,checkReference,referenceMotionVersions}from'./reference.mjs';
import{cameraTargetPlugin}from'./diorama-camera-source.mjs';
import{cameraFlightPlugin}from'./diorama-flight-source.mjs';
checkReference();const require=createRequire(import.meta.url);
const folder=path.join(root,'verification/diorama-fixtures/flight-v1');
const original=JSON.parse(fs.readFileSync(path.join(folder,'scene.json'),'utf8'));
if(fs.existsSync(path.join(folder,'sequence.json')))throw Error('Preserve original sequence; refusing overwrite');
const out=path.join(root,'build/verification/diorama-flight-browser');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFlightPlugin()],
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-flight-webgl'),
 server:{host:'127.0.0.1',port:4186,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});const errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:4186/diorama-flight.html?scene=latin');
 await page.waitForFunction(()=>window.flightReady);
 const keyframes=original.captures.filter(c=>c.scene==='latin'&&c.width===800&&c.fps===60);
 const times=[...new Set([...original.times,...Array.from({length:64},(_,i)=>i/15)])].sort((a,b)=>a-b);
 const ticks=[...new Set([0,...original.times,...Array.from({length:252},(_,i)=>(i+1)/60)])].sort((a,b)=>a-b);
 let previous=0;const captures=[];const checks=[];
 const hash=b=>createHash('sha256').update(b).digest('hex');
 for(const t of ticks){
  const step=await page.evaluate(([t,dt])=>window.stepFlight(t,dt),[t,t-previous]);previous=t;
  if(times.includes(t)){
   const state=await page.evaluate(t=>window.drawFlight(t),t);
   const file=`sequence-latin-800-60-${Math.round(t*1000)}.png`;
   const png=await page.screenshot({path:path.join(out,file)});
   const old=keyframes.find(c=>c.time===t);
   if(old){
    if(JSON.stringify(step)!==JSON.stringify(old.step))throw Error(`Source step changed at ${t}`);
    const oldHash=hash(fs.readFileSync(path.join(out,old.file))),newHash=hash(png);
    if(oldHash!==newHash)throw Error(`Draw-only observation changed source raster at ${t}`);
    checks.push({time:t,oldFile:old.file,sha256:newHash});
   }
   captures.push({file,scene:'latin',width:800,fps:60,time:t,...state,step});
  }
 }
 if(errors.length)throw Error(errors.join('\n'));
 if(captures.length!==times.length||checks.length!==8)throw Error('Incomplete chronological observation');
 fs.writeFileSync(path.join(folder,'sequence.json'),JSON.stringify({...original,captures,times,
  browser:browser.version(),motion:referenceMotionVersions(),scope:original.scope+' Chronological 15Hz drawing observes the SAME 60Hz source schedule; all eight original source keyframes and steps are byte/value identical.',
  sourceKeyframeChecks:checks,sequenceHarnessSha256:hash(fs.readFileSync(new URL(import.meta.url)))},null,2)+'\n');
 const rows=captures.map(o=>[o.file,o.scene,o.width,o.fps,o.time,Buffer.from(o.text).toString('base64'),
  o.words.map(w=>[Buffer.from(w.text).toString('base64'),w.startTime,w.endTime].join(',')).join(';'),o.lineWidth,
  o.primary.join(','),o.accent.join(','),...o.renderedPosition,...o.renderedOrientation,o.life].join('\t'));
 fs.writeFileSync(path.join(folder,'sequence.tsv'),['# Same columns as scene.tsv; chronological observations of existing source schedule',...rows].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,unchangedSourceKeyframes:checks.length}));
}finally{await browser?.close();await server.close()}
