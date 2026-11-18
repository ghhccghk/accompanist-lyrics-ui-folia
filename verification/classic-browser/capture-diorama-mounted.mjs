import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{createRequire}from'node:module';
import{createServer}from'vite';import{chromium}from'playwright';
import{root,upstream,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
import{cameraTargetPlugin,sourceFragments}from'./diorama-camera-source.mjs';
import{cameraFlightPlugin,flightSourceFragments}from'./diorama-flight-source.mjs';
checkReference();const require=createRequire(import.meta.url),out=path.join(root,'build/verification/diorama-mounted-browser');
function renderingVersions(){
 const directory=path.resolve(path.dirname(fs.realpathSync(require.resolve('@react-three/fiber'))),'..');
 const p=JSON.parse(fs.readFileSync(path.join(directory,'package.json'),'utf8'));
 const lock=JSON.parse(fs.readFileSync(path.join(upstream,'package-lock.json'),'utf8'));
 const entries=[['@react-three/fiber',p.version],...Object.keys(p.dependencies).map(name=>{
  const meta=JSON.parse(fs.readFileSync(path.resolve(directory,'../..',name,'package.json'),'utf8'));return[name,meta.version];
 })];
 for(const[name,version]of entries)if(version!==lock.packages['node_modules/'+name].version)throw Error(`Original rendering runtime differs: ${name} ${version}`);
 return Object.fromEntries(entries);
}
const rendering=renderingVersions();
const diagnostic=process.argv.includes('--diagnostic');
const folder=path.join(root,diagnostic?'.work/diorama-mounted-control-diagnostic':'verification/diorama-fixtures/mounted-cut-v1');
if(fs.existsSync(folder))throw Error('Preserve mounted source fixture; refusing overwrite');
const aliases=['react','react/jsx-runtime','react/jsx-dev-runtime','react-dom','react-dom/client','@react-three/fiber','framer-motion']
 .map(n=>({find:new RegExp('^'+n+'$'),replacement:require.resolve(n)}));
aliases.push({find:/^three$/,replacement:path.join(path.dirname(require.resolve('three')),'three.module.js')});
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),plugins:[cameraTargetPlugin(),cameraFlightPlugin()],resolve:{alias:aliases,dedupe:['react','react-dom','three','@react-three/fiber']},
 optimizeDeps:{noDiscovery:true,entries:[],include:['react','react/jsx-runtime','react/jsx-dev-runtime','react-dom','react-dom/client','@react-three/fiber','framer-motion','three']},
 cacheDir:path.join(root,'.work/diorama-mounted-vite-v2'),server:{host:'127.0.0.1',port:4188,strictPort:true,hmr:false,fs:{allow:[root]}}});
let browser;try{
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const times=[0,.7,1.6,2.099,2.1,3.199,3.2,3.201,3.499,3.5,4.5,4.6],captures=[],steps=[],teardowns=[];let inputs;
 fs.mkdirSync(out,{recursive:true});
 for(const width of(diagnostic?[380]:[380,800]))for(const fps of(diagnostic?[60]:[60,144])){
  const page=await browser.newPage({viewport:{width,height:600},deviceScaleFactor:1}),errors=[];
  page.on('pageerror',e=>{errors.push(e.message);console.error(e.message)});
  await page.goto('http://127.0.0.1:4188/diorama-mounted.html');await page.waitForFunction(()=>window.mountedReady,null,{timeout:20000});
  inputs=await page.evaluate(()=>window.mountedInputs);await page.screenshot();
  const oldTicks=[...new Set([0,6.8,...Array.from({length:Math.ceil(6.8*fps)},(_,i)=>(i+1)/fps).filter(t=>t<=6.8)])].sort((a,b)=>a-b);
  for(const t of oldTicks)steps.push({width,fps,phase:'outgoing',...await page.evaluate(t=>window.stepMounted(t,t),t)});
  const incoming=await page.evaluate(()=>window.beginMountedCut());
  const ticks=[...new Set([0,...times,...Array.from({length:Math.ceil(4.6*fps)},(_,i)=>(i+1)/fps).filter(t=>t<=4.6)])].sort((a,b)=>a-b);
  for(const t of ticks){
   const state=await page.evaluate(t=>window.stepMounted(t,6.8+t),t);steps.push({width,fps,phase:'incoming',...state});
   if(times.includes(t)){
    const file=`mounted-${width}-${fps}-${Math.round(t*1000)}.png`;
    await page.screenshot({path:path.join(out,file)});captures.push({file,width,fps,time:t,incoming,...state});
   }
  }
  teardowns.push({width,fps,...await page.evaluate(()=>window.unmountMounted())});
  if(errors.length)throw Error(errors.join('\n'));await page.close();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 const sourceFiles=['CameraRig.tsx','DioramaScene.tsx','DioramaActiveUnitPlanes.tsx','dioramaLineRasterCache.ts','dioramaSceneFrame.ts','dioramaSceneLayout.ts','cameraPath.ts'];
 const records={revision,motion:referenceMotionVersions(),rendering,browser:browser.version(),DPR:1,inputs,times,teardowns,sourceFragments:[...sourceFragments,...flightSourceFragments],
  hostFiles:['diorama-mounted.tsx','diorama-mounted.html','capture-diorama-mounted.mjs','pnpm-lock.yaml','pnpm-workspace.yaml'].map(file=>({file,sha256:hash(path.join(root,'verification/classic-browser',file))})),
  sourceFiles:sourceFiles.map(file=>({file,sha256:hash(path.join(upstream,'src/components/visualizer/diorama',file))})),
  font:{file:'FoliaParityLatin-Bold.ttf',sha256:hash(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))},
  scope:'Untouched original CameraRig + DioramaScene React/R3F components, real original default shot picker/active geometry/envelopes/breath/fog/tone mapping/cache. Background geometry/audio disabled via real props, uniform colors, no source refs mutated. Host explicitly controls ready-song edge/global index/local media/wall6.8+t/manual R3F advance and one queued original cache RAF batch per turn. NOT complete VisualizerDiorama ready gate/wall-timeout/normal browser RAF or device equivalence.',captures};
 fs.mkdirSync(folder,{recursive:true});fs.writeFileSync(path.join(folder,'scene.json'),JSON.stringify(records,null,2)+'\n');
 // Compact complete scalar history; duplicate per-step material trees stay in
 // ignored build output. No original callback interval or camera state omitted.
 fs.writeFileSync(path.join(out,'steps-full.json'),JSON.stringify(steps)+'\n');
 fs.writeFileSync(path.join(folder,'steps.tsv'),['# phase width fps time wall delta current outgoing epoch shot widthBefore widthAfter position3 velocity3 look3 lookVelocity3 alignment alignmentQuaternion4 orientation4 renderedPosition3 renderedQuaternion4 controlPeak rasterIndices neighborIndices',
  ...steps.map(s=>[s.phase,s.width,s.fps,s.time,s.wall,s.delta,s.current,s.outgoing??-1,s.epoch,s.shot,s.widthBefore,s.widthAfter,
   ...s.state.position,...s.state.velocity,...s.state.look,...s.state.lookVelocity,s.state.alignment,...s.state.alignmentOrientation,
   ...s.state.orientation,...s.renderedPosition,...s.renderedOrientation,s.controlPeak,s.rasterIndices.join(','),s.neighbors.map(n=>n.index).join(',')].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({frames:captures.length,steps:steps.length,teardowns,folder}));
}finally{await browser?.close();await server.close()}
