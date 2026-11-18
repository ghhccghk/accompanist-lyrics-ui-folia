import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{createRequire}from'node:module';import{createServer}from'vite';import{chromium}from'playwright';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
import{cameraTargetPlugin,sourceFragments}from'./diorama-camera-source.mjs';
import{cameraFlightPlugin,flightSourceFragments}from'./diorama-flight-source.mjs';
checkReference();const require=createRequire(import.meta.url);
const folder=path.join(root,'verification/diorama-fixtures/loop-stage-v1'),out=path.join(root,'build/verification/diorama-loop-stage-browser');
if(fs.existsSync(folder))throw Error('Preserve original loop stage; refusing overwrite');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFlightPlugin()],resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-loop-stage-webgl'),
 server:{host:'127.0.0.1',port:4186,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const times=[0,.7,1.6,2.099,2.1,3.199,3.2,3.201,3.5,4.5],captures=[];let inputs;
 fs.mkdirSync(out,{recursive:true});
 for(const width of[380,800])for(const fps of[60,144]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:4186/diorama-loop-stage.html');
  await page.waitForFunction(()=>window.loopStageReady);inputs=await page.evaluate(()=>window.loopInputs);
  let last=0;
  const outgoing=[...new Set([0,4.2,...Array.from({length:Math.ceil(4.2*fps)},(_,i)=>(i+1)/fps).filter(t=>t<=4.2)])].sort((a,b)=>a-b);
  for(const t of outgoing){await page.evaluate(([t,dt])=>window.stepOutgoing(t,dt),[t,t-last]);last=t;}
  const incoming=await page.evaluate(()=>window.beginLoop());last=0;
  const ticks=[...new Set([0,...times,...Array.from({length:Math.ceil(4.5*fps)},(_,i)=>(i+1)/fps).filter(t=>t<=4.5)])].sort((a,b)=>a-b);
  for(const t of ticks){
   const step=await page.evaluate(([t,dt])=>window.stepLoop(t,dt),[t,t-last]);last=t;
   if(times.includes(t)){
    const state=await page.evaluate(t=>window.drawLoop(t),t),file=`loop-${width}-${fps}-${Math.round(t*1000)}.png`;
    await page.screenshot({path:path.join(out,file)});captures.push({file,scene:'latin-loop',width,fps,time:t,incoming,...state,step});
   }
  }
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 const files=['diorama-loop-stage.ts','capture-diorama-loop-stage.mjs','diorama-camera-source.mjs','diorama-flight-source.mjs','pnpm-lock.yaml'];
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'scene.json'),JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),DPR:1,inputs,times,
  sources:files.map(file=>({file,sha256:hash(path.join(root,'verification/classic-browser',file))})),
  font:{file:'FoliaParityLatin-Bold.ttf',sha256:hash(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))},
  sourceFragments:[...sourceFragments,...flightSourceFragments],sourceTextHashes:['dioramaSceneLayout.ts','dioramaSceneFrame.ts','dioramaTextRaster.ts','dioramaSceneConstants.ts','dioramaSceneUnits.ts','dioramaSequencer.ts']
   .map(file=>({file,sha256:hash(path.join(upstream,'src/components/visualizer/diorama',file))})),
  scope:'Complete original text-only window/plain+active three-layer stage with original extracted flight/follower, including outgoing flight2 then same-corridor flight3. Source finite60/144 schedules; local loop clock reset is explicit, window time uses same controlled media3.2s. Analytic unit charge injection/delta0/breath1/audio0. NOT mounted React/RAF envelope/theme damping/fog/background/depth/device equivalence.',captures},null,2)+'\n');
 fs.writeFileSync(path.join(folder,'scene.tsv'),['# file width fps time current outgoing(-1=null) primary3 secondary3 accent3 lineWidths3 renderedEye3 renderedQuaternion4 life indices',
  ...captures.map(c=>[c.file,c.width,c.fps,c.time,c.current,c.outgoing??-1,c.primary.join(','),c.secondary.join(','),c.accent.join(','),
   c.lineWidths.join(','),...c.renderedPosition,...c.renderedOrientation,c.life,c.indices.join(',')].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,browser:browser.version()}));
}finally{await browser?.close();await server.close()}
