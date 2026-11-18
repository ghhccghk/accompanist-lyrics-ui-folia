// Bounded passive cache inspection, using the frozen mounted380/60/700 history.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import {createServer} from 'vite';
import {chromium} from 'playwright';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
import {cameraTargetPlugin} from './diorama-camera-source.mjs';
import {cameraFlightPlugin} from './diorama-flight-source.mjs';

checkReference();
const require=createRequire(import.meta.url),hash=data=>createHash('sha256').update(data).digest('hex');
const layers=process.argv.includes('--layers');
const geometry=process.argv.includes('--geometry');
const sampling=process.argv.includes('--sampling');
const mips=process.argv.includes('--mips');
if([layers,geometry,sampling,mips].filter(Boolean).length>1)throw Error('Use one diagnostic at a time');
const label=mips?'mounted-mips-v1':sampling?'mounted-sampling-v1':geometry?'mounted-geometry-v1':layers?'mounted-input-layers-v1':'mounted-raster-v1';
const fixture=path.join(root,'verification/diorama-fixtures',label);
if(fs.existsSync(fixture))throw Error('Preserve original cache observations; refusing fixture overwrite');
const original=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-fixtures/mounted-cut-v1/scene.json'),'utf8'));
const sourceFile='mounted-380-60-700.png';
const report=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-breath/comparison.json'),'utf8'));
const frozen=report.frames_detail.find(c=>c.file===sourceFile);
if(!frozen)throw Error('Missing independently retained original screenshot hash');
const expectedHash=Object.entries(frozen.hashes).find(([key])=>key.replaceAll('\\','/')===`build/verification/diorama-mounted-browser/${sourceFile}`)?.[1];
if(!expectedHash)throw Error('Missing original PNG hash');
const directory=path.resolve(path.dirname(fs.realpathSync(require.resolve('@react-three/fiber'))),'..');
const runtime=JSON.parse(fs.readFileSync(path.join(directory,'package.json'),'utf8'));
const rendering=Object.fromEntries([['@react-three/fiber',runtime.version],...Object.keys(runtime.dependencies).map(name=>
 [name,JSON.parse(fs.readFileSync(path.resolve(directory,'../..',name,'package.json'),'utf8')).version])]);
if(JSON.stringify(rendering)!==JSON.stringify(original.rendering))throw Error('Rendering dependency versions differ from frozen mounted source');
const aliases=['react','react/jsx-runtime','react/jsx-dev-runtime','react-dom','react-dom/client','@react-three/fiber','framer-motion']
 .map(n=>({find:new RegExp('^'+n+'$'),replacement:require.resolve(n)}));
aliases.push({find:/^three$/,replacement:path.join(path.dirname(require.resolve('three')),'three.module.js')});
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFlightPlugin()],resolve:{alias:aliases,dedupe:['react','react-dom','three','@react-three/fiber']},
 optimizeDeps:{noDiscovery:true,entries:[],include:['react','react/jsx-runtime','react/jsx-dev-runtime','react-dom','react-dom/client','@react-three/fiber','framer-motion','three']},
 cacheDir:path.join(root,'.work/diorama-mounted-vite-v2'),server:{host:'127.0.0.1',port:4188,strictPort:true,hmr:false,fs:{allow:[root]}}});
const out=path.join(root,mips?'build/verification/diorama-mounted-mips-browser':sampling?'build/verification/diorama-mounted-sampling-browser':geometry?'build/verification/diorama-mounted-geometry-browser':layers?'build/verification/diorama-mounted-input-browser':'build/verification/diorama-mounted-raster-browser');
let browser;
try {
 await server.listen();browser=await chromium.launch({headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 if(browser.version()!==original.browser)throw Error('Browser version differs from frozen original');
 const page=await browser.newPage({viewport:{width:380,height:600},deviceScaleFactor:1}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:4188/diorama-mounted.html');
 await page.waitForFunction(()=>window.mountedReady,null,{timeout:20000});
 await page.screenshot();
 const history=[];
 const oldTicks=[...new Set([0,6.8,...Array.from({length:Math.ceil(6.8*60)},(_,i)=>(i+1)/60).filter(t=>t<=6.8)])].sort((a,b)=>a-b);
 for(const time of oldTicks)history.push(await page.evaluate(t=>window.stepMounted(t,t),time));
 await page.evaluate(()=>window.beginMountedCut());
 const ticks=[...new Set([0,.7,...Array.from({length:Math.ceil(.7*60)},(_,i)=>(i+1)/60).filter(t=>t<=.7)])].sort((a,b)=>a-b);
 for(const time of ticks)history.push(await page.evaluate(t=>window.stepMounted(t,6.8+t),time));
 const before=await page.screenshot(),stateBefore=await page.evaluate(()=>window.readMounted());
 if(hash(before)!==expectedHash)throw Error(`Original replay differs BEFORE cache read: ${hash(before)} != ${expectedHash}`);
 const rasters=await page.evaluate(()=>window.readMountedLineRasters());
 const meshGeometry=geometry?await page.evaluate(()=>window.readMountedNeighborGeometry(7)):null;
 const mipData=mips?await page.evaluate(()=>window.readMountedNeighborMips(7,[0,4,5])):null;
 const after=await page.screenshot(),stateAfter=await page.evaluate(()=>window.readMounted());
 if(hash(after)!==expectedHash||JSON.stringify(stateBefore)!==JSON.stringify(stateAfter))throw Error('Passive cache read changed original pixels/state');
 const layerCaptures=[];
 const samplingCaptures=[];
 if(sampling){
  fs.mkdirSync(out,{recursive:true});
  const sourceState=await page.evaluate(()=>window.readMountedNeighborGeometry(7));
  for(const mode of ['original','linear-no-mip','trilinear-1']){
   await page.evaluate(mode=>window.renderMountedNeighborLayer(7,mode==='original'?undefined:mode),mode);
   const data=await page.screenshot(),file=`source-${mode}.png`;
   if(mode==='original'){
    const oldLayer=JSON.parse(fs.readFileSync(path.join(root,'verification/diorama-fixtures/mounted-input-layers-v1/scene.json'),'utf8'));
    if(hash(data)!==oldLayer.layerCaptures[0].sha256)throw Error('Unmodified source clone changed');
   }
   fs.writeFileSync(path.join(out,file),data);samplingCaptures.push({mode,file,sha256:hash(data)});
  }
  await page.evaluate(()=>window.renderMountedNeighborLayer(null));
  const restored=await page.screenshot(),stateRestored=await page.evaluate(()=>window.readMounted());
  const geometryRestored=await page.evaluate(()=>window.readMountedNeighborGeometry(7));
  if(hash(restored)!==expectedHash||JSON.stringify(stateBefore)!==JSON.stringify(stateRestored)||
    JSON.stringify(sourceState)!==JSON.stringify(geometryRestored))throw Error('Sampling clone altered original scene/state/inputs');
  fs.writeFileSync(path.join(out,'restored.png'),restored);
 }
 if(layers){
  fs.mkdirSync(out,{recursive:true});
  await page.evaluate(()=>window.renderMountedNeighborLayer(7));
  const data=await page.screenshot();fs.writeFileSync(path.join(out,'source-neighbor-7.png'),data);
  layerCaptures.push({file:'source-neighbor-7.png',sha256:hash(data)});
  await page.evaluate(()=>window.renderMountedNeighborLayer(null));
  const restored=await page.screenshot(),stateRestored=await page.evaluate(()=>window.readMounted());
  if(hash(restored)!==expectedHash||JSON.stringify(stateBefore)!==JSON.stringify(stateRestored))throw Error('Original scene/state not restored after cloned layer render');
  fs.writeFileSync(path.join(out,'restored.png'),restored);
 }
 const teardown=await page.evaluate(()=>window.unmountMounted());
 if(teardown.roots!==0||teardown.queuedRafs!==0||errors.length)throw Error(JSON.stringify({teardown,errors}));
 fs.mkdirSync(out,{recursive:true});fs.mkdirSync(fixture,{recursive:true});
 const records=rasters.map(({png,...r})=>{
  const data=Buffer.from(png.split(',')[1],'base64'),file=`cache-${r.index}.png`;
  fs.writeFileSync(path.join(out,file),data);return{...r,file,sha256:hash(data)};
 });
 fs.writeFileSync(path.join(out,'mounted-before.png'),before);fs.writeFileSync(path.join(out,'mounted-after.png'),after);
 const record={revision,motion:referenceMotionVersions(),rendering,browser:browser.version(),
  sourceFile,originalPngSha256:expectedHash,originalReplayBeforeAfterIdentical:true,
  mountedSceneSha256:hash(fs.readFileSync(path.join(root,'verification/diorama-fixtures/mounted-cut-v1/scene.json'))),
  fontSha256:hash(fs.readFileSync(path.join(root,'verification/assets/FoliaParityLatin-Bold.ttf'))),
  inputs:original.inputs,width:380,height:600,DPR:1,fps:60,mediaTime:.7,wallTime:7.5,
  actualCallbacks:history.length,callbackIntervals:history.map(s=>s.delta),state:stateBefore,teardown,
  sourceFiles:['DioramaScene.tsx','dioramaLineRasterCache.ts','dioramaTextRaster.ts'].map(file=>({file,sha256:hash(fs.readFileSync(path.join(upstream,'src/components/visualizer/diorama',file)))})),
  hostFiles:['diorama-mounted.tsx','diorama-mounted.html','capture-diorama-mounted-raster.mjs','pnpm-lock.yaml'].map(file=>({file,sha256:hash(fs.readFileSync(path.join(root,'verification/classic-browser',file)))})),
  scope:'Passive actual mounted neighbor-cache canvas read only. Original complete callback history through380/60/700, unchanged screenshot/state before/after. No source raster recreation, texture replacement, ref mutation or extra animation turn.',rasters:records,
  ...(layers?{layerCaptures,layerScope:'Diagnostic cloned actual mounted neighbour7 only, same material/texture/world matrix/camera/fog/renderer. No original refs or objects mutated. Whole original scene restored byte-identically without another RAF callback. This is layer isolation, not another whole-scene acceptance capture.'}:{}),
  ...(geometry?{meshGeometry,geometryScope:'Passive actual original mesh7 buffer UV/world/view/clip/pixel read, no matrix update or extra render, unchanged original state/pixels.'}:{}),
  ...(sampling?{samplingCaptures,samplingScope:'Isolated clone-only DIAGNOSTICS, not original acceptance frames: original actual mesh7 material/texture, versus independently cloned texture Source with linear/no-mip/aniso1 and trilinear/aniso1. All other state unchanged; no source object mutation, no additional RAF; original full scene restored unchanged.'}:{})};
 if(mips){
  record.mips=mipData.records.map(({rgba,png,...r})=>{
   const data=Buffer.from(rgba),file=`mip-${r.level}.rgba`;
   fs.writeFileSync(path.join(out,file),data);fs.writeFileSync(path.join(out,`mip-${r.level}.png`),Buffer.from(png.split(',')[1],'base64'));
   if(r.level!==0)fs.writeFileSync(path.join(fixture,file),data); // bounded synthetic texels only; base is the existing cache7 PNG
   return{...r,file,sha256:hash(data)};
  });
  record.mipScope='Read actual already-generated texture7 mip0/4/5 using temporary READ-only framebuffer attachment. Raw RGBA retained losslessly; PNG visual aids may round tiny-alpha RGB. No render, no extra RAF, no texture writes, exact READ binding/read-buffer restore and DRAW untouched; texture/source versions unchanged.';
  record.textureVersion=mipData.textureVersion;record.textureSourceVersion=mipData.sourceVersion;
 }
 fs.writeFileSync(path.join(fixture,'scene.json'),JSON.stringify(record,null,2)+'\n');
 fs.writeFileSync(path.join(fixture,'rasters.tsv'),['# index fontPx advancePx width height textBase64',...records.map(r=>
  [r.index,r.fontPx,r.advancePx,r.width,r.height,Buffer.from(r.text).toString('base64')].join('\t'))].join('\n')+'\n');
 if(geometry)fs.writeFileSync(path.join(fixture,'vertices.tsv'),['# textureX textureY worldX worldY worldZ pixelX pixelY depth',
  ...meshGeometry.vertices.map(v=>[v.uv[0]*meshGeometry.texture.width,(1-v.uv[1])*meshGeometry.texture.height,...v.world,...v.pixel,v.depth].join('\t'))].join('\n')+'\n');
 console.log(JSON.stringify({rasters:records.length,actualCallbacks:history.length,expectedHash,teardown,folder:fixture}));
}finally {await browser?.close();await server.close()}
