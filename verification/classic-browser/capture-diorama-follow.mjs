import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';
import {createServer} from 'vite';
import {chromium} from 'playwright';
import {root,revision,checkReference,referenceMotionVersions} from './reference.mjs';
import {cameraTargetPlugin,sourceFragments} from './diorama-camera-source.mjs';
import {cameraFollowPlugin,followSourceFragments} from './diorama-follow-source.mjs';
checkReference();const require=createRequire(import.meta.url);
const out=path.join(root,'build/verification/diorama-follow-browser');
const fixture=path.join(root,'verification/diorama-fixtures/follow-v1/scene.json');
if(fs.existsSync(fixture))throw Error('Preserve source scene before recapturing');
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFollowPlugin()],resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-follow-webgl'),
 server:{host:'127.0.0.1',port:4186,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;
try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 fs.mkdirSync(out,{recursive:true});const captures=[];const times=[.7,1,1.5,2.3,3.1,3.45,4.2,8.92];
 for(const scene of ['latin','arabic','cjk'])for(const width of [380,800])for(const fps of [60,144]){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:4186/diorama-follow.html?scene=${scene}`);
  await page.waitForFunction(()=>window.followReady);
  const ticks=[...new Set([0,...times,...Array.from({length:Math.ceil(times.at(-1)*fps)},(_,i)=>(i+1)/fps).filter(t=>t<=times.at(-1))])].sort((a,b)=>a-b);
  let previous=0;
  for(const t of ticks){
   const step=await page.evaluate(([t,dt])=>window.stepFollow(t,dt),[t,t-previous]);previous=t;
   if(times.includes(t)){
    const state=await page.evaluate(t=>window.drawFollow(t),t);
    const file=`${scene}-${width}-${fps}-${Math.round(t*1000)}.png`;
    await page.screenshot({path:path.join(out,file)});
    captures.push({file,scene,width,fps,time:t,...state,step});
   }
  }
  if(errors.length)throw Error(errors.join('\n'));
  await page.close();
 }
 const fonts=['FoliaParityLatin-Bold.ttf','FoliaParityBidi-Bold.ttf','FoliaParityChatCJK-Bold.otf'].map(file=>({file,
  sha256:createHash('sha256').update(fs.readFileSync(path.join(root,'verification/assets',file))).digest('hex')}));
 fs.writeFileSync(fixture,JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),DPR:1,
  fonts,sourceFragments:[...sourceFragments,...followSourceFragments],times,shot:'orbit',seed:'DioramaProbe',
  scope:'Actual extracted original normal CameraRig follower + original Three active text three-layer stage. Source finite 60/144 schedules, including exact probes; no React CameraRig mount, transition, neighbours, depth/fog. Analytic active-unit charge injection unchanged from previous stage.',captures},null,2)+'\n');
 const rows=captures.map(o=>[o.file,o.scene,o.width,o.fps,o.time,Buffer.from(o.text).toString('base64'),
  o.words.map(w=>[Buffer.from(w.text).toString('base64'),w.startTime,w.endTime].join(',')).join(';'),o.lineWidth,
  o.primary.join(','),o.accent.join(','),...o.follow.position,...o.follow.look,...o.follow.orientation].join('\t'));
 fs.writeFileSync(fixture.replace(/\.json$/,'.tsv'),['# file scene width fps time text64 words64startEnd lineWidth primary accent positionXYZ lookXYZ quaternionXYZW',...rows].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,browser:browser.version()}));
}finally{await browser?.close();await server.close()}
