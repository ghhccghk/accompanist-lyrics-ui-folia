// New bounded actual wheel tail/jump input, leaving the old144-frame oracle intact.
import fs from 'node:fs';import path from 'node:path';import {createHash}from'node:crypto';
import {chromium}from'playwright';import{startReferenceServer}from'./serve.mjs';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
checkReference();
const fixture=path.join(root,'verification/pendolo-fixtures/follower-stage-v1');
if(fs.existsSync(fixture))throw Error('Preserve original follower-stage fixture; refusing overwrite');
const server=await startReferenceServer(4190);let browser;
try {
 browser=await chromium.launch({headless:true});const records=[],frames=['# file timing intensity mediaMillis target value wheel'],rowRecords=['# file index opacity contentRect'];
 const output=path.join(root,'build/verification/pendolo-follower-stage-browser');fs.mkdirSync(output,{recursive:true});
 const probes=[.95,1,1.1,1.2,1.27,1.3,1.39,1.4,1.5];
 for(const [timing,intensity]of [['regular','chaotic'],['tied','normal'],['tied','chaotic']]){
  const page=await browser.newPage({viewport:{width:380,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.goto(`http://127.0.0.1:4190/pendolo-tick-stage.html?scene=latin&intensity=${intensity}&timing=${timing}`);
  await page.waitForFunction(()=>window.ready);for(let i=0;i<10;i++)await page.evaluate(()=>window.advanceSourceFrame());
  const base=await page.evaluate(()=>window.testNow),inputs=await page.evaluate(()=>window.tickInputs);
  const times=[...new Set([...probes,.7,...Array.from({length:91},(_,i)=>i/60)])].sort((a,b)=>a-b);let previous=-1;
  for(const t of probes){
   for(const time of times.filter(x=>x>previous&&x<=t))await page.evaluate(({time,base})=>{window.testNow=base+time*1000;window.advanceWheel(time);window.advanceSourceFrame(0);window.driveNativeAnimations()},{time,base});
   previous=t;const s=await page.evaluate(()=>window.readTickWheel()),target=await page.evaluate(t=>window.advanceWheel(t).target,t);
   const file=`${timing}-${intensity}-${Math.round(t*1000)}.png`;
   frames.push([file,timing,intensity,Math.round(t*1000),target,s.value,s.rotation].join('\t'));
   for(const r of s.rows)rowRecords.push([file,r.index,r.opacity,r.content.join(',')].join('\t'));
   records.push({file,timing,intensity,time:t,target,inputs,state:s});await page.screenshot({path:path.join(output,file)});
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(fixture,{recursive:true});
 fs.writeFileSync(path.join(fixture,'frames.tsv'),frames.join('\n')+'\n');fs.writeFileSync(path.join(fixture,'rows.tsv'),rowRecords.join('\n')+'\n');
 fs.writeFileSync(path.join(fixture,'conditions.json'),JSON.stringify({revision,browser:browser.version(),motion:referenceMotionVersions(),DPR:1,viewport:[380,600],
  sourceHash:hash(path.join(upstream,'src/components/visualizer/pendolo/VisualizerPendolo.tsx')),
  hostHash:hash(path.join(root,'verification/classic-browser/pendolo-tick-stage.tsx')),
  referenceHostHashes:['.work/pendolo-tick-stage-reference.tsx','verification/classic-browser/pendolo-tick-stage.html',
   'verification/classic-browser/generate-pendolo-stage-reference.mjs','verification/classic-browser/generate-pendolo-tick-stage-reference.mjs']
   .map(file=>({file,sha256:hash(path.join(root,file))})),
  lockHash:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  fontHash:hash(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf')),records,
  scope:'Unchanged original wheel JSX/useSpring, same source60Hz plus exact integer-ms probes. One input option makes eight lines start together (delta8); default regular unchanged. Smart-beat/audio/gears/chorus absent; no source spring/time alteration. Host RAF/WAAPI intervention inherited from tick-stage.'},null,2)+'\n');
 console.log(JSON.stringify({frames:records.length,rows:rowRecords.length-1,browser:browser.version()}));
}finally{await browser?.close();await server.close()}
