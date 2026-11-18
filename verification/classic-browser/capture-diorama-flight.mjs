import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';
import {createServer} from 'vite';
import {chromium} from 'playwright';
import {root,revision,checkReference,referenceMotionVersions} from './reference.mjs';
import {cameraTargetPlugin,sourceFragments} from './diorama-camera-source.mjs';
import {cameraFlightPlugin,flightSourceFragments} from './diorama-flight-source.mjs';

checkReference();const require=createRequire(import.meta.url);
const out=path.join(root,'build/verification/diorama-flight-browser');
const fixture=path.join(root,'verification/diorama-fixtures/flight-v1/scene.json');
if(fs.existsSync(fixture))throw Error('Preserve original flight scene before recapturing');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFlightPlugin()],
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-flight-webgl'),
 server:{host:'127.0.0.1',port:4186,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,
  args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 fs.mkdirSync(out,{recursive:true});const captures=[];
 const times=[0,.7,1.6,2.4,3.199,3.2,3.201,4.2];
 for(const scene of ['latin','arabic','cjk'])for(const width of [380,800])for(const fps of [60,144]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4186/diorama-flight.html?scene=${scene}`);
  await page.waitForFunction(()=>window.flightReady);
  const ticks=[...new Set([0,...times,...Array.from({length:Math.ceil(times.at(-1)*fps)},(_,i)=>(i+1)/fps)
   .filter(t=>t<=times.at(-1))])].sort((a,b)=>a-b);
  let previous=0;
  for(const t of ticks){
   const step=await page.evaluate(([t,dt])=>window.stepFlight(t,dt),[t,t-previous]);previous=t;
   if(times.includes(t)){
    const state=await page.evaluate(t=>window.drawFlight(t),t);
    const file=`${scene}-${width}-${fps}-${Math.round(t*1000)}.png`;
    await page.screenshot({path:path.join(out,file)});
    captures.push({file,scene,width,fps,time:t,...state,step});
   }
  }
  if(errors.length)throw Error(errors.join('\n'));
  await page.close();
 }
 const hash=file=>createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex');
 const sources=['verification/classic-browser/pnpm-lock.yaml','verification/classic-browser/diorama-flight.ts',
  'verification/classic-browser/diorama-flight-source.mjs','verification/classic-browser/capture-diorama-flight.mjs',
  ...['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(f=>'verification/assets/'+f)]
  .map(file=>({file,sha256:hash(file)}));
 fs.writeFileSync(fixture,JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),DPR:1,
  sources,sourceFragments:[...sourceFragments,...flightSourceFragments],times,shot:'orbit',seed:'Incoming mirror',epoch:1,
  initialState:{position:[0,.6,9],velocity:[0,0,0],look:[0,0,-10],lookVelocity:[0,0,0],alignment:0},
  scope:'Actual extracted original CameraRig flight/follower + original Three full-shaped incoming text/life. Shared explicit incoming refs, normal source targets, finite 60/144 schedules including exact probes. No React mount, outgoing corridor, depth/fog/background. Analytic active-unit charge injection, delta0, is a separate media adaptation.',captures},null,2)+'\n');
 const rows=captures.map(o=>[o.file,o.scene,o.width,o.fps,o.time,Buffer.from(o.text).toString('base64'),
  o.words.map(w=>[Buffer.from(w.text).toString('base64'),w.startTime,w.endTime].join(',')).join(';'),o.lineWidth,
  o.primary.join(','),o.accent.join(','),...o.renderedPosition,...o.renderedOrientation,o.life].join('\t'));
 fs.writeFileSync(fixture.replace(/\.json$/,'.tsv'),
  ['# file scene width fps time text64 words64startEnd lineWidth primary accent renderedEye3 renderedQuaternion4 life',...rows].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,browser:browser.version()}));
}finally{await browser?.close();await server.close()}
